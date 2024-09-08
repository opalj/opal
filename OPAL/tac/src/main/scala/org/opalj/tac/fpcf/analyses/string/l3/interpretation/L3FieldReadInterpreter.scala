/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package string
package l3
package interpretation

import scala.collection.mutable.ListBuffer

import org.opalj.br.DeclaredField
import org.opalj.br.analyses.DeclaredFields
import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.analyses.ContextProvider
import org.opalj.br.fpcf.properties.fieldaccess.FieldWriteAccessInformation
import org.opalj.br.fpcf.properties.string.StringConstancyProperty
import org.opalj.br.fpcf.properties.string.StringTreeNode
import org.opalj.br.fpcf.properties.string.StringTreeNull
import org.opalj.br.fpcf.properties.string.StringTreeOr
import org.opalj.fpcf.EOptionP
import org.opalj.fpcf.InterimResult
import org.opalj.fpcf.ProperPropertyComputationResult
import org.opalj.fpcf.PropertyStore
import org.opalj.fpcf.SomeEOptionP
import org.opalj.fpcf.SomeEPS
import org.opalj.fpcf.UBP
import org.opalj.tac.fpcf.analyses.string.SoundnessMode
import org.opalj.tac.fpcf.analyses.string.interpretation.InterpretationHandler
import org.opalj.tac.fpcf.properties.string.StringFlowFunctionProperty

/**
 * Responsible for processing direct reads to fields (see [[FieldRead]]) by analyzing the write accesses to these fields
 * via the [[FieldWriteAccessInformation]].
 *
 * @author Maximilian Rüsch
 */
class L3FieldReadInterpreter(
    implicit val ps:              PropertyStore,
    implicit val project:         SomeProject,
    implicit val declaredFields:  DeclaredFields,
    implicit val contextProvider: ContextProvider,
    implicit val soundnessMode:   SoundnessMode
) extends AssignmentBasedStringInterpreter {

    override type E = FieldRead[V]

    private case class FieldReadState(
        target:                        PV,
        var fieldAccessDependee:       EOptionP[DeclaredField, FieldWriteAccessInformation],
        var seenDirectFieldAccesses:   Int                                                        = 0,
        var seenIndirectFieldAccesses: Int                                                        = 0,
        var hasWriteInSameMethod:      Boolean                                                    = false,
        var hasInit:                   Boolean                                                    = false,
        var hasUnresolvableAccess:     Boolean                                                    = false,
        var accessDependees:           Seq[EOptionP[VariableDefinition, StringConstancyProperty]] = Seq.empty,
        previousResults:               ListBuffer[StringTreeNode]                                 = ListBuffer.empty
    ) {

        def updateAccessDependee(newDependee: EOptionP[VariableDefinition, StringConstancyProperty]): Unit = {
            accessDependees = accessDependees.updated(
                accessDependees.indexWhere(_.e == newDependee.e),
                newDependee
            )
        }

        def hasDependees: Boolean = fieldAccessDependee.isRefinable || accessDependees.exists(_.isRefinable)

        def dependees: Iterable[SomeEOptionP] = {
            val dependees = accessDependees.filter(_.isRefinable)

            if (fieldAccessDependee.isRefinable) fieldAccessDependee +: dependees
            else dependees
        }
    }

    override def interpretExpr(target: PV, fieldRead: E)(implicit
        state: InterpretationState
    ): ProperPropertyComputationResult = {
        if (!InterpretationHandler.isSupportedType(fieldRead.declaredFieldType)) {
            return failure(target)
        }

        val field = declaredFields(fieldRead.declaringClass, fieldRead.name, fieldRead.declaredFieldType)
        val fieldAccessEOptP = ps(field, FieldWriteAccessInformation.key)

        implicit val accessState: FieldReadState = FieldReadState(target, fieldAccessEOptP)
        if (fieldAccessEOptP.hasUBP) {
            handleFieldAccessInformation(fieldAccessEOptP.ub)
        } else {
            accessState.previousResults.prepend(StringTreeNode.ub)
            InterimResult.forUB(
                InterpretationHandler.getEntity,
                StringFlowFunctionProperty.ub(state.pc, target),
                accessState.dependees.toSet,
                continuation(accessState, state)
            )
        }
    }

    private def handleFieldAccessInformation(accessInformation: FieldWriteAccessInformation)(
        implicit
        accessState: FieldReadState,
        state:       InterpretationState
    ): ProperPropertyComputationResult = {
        if (accessState.fieldAccessDependee.isFinal && accessInformation.accesses.isEmpty) {
            // No methods which write the field were found => Field could either be null or any value
            return computeFinalResult(computeUBWithNewTree(StringTreeOr.fromNodes(
                if (soundnessMode.isHigh) StringTreeNode.lb
                else StringTreeNode.ub,
                StringTreeNull
            )))
        }

        accessInformation.getNewestAccesses(
            accessInformation.numDirectAccesses - accessState.seenDirectFieldAccesses,
            accessInformation.numIndirectAccesses - accessState.seenIndirectFieldAccesses
        ).foreach {
            case (contextId, pc, _, parameter) =>
                val method = contextProvider.contextFromId(contextId).method.definedMethod

                if (method == state.dm.definedMethod) {
                    accessState.hasWriteInSameMethod = true
                }

                if (method.name == "<init>" || method.name == "<clinit>") {
                    accessState.hasInit = true
                }

                if (parameter.isEmpty) {
                    // Field parameter information is not available
                    accessState.hasUnresolvableAccess = true
                } else {
                    val entity = VariableDefinition(pc, PUVar(parameter.get._1, parameter.get._2), method)
                    accessState.accessDependees = accessState.accessDependees :+ ps(entity, StringConstancyProperty.key)
                }
        }

        accessState.seenDirectFieldAccesses = accessInformation.numDirectAccesses
        accessState.seenIndirectFieldAccesses = accessInformation.numIndirectAccesses

        tryComputeFinalResult
    }

    private def tryComputeFinalResult(implicit
        accessState: FieldReadState,
        state:       InterpretationState
    ): ProperPropertyComputationResult = {
        if (accessState.hasWriteInSameMethod && soundnessMode.isHigh) {
            // We cannot handle writes to a field that is read in the same method at the moment as the flow functions do
            // not capture field state. This can be improved upon in the future.
            computeFinalResult(computeUBWithNewTree(StringTreeNode.lb))
        } else {
            var trees = accessState.accessDependees.map { ad =>
                if (ad.hasUBP) {
                    val tree = ad.ub.sci.tree
                    if (tree.parameterIndices.nonEmpty) {
                        // We cannot handle write values that contain parameter indices since resolving the parameters
                        // requires context and this interpreter is present in multiple contexts.
                        if (soundnessMode.isHigh) StringTreeNode.lb
                        else StringTreeNode.ub
                    } else
                        tree
                } else StringTreeNode.ub
            }
            // No init is present => append a `null` element to indicate that the field might be null; this behavior
            // could be refined by only setting the null element if no statement is guaranteed to be executed prior
            // to the field read
            if (accessState.fieldAccessDependee.isFinal && !accessState.hasInit) {
                trees = trees :+ StringTreeNull
            }

            if (accessState.hasUnresolvableAccess && soundnessMode.isHigh) {
                trees = trees :+ StringTreeNode.lb
            }

            if (accessState.hasDependees) {
                InterimResult.forUB(
                    InterpretationHandler.getEntity,
                    computeUBWithNewTree(StringTreeOr(trees)),
                    accessState.dependees.toSet,
                    continuation(accessState, state)
                )
            } else {
                computeFinalResult(computeUBWithNewTree(StringTreeOr(trees)))
            }
        }
    }

    private def continuation(
        accessState: FieldReadState,
        state:       InterpretationState
    )(eps: SomeEPS): ProperPropertyComputationResult = {
        eps match {
            case UBP(ub: FieldWriteAccessInformation) =>
                accessState.fieldAccessDependee = eps.asInstanceOf[EOptionP[DeclaredField, FieldWriteAccessInformation]]
                handleFieldAccessInformation(ub)(accessState, state)

            case UBP(_: StringConstancyProperty) =>
                accessState.updateAccessDependee(eps.asInstanceOf[EOptionP[VariableDefinition, StringConstancyProperty]])
                tryComputeFinalResult(accessState, state)

            case _ => throw new IllegalArgumentException(s"Encountered unknown eps: $eps")
        }
    }

    private def computeUBWithNewTree(newTree: StringTreeNode)(
        implicit
        accessState: FieldReadState,
        state:       InterpretationState
    ): StringFlowFunctionProperty = {
        accessState.previousResults.prepend(newTree)

        StringFlowFunctionProperty.constForVariableAt(
            state.pc,
            accessState.target,
            StringTreeOr(accessState.previousResults.toSeq)
        )
    }
}

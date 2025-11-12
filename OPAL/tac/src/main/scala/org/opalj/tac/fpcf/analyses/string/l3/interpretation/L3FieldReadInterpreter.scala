/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package string
package l3
package interpretation

import scala.collection.mutable.ListBuffer

import org.opalj.br.ClassType
import org.opalj.br.DeclaredField
import org.opalj.br.FieldType
import org.opalj.br.PUVar
import org.opalj.br.analyses.DeclaredFields
import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.analyses.ContextProvider
import org.opalj.br.fpcf.properties.fieldaccess.FieldWriteAccessInformation
import org.opalj.br.fpcf.properties.fieldaccess.NoFieldWriteAccessInformation
import org.opalj.br.fpcf.properties.string.StringConstancyProperty
import org.opalj.br.fpcf.properties.string.StringTreeConst
import org.opalj.br.fpcf.properties.string.StringTreeNode
import org.opalj.br.fpcf.properties.string.StringTreeNull
import org.opalj.br.fpcf.properties.string.StringTreeOr
import org.opalj.fpcf.EOptionP
import org.opalj.fpcf.EPS
import org.opalj.fpcf.InterimEUBP
import org.opalj.fpcf.InterimResult
import org.opalj.fpcf.ProperPropertyComputationResult
import org.opalj.fpcf.PropertyStore
import org.opalj.fpcf.SomeEOptionP
import org.opalj.fpcf.SomeEPS
import org.opalj.fpcf.UBP
import org.opalj.tac.fpcf.analyses.string.interpretation.InterpretationHandler
import org.opalj.tac.fpcf.analyses.string.interpretation.InterpretationState
import org.opalj.tac.fpcf.properties.string.StringFlowFunctionProperty

/**
 * Interprets direct reads to fields (see [[FieldRead]]) by analyzing the write accesses to these fields via the
 * [[FieldWriteAccessInformation]] and the possible string values passed to these write accesses.
 *
 * @author Maximilian Rüsch
 */
class L3FieldReadInterpreter(
    implicit
    val ps:              PropertyStore,
    val project:         SomeProject,
    val declaredFields:  DeclaredFields,
    val contextProvider: ContextProvider,
    val highSoundness:   Boolean
) extends AssignmentBasedStringInterpreter {

    override type E = FieldRead[V]

    private case class FieldReadState(
        field:                     DeclaredField,
        target:                    PV,
        var fieldAccessDependee:   EOptionP[DeclaredField, FieldWriteAccessInformation],
        var hasWriteInSameMethod:  Boolean                                                    = false,
        var hasInit:               Boolean                                                    = false,
        var hasUnresolvableAccess: Boolean                                                    = false,
        var accessDependees:       Seq[EOptionP[VariableDefinition, StringConstancyProperty]] = Seq.empty,
        previousResults:           ListBuffer[StringTreeNode]                                 = ListBuffer.empty
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

    /**
     * Checks whether the given type is supported by the field read analysis, i.e. if it may contain values desirable
     * AND resolvable by the string analysis as a whole.
     */
    @inline
    private def isSupportedType(fieldType: FieldType): Boolean = fieldType.isBaseType || (fieldType eq ClassType.String)

    override def interpretExpr(target: PV, fieldRead: E)(implicit
        state: InterpretationState
    ): ProperPropertyComputationResult = {
        if (!isSupportedType(fieldRead.declaredFieldType)) {
            return failure(target)
        }

        fieldRead.resolveField match {
            case Some(field)
                if !highSoundness && field.isStatic && field.isFinal && field.constantFieldValue.isDefined =>
                // The field is really a constant (apart from potential reflection shenanigans)
                return computeFinalResult(StringFlowFunctionProperty.constForVariableAt(
                    state.pc,
                    target,
                    StringTreeConst(field.constantFieldValue.get.valueToString)
                ))
            case _ =>
        }

        val field = declaredFields(fieldRead.declaringClass, fieldRead.name, fieldRead.declaredFieldType)
        val fieldAccessEOptP = ps(field, FieldWriteAccessInformation.key)

        implicit val accessState: FieldReadState =
            FieldReadState(field, target, InterimEUBP(field, NoFieldWriteAccessInformation))
        if (fieldAccessEOptP.hasUBP) {
            handleFieldAccessInformation(fieldAccessEOptP.asEPS)
        } else {
            InterimResult.forUB(
                InterpretationHandler.getEntity,
                StringFlowFunctionProperty.ub(state.pc, target),
                accessState.dependees.toSet,
                continuation(accessState, state)
            )
        }
    }

    private def handleFieldAccessInformation(accessDependee: EPS[DeclaredField, FieldWriteAccessInformation])(
        implicit
        accessState: FieldReadState,
        state:       InterpretationState
    ): ProperPropertyComputationResult = {
        val accessInformation = accessDependee.ub

        if (accessDependee.isFinal && accessInformation.accesses.isEmpty) {
            // No methods which write the field were found => Field could either be null or any value
            if (accessState.field.fieldType.isReferenceType) {
                return computeFinalResult(StringFlowFunctionProperty.constForVariableAt(
                    state.pc,
                    accessState.target,
                    StringTreeOr.fromNodes(failureTree, StringTreeNull)
                ))
            } else {
                return computeFinalResult(StringFlowFunctionProperty.constForVariableAt(
                    state.pc,
                    accessState.target,
                    failureTree
                ))
            }
        }

        accessInformation.getNewestAccesses(
            accessInformation.numDirectAccesses - accessState.fieldAccessDependee.ub.numDirectAccesses,
            accessInformation.numIndirectAccesses - accessState.fieldAccessDependee.ub.numIndirectAccesses
        ).foreach {
            case (contextId, pc, _, parameter) =>
                val method = contextProvider.contextFromId(contextId).method.definedMethod

                if (method == state.dm.definedMethod) {
                    accessState.hasWriteInSameMethod = true
                }

                if (method.isInitializer) {
                    accessState.hasInit = true
                }

                if (parameter.isEmpty) {
                    // Field parameter information is not available
                    accessState.hasUnresolvableAccess = true
                } else {
                    // IMPROVE use variable contexts here to support field writes based on method parameters in other
                    // methods. Requires a context to exist for variable definitions as well
                    val entity = VariableDefinition(pc, PUVar(parameter.get._1, parameter.get._2), method)
                    accessState.accessDependees = accessState.accessDependees :+ ps(entity, StringConstancyProperty.key)
                }
        }

        accessState.fieldAccessDependee = accessDependee

        computeResult
    }

    private def computeResult(implicit
        accessState: FieldReadState,
        state:       InterpretationState
    ): ProperPropertyComputationResult = {
        if (accessState.hasWriteInSameMethod && highSoundness) {
            // We cannot handle writes to a field that is read in the same method at the moment as the flow functions do
            // not capture field state. This can be improved upon in the future.
            computeFinalResult(StringFlowFunctionProperty.lb(state.pc, accessState.target))
        } else {
            var trees = accessState.accessDependees.map { ad =>
                if (ad.hasUBP) {
                    val tree = ad.ub.tree
                    if (tree.parameterIndices.nonEmpty) {
                        // We cannot handle write values that contain parameter indices since resolving the parameters
                        // requires context and this interpreter is present in multiple contexts.
                        tree.replaceParameters(tree.parameterIndices.map((_, failureTree)).toMap)
                    } else
                        tree
                } else StringTreeNode.ub
            }

            // Always append a "null" for soundness
            if (accessState.field.fieldType.isReferenceType) {
                trees = trees :+ StringTreeNull
            }

            if (accessState.hasUnresolvableAccess && highSoundness) {
                trees = trees :+ StringTreeNode.lb
            }

            val newUB = StringFlowFunctionProperty.constForVariableAt(state.pc, accessState.target, StringTreeOr(trees))
            if (accessState.hasDependees) {
                InterimResult.forUB(
                    InterpretationHandler.getEntity,
                    newUB,
                    accessState.dependees.toSet,
                    continuation(accessState, state)
                )
            } else {
                computeFinalResult(newUB)
            }
        }
    }

    private def continuation(
        accessState: FieldReadState,
        state:       InterpretationState
    )(eps: SomeEPS): ProperPropertyComputationResult = {
        eps match {
            case UBP(_: FieldWriteAccessInformation) =>
                handleFieldAccessInformation(eps.asInstanceOf[EPS[DeclaredField, FieldWriteAccessInformation]])(
                    using
                    accessState,
                    state
                )

            case UBP(_: StringConstancyProperty) =>
                accessState.updateAccessDependee(eps.asInstanceOf[EOptionP[VariableDefinition, StringConstancyProperty]])
                computeResult(using accessState, state)

            case _ => throw new IllegalArgumentException(s"Encountered unknown eps: $eps")
        }
    }
}

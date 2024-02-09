/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package string_analysis
package l0

import org.opalj.br.DeclaredMethod
import org.opalj.br.analyses.ProjectInformationKeys
import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.FPCFAnalysis
import org.opalj.br.fpcf.FPCFAnalysisScheduler
import org.opalj.br.fpcf.FPCFLazyAnalysisScheduler
import org.opalj.br.fpcf.properties.StringConstancyProperty
import org.opalj.br.fpcf.properties.cg.Callees
import org.opalj.br.fpcf.properties.string_definition.StringConstancyInformation
import org.opalj.fpcf.Entity
import org.opalj.fpcf.FinalEP
import org.opalj.fpcf.InterimLUBP
import org.opalj.fpcf.InterimResult
import org.opalj.fpcf.ProperPropertyComputationResult
import org.opalj.fpcf.PropertyBounds
import org.opalj.fpcf.PropertyStore
import org.opalj.fpcf.Result
import org.opalj.fpcf.SomeEPS
import org.opalj.fpcf.UBP
import org.opalj.tac.fpcf.analyses.string_analysis.interpretation.InterpretationHandler
import org.opalj.tac.fpcf.analyses.string_analysis.l0.interpretation.L0InterpretationHandler
import org.opalj.tac.fpcf.properties.TACAI

/**
 * This class is to be used to store state information that are required at a later point in
 * time during the analysis, e.g., due to the fact that another analysis had to be triggered to
 * have all required information ready for a final result.
 */
protected[l0] case class L0ComputationState(
        override val dm:     DeclaredMethod,
        override val entity: SContext
) extends ComputationState[L0ComputationState]

/**
 * IntraproceduralStringAnalysis processes a read operation of a local string variable at a program
 * position, ''pp'', in a way that it finds the set of possible strings that can be read at ''pp''.
 * <p>
 * This analysis takes into account only the enclosing function as a context, i.e., it is
 * intraprocedural. Values coming from other functions are regarded as dynamic values even if the
 * function returns a constant string value.
 * <p>
 * From a high-level perspective, this analysis works as follows. First, it has to be differentiated
 * whether string literals / variables or String{Buffer, Builder} are to be processed.
 * For the former, the definition sites are processed. Only one definition site is the trivial case
 * and directly corresponds to a leaf node in the string tree (such trees consist of only one node).
 * Multiple definition sites indicate > 1 possible initialization values and are transformed into a
 * string tree whose root node is an OR element and the children are the possible initialization
 * values. Note that all this is handled by [[StringConstancyInformation.reduceMultiple]].
 * <p>
 * For the latter, String{Buffer, Builder}, lean paths from the definition sites to the usage
 * (indicated by the given DUVar) is computed. That is, all paths from all definition sites to the
 * usage where only statements are contained that include the String{Builder, Buffer} object of
 * interest in some way (like an "append" or "replace" operation for example). These paths are then
 * transformed into a string tree by making use of a
 * [[org.opalj.tac.fpcf.analyses.string_analysis.preprocessing.PathTransformer]].
 *
 * @author Patrick Mell
 */
class L0StringAnalysis(override val project: SomeProject) extends StringAnalysis {

    override type State = L0ComputationState

    def analyze(data: SContext): ProperPropertyComputationResult = {
        // Retrieve TAC from property store
        val tacOpt: Option[TAC] = ps(data._2, TACAI.key) match {
            case UBP(tac) => if (tac.tac.isEmpty) None else Some(tac.tac.get)
            case _        => None
        }
        // No TAC available, e.g., because the method has no body
        if (tacOpt.isEmpty)
            return Result(data, StringConstancyProperty.lb) // TODO add continuation

        val tac = tacOpt.get
        val state = L0ComputationState(declaredMethods(data._2), data)
        state.iHandler = L0InterpretationHandler(tac)
        state.interimIHandler = L0InterpretationHandler(tac)
        state.tac = tac
        determinePossibleStrings(state)
    }

    override protected[string_analysis] def determinePossibleStrings(implicit
        state: State
    ): ProperPropertyComputationResult = {
        implicit val tac: TAC = state.tac
        val stmts = tac.stmts

        val puVar = state.entity._1
        val uVar = puVar.toValueOriginForm(tac.pcToIndex)
        val defSites = uVar.definedBy.toArray.sorted

        if (state.params.isEmpty) {
            state.params = StringAnalysis.getParams(state.entity)
        }

        if (state.params.isEmpty && defSites.exists(_ < 0)) {
            if (InterpretationHandler.isStringConstExpression(uVar)) {
                // We can evaluate string const expressions as function parameters
            } else if (StringAnalysis.isSupportedPrimitiveNumberType(uVar)) {
                val numType = uVar.value.asPrimitiveValue.primitiveType.toJava
                val sci = StringAnalysis.getDynamicStringInformationForNumberType(numType)
                return Result(state.entity, StringConstancyProperty(sci))
            } else {
                // StringBuilders as parameters are currently not evaluated
                return Result(state.entity, StringConstancyProperty.lb)
            }
        }

        if (state.parameterDependeesCount > 0) {
            return getInterimResult(state)
        } else {
            state.isSetupCompleted = true
        }

        // Interpret a function / method parameter using the parameter information in state
        if (defSites.head < 0) {
            val r = state.iHandler.processDefSite(defSites.head)(state)
            return Result(state.entity, StringConstancyProperty(r.asFinal.p.stringConstancyInformation))
        }

        val expr = stmts(defSites.head).asAssignment.expr
        if (InterpretationHandler.isStringBuilderBufferToStringCall(expr)) {
            val leanPath = computeLeanPathForStringBuilder(uVar)
            if (leanPath.isEmpty) {
                return Result(state.entity, StringConstancyProperty.lb)
            }
            state.computedLeanPath = leanPath.get

            // Find DUVars, that the analysis of the current entity depends on
            val dependentVars = findDependentVars(state.computedLeanPath, puVar)
            if (dependentVars.nonEmpty) {
                dependentVars.foreach { case (k, v) => state.appendToVar2IndexMapping(k, v) }
                dependentVars.keys.foreach { nextVar =>
                    propertyStore((nextVar, state.entity._2), StringConstancyProperty.key) match {
                        case FinalEP(e, p) =>
                            // Add mapping information (which will be used for computing the final result)
                            state.var2IndexMapping(e._1).foreach {
                                state.appendToFpe2Sci(_, p.stringConstancyInformation)
                            }
                            state.dependees = state.dependees.filter(_.e.asInstanceOf[SContext] != e)
                        case ep =>
                            state.dependees = ep :: state.dependees
                    }
                }
            }

            if (state.dependees.isEmpty) {
                computeFinalResult(state)
            } else {
                getInterimResult(state)
            }
        } else {
            // We deal with pure strings TODO unify result handling
            val sci = StringConstancyInformation.reduceMultiple(
                uVar.definedBy.toArray.sorted.map { ds =>
                    state.iHandler.processDefSite(ds).asFinal.p.stringConstancyInformation
                }
            )

            if (state.dependees.isEmpty) {
                Result(state.entity, StringConstancyProperty(sci))
            } else {
                getInterimResult(state)
            }
        }
    }

    /**
     * Continuation function.
     *
     * @param state The computation state (which was originally captured by `analyze` and possibly
     *              extended / updated by other methods involved in computing the final result.
     * @return This function can either produce a final result or another intermediate result.
     */
    override protected def continuation(
        state: State
    )(eps: SomeEPS): ProperPropertyComputationResult = eps match {
        case FinalEP(e: Entity, p: StringConstancyProperty) if eps.pk.equals(StringConstancyProperty.key) =>
            processFinalP(state, e, p)
            if (state.dependees.isEmpty) {
                computeFinalResult(state)
            } else {
                getInterimResult(state)
            }

        case InterimLUBP(lb, ub) =>
            InterimResult(state.entity, lb, ub, state.dependees.toSet, continuation(state))
        case _ =>
            throw new IllegalStateException("Could not process the continuation successfully.")
    }
}

sealed trait L0StringAnalysisScheduler extends FPCFAnalysisScheduler {

    final def derivedProperty: PropertyBounds = PropertyBounds.lub(StringConstancyProperty)

    override final def uses: Set[PropertyBounds] = Set(
        PropertyBounds.ub(TACAI),
        PropertyBounds.ub(Callees),
        PropertyBounds.lub(StringConstancyProperty)
    )

    override final type InitializationData = L0StringAnalysis
    override final def init(p: SomeProject, ps: PropertyStore): InitializationData = {
        new L0StringAnalysis(p)
    }

    override def beforeSchedule(p: SomeProject, ps: PropertyStore): Unit = {}

    override def afterPhaseScheduling(ps: PropertyStore, analysis: FPCFAnalysis): Unit = {}

    override def afterPhaseCompletion(
        p:        SomeProject,
        ps:       PropertyStore,
        analysis: FPCFAnalysis
    ): Unit = {}
}

object LazyL0StringAnalysis
    extends L0StringAnalysisScheduler with FPCFLazyAnalysisScheduler {

    override def register(
        p:        SomeProject,
        ps:       PropertyStore,
        analysis: InitializationData
    ): FPCFAnalysis = {
        val analysis = new L0StringAnalysis(p)
        ps.registerLazyPropertyComputation(StringConstancyProperty.key, analysis.analyze)
        analysis
    }

    override def derivesLazily: Some[PropertyBounds] = Some(derivedProperty)

    override def requiredProjectInformation: ProjectInformationKeys = Seq(EagerDetachedTACAIKey)
}

/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package string_analysis
package l1

import org.opalj.br.analyses.ProjectInformationKeys
import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.ContextProviderKey
import org.opalj.br.fpcf.properties.StringConstancyProperty
import org.opalj.br.fpcf.properties.cg.Callees
import org.opalj.fpcf.FinalEP
import org.opalj.fpcf.InterimResult
import org.opalj.fpcf.ProperPropertyComputationResult
import org.opalj.fpcf.PropertyBounds
import org.opalj.fpcf.PropertyStore
import org.opalj.fpcf.Result
import org.opalj.tac.fpcf.analyses.string_analysis.interpretation.InterpretationHandler
import org.opalj.tac.fpcf.analyses.string_analysis.l1.interpretation.L1InterpretationHandler
import org.opalj.tac.fpcf.properties.TACAI

/**
 * InterproceduralStringAnalysis processes a read operation of a string variable at a program
 * position, ''pp'', in a way that it finds the set of possible strings that can be read at ''pp''.
 * <p>
 * In comparison to [[org.opalj.tac.fpcf.analyses.string_analysis.l0.L0StringAnalysis]], this version tries to resolve
 * method calls that are involved in a string construction as far as possible.
 * <p>
 * The main difference in the intra- and interprocedural implementation is the following (see the
 * description of [[org.opalj.tac.fpcf.analyses.string_analysis.l0.L0StringAnalysis]] for a general overview):
 * This analysis can only start to transform the computed lean paths into a string tree (again using a
 * [[PathTransformer]]) after all relevant string values (determined by the [[L1InterpretationHandler]])
 * have been figured out. As the [[PropertyStore]] is used for recursively starting this analysis
 * to determine possible strings of called method and functions, the path transformation can take
 * place after all results for sub-expressions are available. Thus, the interprocedural
 * interpretation handler cannot determine final results, e.g., for the array interpreter or static
 * function call interpreter. This analysis handles this circumstance by first collecting all
 * information for all definition sites. Only when these are available, further information, e.g.,
 * for the final results of arrays or static function calls, are derived. Finally, after all
 * these information are ready as well, the path transformation takes place by only looking up what
 * string expression corresponds to which definition sites (remember, at this point, for all
 * definition sites all possible string values are known, thus look-ups are enough and no further
 * interpretation is required).
 *
 * @author Patrick Mell
 */
class L1StringAnalysis(val project: SomeProject) extends StringAnalysis {

    override def analyze(data: SContext): ProperPropertyComputationResult = {
        // IMPROVE enable handling call string contexts here (build a chain, probably via SContext)
        val state = ComputationState(declaredMethods(data._2), data)
        val iHandler = L1InterpretationHandler(project, ps)

        val tacaiEOptP = ps(data._2, TACAI.key)
        if (tacaiEOptP.isRefinable) {
            state.tacDependee = Some(tacaiEOptP)
            return getInterimResult(state, iHandler)
        }

        if (tacaiEOptP.ub.tac.isEmpty) {
            // No TAC available, e.g., because the method has no body
            return Result(state.entity, StringConstancyProperty.lb)
        }

        state.tac = tacaiEOptP.ub.tac.get

        determinePossibleStrings(state, iHandler)
    }

    /**
     * Takes the `data` an analysis was started with as well as a computation `state` and determines
     * the possible string values. This method returns either a final [[Result]] or an
     * [[org.opalj.fpcf.InterimResult]] depending on whether other information needs to be computed first.
     */
    override protected[string_analysis] def determinePossibleStrings(implicit
        state:    ComputationState,
        iHandler: InterpretationHandler
    ): ProperPropertyComputationResult = {
        val puVar = state.entity._1
        val uVar = puVar.toValueOriginForm(state.tac.pcToIndex)
        val defSites = uVar.definedBy.toArray.sorted

        if (state.tac == null) {
            return getInterimResult(state, iHandler)
        }

        if (defSites.exists(_ < 0)) {
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

        // Interpret a function / method parameter using the parameter information in state
        if (defSites.head < 0) {
            val ep = ps(InterpretationHandler.getEntityFromDefSite(defSites.head), StringConstancyProperty.key)
            if (ep.isRefinable) {
                state.dependees = ep :: state.dependees
                InterimResult.forLB(
                    state.entity,
                    StringConstancyProperty.lb,
                    state.dependees.toSet,
                    continuation(state, iHandler)
                )
            } else {
                return Result(state.entity, ep.asFinal.p)
            }
        }

        if (state.computedLeanPath == null) {
            state.computedLeanPath = computeLeanPath(uVar)(state.tac)
        }

        val call = state.tac.stmts(defSites.head).asAssignment.expr
        var attemptFinalResultComputation = true
        if (InterpretationHandler.isStringBuilderBufferToStringCall(call)) {
            // Find DUVars that the analysis of the current entity depends on
            findDependentVars(state.computedLeanPath, puVar)(state).keys.foreach { nextVar =>
                val ep = propertyStore((nextVar, state.entity._2), StringConstancyProperty.key)
                ep match {
                    case FinalEP(e, _) =>
                        state.dependees = state.dependees.filter(_.e != e)
                        // No more dependees => Return the result for this analysis run
                        if (state.dependees.isEmpty) {
                            return computeFinalResult(state)
                        } else {
                            return getInterimResult(state, iHandler)
                        }
                    case _ =>
                        state.dependees = ep :: state.dependees
                        attemptFinalResultComputation = false
                }
            }
        }

        if (state.dependees.nonEmpty) {
            getInterimResult(state, iHandler)
        } else {
            computeFinalResult(state)
        }
    }
}

object L1StringAnalysis {

    private[l1] final val FieldWriteThresholdConfigKey = {
        "org.opalj.fpcf.analyses.string_analysis.l1.L1StringAnalysis.fieldWriteThreshold"
    }
}

object LazyL1StringAnalysis extends LazyStringAnalysis {

    override final def uses: Set[PropertyBounds] = Set(PropertyBounds.ub(Callees)) ++ super.uses

    override final def init(p: SomeProject, ps: PropertyStore): InitializationData =
        (new L1StringAnalysis(p), L1InterpretationHandler(p, ps))

    override def requiredProjectInformation: ProjectInformationKeys = Seq(ContextProviderKey) ++
        L1InterpretationHandler.requiredProjectInformation ++
        super.requiredProjectInformation
}

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
import org.opalj.tac.fpcf.analyses.string_analysis.preprocessing.SimplePathFinder

/**
 * @author Maximilian Rüsch
 */
class L1StringAnalysis(val project: SomeProject) extends StringAnalysis {

    /**
     * Takes the `data` an analysis was started with as well as a computation `state` and determines
     * the possible string values. This method returns either a final [[Result]] or an
     * [[org.opalj.fpcf.InterimResult]] depending on whether other information needs to be computed first.
     */
    override protected[string_analysis] def determinePossibleStrings(implicit
        state: ComputationState
    ): ProperPropertyComputationResult = {
        implicit val tac: TAC = state.tac

        if (SimplePathFinder.containsComplexControlFlow(tac)) {
            return Result(state.entity, StringConstancyProperty.lb)
        }

        val uVar = state.entity._1.toValueOriginForm(tac.pcToIndex)
        val defSites = uVar.definedBy.toArray.sorted

        // Interpret a function / method parameter using the parameter information in state
        if (defSites.head < 0) {
            val ep = ps(
                InterpretationHandler.getEntityForDefSite(defSites.head, state.dm, tac),
                StringConstancyProperty.key
            )
            if (ep.isRefinable) {
                state.dependees = ep :: state.dependees
                return InterimResult.forUB(
                    state.entity,
                    StringConstancyProperty.ub,
                    state.dependees.toSet,
                    continuation(state)
                )
            } else {
                return Result(state.entity, ep.asFinal.p)
            }
        }

        if (state.computedLeanPath == null) {
            state.computedLeanPath = computeLeanPath(uVar)
        }

        getPCsInPath(state.computedLeanPath).foreach { pc =>
            propertyStore(
                InterpretationHandler.getEntityForPC(pc, state.dm, tac),
                StringConstancyProperty.key
            ) match {
                case FinalEP(e, _) =>
                    state.dependees = state.dependees.filter(_.e != e)
                case ep =>
                    state.dependees = ep :: state.dependees
            }
        }

        if (state.dependees.nonEmpty) {
            getInterimResult(state)
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

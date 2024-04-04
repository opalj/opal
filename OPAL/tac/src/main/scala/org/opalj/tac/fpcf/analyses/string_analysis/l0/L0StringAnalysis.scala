/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package string_analysis
package l0

import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.properties.StringConstancyProperty
import org.opalj.fpcf.FinalEP
import org.opalj.fpcf.InterimResult
import org.opalj.fpcf.ProperPropertyComputationResult
import org.opalj.fpcf.PropertyStore
import org.opalj.fpcf.Result
import org.opalj.tac.fpcf.analyses.string_analysis.interpretation.InterpretationHandler
import org.opalj.tac.fpcf.analyses.string_analysis.l0.interpretation.L0InterpretationHandler
import org.opalj.tac.fpcf.analyses.string_analysis.preprocessing.SimplePathFinder

/**
 * @author Maximilian Rüsch
 */
class L0StringAnalysis(override val project: SomeProject) extends StringAnalysis {

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
                InterpretationHandler.getEntityForDefSite(defSites.head, state.dm, state.tac),
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
                return Result(state.entity, StringConstancyProperty(ep.asFinal.p.sci))
            }
        }

        if (state.computedLeanPath == null) {
            state.computedLeanPath = computeLeanPath(uVar)
        }

        getPCsInPath(state.computedLeanPath).foreach { pc =>
            propertyStore(
                InterpretationHandler.getEntityForPC(pc, state.dm, state.tac),
                StringConstancyProperty.key
            ) match {
                case FinalEP(e, _) =>
                    state.dependees = state.dependees.filter(_.e != e)
                case ep =>
                    state.dependees = ep :: state.dependees
            }
        }

        if (state.dependees.isEmpty) {
            computeFinalResult(state)
        } else {
            getInterimResult(state)
        }
    }
}

object LazyL0StringAnalysis extends LazyStringAnalysis {

    override def init(p: SomeProject, ps: PropertyStore): InitializationData =
        (new L0StringAnalysis(p), L0InterpretationHandler()(p, ps))
}

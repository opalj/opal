/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.tac.fpcf.analyses.alias

import org.opalj.br.PC
import org.opalj.br.fpcf.properties.Context
import org.opalj.br.fpcf.properties.NoContext
import org.opalj.tac.Goto

trait AllocationSiteAndTacBasedAliasAnalysis extends AllocationSiteBasedAliasAnalysis with TacBasedAliasAnalysis {

    override protected[this] type AnalysisState <: AllocationSiteBasedAliasAnalysisState with TacBasedAliasAnalysisState

    override protected[this] def checkMustAlias(intersection: AllocationSiteBasedAliasSet)(implicit
        state:   AnalysisState,
        context: AnalysisContext
    ): Boolean = {

        val pointsTo1 = state.pointsTo1
        val pointsTo2 = state.pointsTo2

        if (intersection.size != 1 ||
            pointsTo1.size != 1 ||
            pointsTo2.size != 1
        ) return false

        // they refer to the same allocation site but aren't necessarily the same object (e.g. if the allocation site
        // is inside a loop or a different method and is executed multiple times)

        val (pointsToContext: Context, pc: PC) = pointsTo1.allPointsTo.head
        val method = pointsToContext match {
            case NoContext => context.element1.declaredMethod
            case _         => pointsToContext.method
        }

        if (context.element1.isAliasUVar &&
            context.element2.isAliasUVar &&
            context.element1.declaredMethod == method &&
            context.element2.declaredMethod == method
        ) {

            // Both elements are uVars that point to the same allocation site and both are inside the method of the allocation site
            // -> they must alias if the allocation site is executed only once (i.e. no loop or recursion)

            val defSite1 = context.element1.asAliasUVar.persistentUVar.defSites
            val defSite2 = context.element2.asAliasUVar.persistentUVar.defSites

            if (defSite1.size != 1 || defSite1.size != 1 || defSite1.head != defSite2.head) return false // multiple or different def sites for one element -> might be different objects (e.g. due to recursion via parameter)

            val tac = state.tacai1.get

            // the definition site is not the allocation site -> it is a method call or something similar
            if (pc != defSite1.head) return false

            for (stmt <- state.tacai1.get.stmts) {
                stmt match {
                    case goto: Goto =>
                        val targetPC = tac.stmts(goto.targetStmt).pc
                        if (targetPC <= pc && goto.pc >= pc) return false // jumping from behind the allocation site in front of it -> might break aliasing because allocation site is executed multiple times
                    case _ =>
                }
            }

            return true
        }

        false
    }
}

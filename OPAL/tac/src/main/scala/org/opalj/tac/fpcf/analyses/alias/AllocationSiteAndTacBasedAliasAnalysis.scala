/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package alias

import org.opalj.br.PC
import org.opalj.br.fpcf.properties.Context
import org.opalj.br.fpcf.properties.NoContext

trait AllocationSiteAndTacBasedAliasAnalysis extends AllocationSiteBasedAliasAnalysis with TacBasedAliasAnalysis {

    override protected[this] type AnalysisState <: AllocationSiteBasedAliasAnalysisState with TacBasedAliasAnalysisState

    override protected[this] def checkMustAlias(intersectingElement: AliasElementType)(implicit
        state:   AnalysisState,
        context: AnalysisContext
    ): Boolean = {

        val pointsTo1 = state.pointsTo1
        val pointsTo2 = state.pointsTo2

        if (pointsTo1.size != 1 || pointsTo2.size != 1) return false

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

            val defSite1 = context.element1.asAliasUVar.persistentUVar.defPCs
            val defSite2 = context.element2.asAliasUVar.persistentUVar.defPCs

            // multiple or different def sites for one element -> might be different objects (e.g. due to recursion via parameter)
            if (defSite1.size != 1 || defSite2.size != 1 || defSite1.head != defSite2.head) return false

            // the definition site is not the allocation site -> it is a method call or something similar
            if (pc != defSite1.head) return false

            val tac = state.tacai1.get
            val cfg = tac.cfg
            val domTree = cfg.dominatorTree
            val postDomTree = cfg.postDominatorTree
            val allocBB = cfg.bb(tac.properStmtIndexForPC(pc)).nodeId

            // check if the allocation site is dominated by a loop header, i.e., is executed multiple times
            domTree.foreachDom(allocBB)(dom => {

                // check if the dominator is a loop header
                cfg.foreachPredecessor(cfg.bb(dom).startPC)(pred => {

                    // if the dominator itself dominates one of its predecessors, it is a loop header
                    if (domTree.strictlyDominates(dom, pred)) {

                        // Only report a negative result if the allocation site is inside the loop.
                        // If the loop head post dominates the allocation site we know that the allocation is behind
                        // the loop.
                        if (!postDomTree.strictlyDominates(dom, allocBB)) {
                            return false
                        }
                    }
                })

            })

            return true
        }

        false
    }
}

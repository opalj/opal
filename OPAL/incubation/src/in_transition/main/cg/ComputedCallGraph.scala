/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai
package analyses.cg

import org.opalj.br.analyses.SomeProject
import org.opalj.br.Method

/**
 * Representation of a computed call graph.
 *
 * @param entryPoints A function to get the entry points that were used to start
 *      the calculation of the call graph.
 *
 * @author Michael Eichberg
 */
class ComputedCallGraph(
        val callGraph:              CallGraph,
        val entryPoints:            () ⇒ Iterable[Method],
        val unresolvedMethodCalls:  List[UnresolvedMethodCall],
        val constructionExceptions: List[CallGraphConstructionException]
)

object ComputedCallGraph {

    def apply(
        callGraph:              CallGraph,
        entryPoints:            () ⇒ Iterable[Method],
        unresolvedMethodCalls:  List[UnresolvedMethodCall],
        constructionExceptions: List[CallGraphConstructionException]
    ): ComputedCallGraph = {
        new ComputedCallGraph(
            callGraph,
            entryPoints,
            unresolvedMethodCalls,
            constructionExceptions
        )
    }

    def unapply(
        cg: ComputedCallGraph
    ): Some[(CallGraph, List[UnresolvedMethodCall], List[CallGraphConstructionException])] =
        Some((cg.callGraph, cg.unresolvedMethodCalls, cg.constructionExceptions))

    def empty(project: SomeProject): ComputedCallGraph = {
        apply(
            new CallGraph(project, Map.empty, Map.empty),
            () ⇒ List.empty,
            List.empty,
            List.empty
        )
    }
}

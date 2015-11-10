/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2014
 * Software Technology Group
 * Department of Computer Science
 * Technische Universität Darmstadt
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  - Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package org.opalj
package ai
package analyses
package cg

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
    ) =
        new ComputedCallGraph(
            callGraph,
            entryPoints,
            unresolvedMethodCalls,
            constructionExceptions
        )

    def unapply(
        cg: ComputedCallGraph
    ): Some[(CallGraph, List[UnresolvedMethodCall], List[CallGraphConstructionException])] =
        Some((cg.callGraph, cg.unresolvedMethodCalls, cg.constructionExceptions))

    def empty(project: SomeProject) =
        apply(
            new CallGraph(project, Map.empty, Map.empty),
            () ⇒ List.empty,
            List.empty,
            List.empty
        )
}

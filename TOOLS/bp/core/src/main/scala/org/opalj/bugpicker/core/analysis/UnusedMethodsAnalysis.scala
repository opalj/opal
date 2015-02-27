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
package bugpicker
package core
package analysis

import org.opalj.ai.AIResult
import org.opalj.ai.analyses.cg.VTACallGraphKey
import org.opalj.bi.VisibilityModifier
import org.opalj.br.ClassFile
import org.opalj.br.Method
import org.opalj.br.analyses.SomeProject

/**
 * A static analysis of unused methods and constructors backed by callgraphs.
 *
 * Currently using the VTACallGraphAlgorithm.
 *
 * @author Marco Jacobasch
 */
object UnusedMethodsAnalysis {

    /**
     * Analyze if the method is never called.
     *
     * If any of the following conditions is true, it will assume the method as being called.
     *
     * - The method is an entry point of the call graph. See also [[org.opalj.ai.analysis.cg.CallGraphFactory.defaultEntryPointsForLibraries]].
     * - The method is a static initializer.
     * - The method is a private constructor in a final class.
     *
     * TODO: reevaluate private constructors, if entry points will include them.
     *
     */
    def analyze(
        theProject: SomeProject, classFile: ClassFile, method: Method,
        result: AIResult): Seq[StandardIssue] = {

        var results: Seq[StandardIssue] = Seq.empty
        val callgraph = theProject.get(VTACallGraphKey)
        val callgraphEntryPoints = callgraph.entryPoints().toSeq

        val ignoreMethod = callgraphEntryPoints.contains(method) || (method.isConstructor && method.isPrivate && classFile.isFinal)

        if (!ignoreMethod) {
            val possibleCallers = callgraph.callGraph calledBy method
            val methodNotCalled = possibleCallers.isEmpty

            if (methodNotCalled) {
                val description = methodOrConstructor(method)

                // the unused method or constructor issue
                val unusedMethodIssue = StandardIssue(
                    theProject, classFile, Some(method), None,
                    None,
                    None,
                    "",
                    Some(description),
                    Set(IssueCategory.Comprehensibility),
                    Set(IssueKind.Unused),
                    Seq(),
                    Relevance.DefaultRelevance
                )
                results = results :+ unusedMethodIssue
            }
        }

        results
    }

    def methodOrConstructor(method: Method): String = method.isConstructor match {
        case true ⇒ s"This ${access(method.accessFlags)} class will never be instantiated."
        case _    ⇒ s"This ${access(method.accessFlags)} method will never be called at runtime."
    }

    def access(flags: Int): String = VisibilityModifier.get(flags) match {
        case Some(visiblity) ⇒ visiblity.javaName.getOrElse("unknown")
        case _               ⇒ "default"
    }

}


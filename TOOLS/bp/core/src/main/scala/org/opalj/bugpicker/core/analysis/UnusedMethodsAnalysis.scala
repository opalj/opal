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

import org.opalj.bi.VisibilityModifier
import org.opalj.br.ClassFile
import org.opalj.br.Method
import org.opalj.br.analyses.SomeProject
import org.opalj.ai.analyses.cg.ComputedCallGraph

/**
 * Identifies unused methods and constructors based on the call graph.
 *
 * Currently using the VTACallGraphAlgorithm.
 *
 * @author Marco Jacobasch
 * @author Michael Eichberg
 */
object UnusedMethodsAnalysis {

    /**
     * Finds those methods that are never called.
     *
     * If any of the following conditions is true, it will assume the method as being called.
     * - The method is the target of a method call in the calculated call graph.
     * - The method is a private default constructor in a final class. Such constructors
     *      are usually defined to avoid instantiations of the respective class.
     */
    def analyze(
        theProject: SomeProject,
        callgraph: ComputedCallGraph, callgraphEntryPoints: Set[Method],
        classFile: ClassFile, method: Method): Option[StandardIssue] = {

        def ignoreMethod(): Boolean = {
            val ignoreIt =
                callgraphEntryPoints.contains(method) || (
                    // it is basically a default constructor ...
                    method.isConstructor && method.isPrivate && method.parametersCount == 1 /*this*/ &&
                    method.body.isDefined && method.body.get.instructions.size == 5 &&
                    // ... which was (usually) defined to avoid instantiations of the
                    // class (e.g., java.lang.Math)
                    classFile.constructors.size == 1
                )
            ignoreIt
        }

        val callers = callgraph.callGraph calledBy method
        if (callers.isEmpty && !ignoreMethod) {
            val description = methodOrConstructor(method)
            // the unused method or constructor issue
            Some(StandardIssue(
                theProject, classFile, Some(method), None,
                None,
                None,
                "unused method",
                Some(description),
                Set(IssueCategory.Comprehensibility),
                Set(IssueKind.Unused),
                Seq(),
                Relevance.DefaultRelevance
            ))
        } else
            None
    }

    def methodOrConstructor(method: Method): String = {
        if (method.isConstructor)
            s"the ${access(method.accessFlags)} constructor is not used"
        else
            s"the ${access(method.accessFlags)} method is not used"
    }

    def access(flags: Int): String =
        VisibilityModifier.get(flags) match {
            case Some(visiblity) ⇒ visiblity.javaName.get
            case _               ⇒ "/*default*/"
        }

}


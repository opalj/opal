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
import org.opalj.br.instructions.ReturnInstruction

/**
 * Identifies unused methods and constructors using the given call graph.
 *
 * @author Marco Jacobasch
 * @author Michael Eichberg
 */
object UnusedMethodsAnalysis {

    /**
     * Checks if the given method is used/is potentially useable. If the method is not used
     * and is also not potentially useable by future clients then an issue is created
     * and returned.
     *
     * If any of the following conditions is true the method is considered as being called.
     * - The method is the target of a method call in the calculated call graph.
     * - The method is a private (empty) default constructor in a final class. Such constructors
     *      are usually defined to avoid instantiations of the respective class.
     * - The method is a private constructor in a final class that always throws an exception.
     *      Such constructors are usually defined to avoid instantiations of the
     *      respective class. E.g.
     *      `private XYZ(){throw new UnsupportedOperationException()`
     */
    def analyze(
        theProject: SomeProject,
        callgraph:  ComputedCallGraph, callgraphEntryPoints: Set[Method],
        classFile: ClassFile, method: Method
    ): Option[StandardIssue] = {

        if (callgraphEntryPoints.contains(method))
            return None; // <=== early return

        def rateMethod(): Relevance = {

            import method.{isConstructor, isPrivate, parametersCount}

            // Let's check if it is a default constructor
            // which was defined to avoid instantiations of the
            // class (e.g., java.lang.Math)
            val isDefaultConstructor = isConstructor && isPrivate && parametersCount == 1 /*this*/
            if (!isDefaultConstructor)
                return Relevance.DefaultRelevance; // <=== early return

            val constructorsIterator = classFile.constructors
            constructorsIterator.next // <= we have at least one constructor
            if (constructorsIterator.hasNext)
                // we have (among others) a default constructor that is not used
                return Relevance.High; // <=== early return

            val body = method.body.get
            val instructions = body.instructions
            def justThrowsException: Boolean = {
                !body.exists { (pc, i) ⇒ /* <= it just throws exceptions */
                    ReturnInstruction.isReturnInstruction(i)
                }
            }
            if (instructions.size == 5 /* <= default empty constructor */ )
                Relevance.TechnicalArtifact
            else if (justThrowsException)
                Relevance.CommonIdiom
            else
                Relevance.DefaultRelevance
        }

        val callers = callgraph.callGraph calledBy method
        if (callers.isEmpty) {

            val description =
                methodOrConstructor(method) + (
                    if (!method.isPrivate) "the class is not instantiable" else ""
                )
            val relevance: Relevance = rateMethod()
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
                relevance
            ))
        } else
            None
    }

    def methodOrConstructor(method: Method): String = {
        def access(flags: Int): String =
            VisibilityModifier.get(flags) match {
                case Some(visiblity) ⇒ visiblity.javaName.get
                case _               ⇒ "/*default*/"
            }

        if (method.isConstructor)
            s"the ${access(method.accessFlags)} constructor is not used"
        else
            s"the ${access(method.accessFlags)} method is not used"
    }

}


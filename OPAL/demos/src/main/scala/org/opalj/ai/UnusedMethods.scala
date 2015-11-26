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

import java.net.URL
import org.opalj.br.Method
import org.opalj.br.ObjectType
import org.opalj.br.analyses.BasicReport
import org.opalj.br.analyses.Project
import org.opalj.ai.analyses.cg.{CallGraphFactory, VTACallGraphKey, ComputedCallGraph}
import org.opalj.br.MethodDescriptor
import org.opalj.br.analyses.DefaultOneStepAnalysis

/**
 * A shallow analysis that tries to identify ((package) private) methods that are dead.
 *
 * @author Michael Eichberg
 */
object UnusedMethods extends DefaultOneStepAnalysis {

    override def title: String = "Dead methods"

    override def description: String = "Identifies methods that are never called."

    override def doAnalyze(
        theProject:    Project[URL],
        parameters:    Seq[String],
        isInterrupted: () ⇒ Boolean
    ) = {
        implicit val classHierarchy = theProject.classHierarchy
        import classHierarchy.isSubtypeOf

        val results = {
            val ComputedCallGraph(callGraph, _, _) = theProject.get(VTACallGraphKey)
            for {
                classFile ← theProject.allProjectClassFiles.par
                if !isInterrupted()
                method ← classFile.methods
                if method.body.isDefined
                if method.isPrivate || method.hasDefaultVisibility
                if callGraph.calledBy(method).isEmpty
                if !(
                    (method.name == "<clinit>" || method.name == "<init>") &&
                    method.descriptor == MethodDescriptor.NoArgsAndReturnVoid
                )
                if !(
                    CallGraphFactory.isPotentiallySerializationRelated(classFile, method) &&
                    isSubtypeOf(classFile.thisType, ObjectType.Serializable).isYesOrUnknown
                )
            } yield {
                (classFile, method)
            }
        }
        val sortedResults =
            (
                results.seq.toSeq.sortWith { (e1, e2) ⇒
                    val (e1ClassFile, e1Method) = e1
                    val (e2ClassFile, e2Method) = e2
                    val e1FQN = e1ClassFile.thisType.fqn
                    val e2FQN = e2ClassFile.thisType.fqn
                    e1FQN < e2FQN || (e1FQN == e2FQN && e1Method < e2Method)
                }
            ).map(e ⇒ e._2.fullyQualifiedSignature(e._1.thisType))

        val msg = sortedResults.mkString("Dead Methods: "+results.size+"): \n", "\n", "\n")
        BasicReport(msg)
    }

}

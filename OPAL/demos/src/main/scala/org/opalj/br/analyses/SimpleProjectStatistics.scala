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
package br
package analyses

import java.net.URL

/**
 * Demonstrates how to collect some statistics about the analyzed project
 * (created for Entwicklertag 2015 in Frankfurt).
 *
 * @author Michael Eichberg
 */
object SimpleProjectStatistics extends DefaultOneStepAnalysis {

    override def title: String = "Collects Project Statistics"

    override def description: String =
        "Collects basic size metrics about a project."

    override def doAnalyze(
        project:       Project[URL],
        parameters:    Seq[String],
        isInterrupted: () ⇒ Boolean
    ) = {

        // the following is highly inefficient

        val classFilesDistribution =
            project.allClassFiles.
                groupBy(cf ⇒ org.opalj.bi.jdkVersion(cf.majorVersion)).toSeq.
                sortWith((l, r) ⇒ l._1 < r._1).
                map { e ⇒
                    val (group, es) = e
                    (group, es.size)
                }

        val maxInstanceFieldsInAClass =
            project.allClassFiles.map(_.fields.filter(f ⇒ !f.isStatic).size).max
        val classWithMaxInstanceFields =
            project.allClassFiles.find(
                _.fields.filter(f ⇒ !f.isStatic).size == maxInstanceFieldsInAClass
            ).map(_.thisType.toJava)

        val maxClassFieldsInAClass =
            project.allClassFiles.map(_.fields.filter(f ⇒ f.isStatic).size).max
        val classWithMaxClassFields =
            project.allClassFiles.find(
                _.fields.filter(f ⇒ f.isStatic).size == maxClassFieldsInAClass
            ).map(_.thisType.toJava)

        val maxMethodsInAClass =
            project.allClassFiles.map(_.methods.size).max
        val classWithMaxMethods =
            project.allClassFiles.find(
                _.methods.size == maxMethodsInAClass
            ).map(_.thisType.toJava)

        val (longestMethodInAClass, theLongestMethod) =
            {
                var max = 0
                var methodName: String = null
                for {
                    classFile ← project.allClassFiles
                    method ← classFile.methods
                    if method.body.isDefined
                    size = method.body.get.programCounters.size
                    if size > max
                } {
                    max = size
                    methodName = method.toJava(classFile)
                }

                (max, methodName)
            }

        val (methodWithMostRegisterVariableInAClass, theMethodWithTheMostLocalVariables) =
            {
                var max = 0
                var methodName: String = null

                for {
                    classFile ← project.allClassFiles
                    method ← classFile.methods
                    if method.body.isDefined
                    count = method.body.get.maxLocals
                    if count > max
                } {
                    max = count
                    methodName = method.toJava(classFile)
                }

                (max, methodName)
            }

        BasicReport(
            classFilesDistribution.mkString("classFilesDistribution:\n\t", "\n\t", "\n")+
                "maxInstanceFieldsInAClass: "+maxInstanceFieldsInAClass+"("+classWithMaxInstanceFields+")\n"+
                "maxClassFieldsInAClass: "+maxClassFieldsInAClass+"("+classWithMaxClassFields+")\n"+
                "maxMethodsInAClass: "+maxMethodsInAClass+"("+classWithMaxMethods+")\n"+
                "longestMethodInAClass: "+longestMethodInAClass+"("+theLongestMethod+")\n"+
                "methodWithMostRegisterVariableInAClass: "+methodWithMostRegisterVariableInAClass+"("+theMethodWithTheMostLocalVariables+")\n"
        )
    }
}

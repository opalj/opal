/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2016
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

import org.opalj.br.analyses.DefaultOneStepAnalysis
import org.opalj.br.analyses.Project

import org.opalj.br.analyses.BasicReport
import org.opalj.ai.analyses.cg.VTACallGraphKey
import org.opalj.br.analyses.SourceElementsPropertyStoreKey
import org.opalj.fpcf.analysis.FPCFAnalysesManagerKey
import org.opalj.fpcf.analysis.methods.CallableFromClassesInOtherPackagesAnalysis
import org.opalj.fpcf.analysis.methods.IsClientCallable
import org.opalj.fpcf.analysis.methods.NotClientCallable

/**
 * Identifies all native methods in a project and checks, which ones can be called outside the
 * project.
 *
 * @author Mario Trageser
 */
object ClientCallableNativeMethods extends DefaultOneStepAnalysis {

    override def title: String = "Native Methods Usage"

    override def description: String = "Identifies all native methods in a project and checks, "+
        "which ones can be called outside the project."

    override def doAnalyze(
        project:       Project[URL],
        parameters:    Seq[String],
        isInterrupted: () ⇒ Boolean
    ) = {

        val callGraph = project.get(VTACallGraphKey).callGraph

        var statistics = s"Number of all methods: ${project.projectMethodsCount}.\n"

        val nativeMethods = project.allClassFiles.flatMap(_.methods.view.filter(_.isNative))
        val percentNativeMethods = (nativeMethods.size.toDouble / project.projectMethodsCount) * 100.0d
        statistics = statistics + s"Number of native methods: ${nativeMethods.size} "
        statistics = statistics + f"($percentNativeMethods%2.2f%%).\n"

        val propertyStore = project.get(SourceElementsPropertyStoreKey)
        val fpcfManager = project.get(FPCFAnalysesManagerKey)
        fpcfManager.run(CallableFromClassesInOtherPackagesAnalysis)

        // TODO In the following the name of the local variable and the text of the report don't fit! 
        
        val notClientCallables = for {
            method ← nativeMethods
            callableInformation = propertyStore(method, IsClientCallable.key)
            if !(callableInformation eq NotClientCallable) && callGraph.calledBy(method).size > 0
        } yield method

        val notClienCallablesDesc = notClientCallables.map(method ⇒ method.toJava(project.classFile(method))).mkString("\n")
        BasicReport(
                statistics +
                notClienCallablesDesc + 
                s"\nIdentified ${notClientCallables.size} client callable native methods.")
    }
}
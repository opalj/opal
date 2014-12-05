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

import java.io.File

import org.opalj.ai.project.CHACallGraphKey
import org.opalj.ai.project.VTACallGraphKey
import org.opalj.br.MethodWithBody
import org.opalj.br.analyses.Project
import org.opalj.br.analyses.SomeProject
import org.opalj.br.instructions.INVOKEDYNAMIC
import org.opalj.br.reader.Java8Framework
import org.opalj.br.reader.Java8FrameworkWithLambdaSupport
import org.opalj.util.JRELibraryFolder
import org.opalj.util.RTJar

/**
 * Creates call graphs for the JRE's rt.jar and whole lib/ folder with and without
 * invokedynamic resolution and compares the resulting numbers of call edges.
 */
object InvokedynamicResolutionImpactOnCallGraphsEvaluator {
    def countInvokedynamics(project: SomeProject): Int = {
        (for {
            classFile ← project.projectClassFiles
            MethodWithBody(body) ← classFile.methods
        } yield body.instructions.filter(_.isInstanceOf[INVOKEDYNAMIC]).size).sum
    }

    def main(args: Array[String]) {
        // You'll probably want to comment some of this out unless you have a LOT of RAM

        println(runCHA(RTJar))
        println(runVTA(RTJar))
        println(runCHA(JRELibraryFolder))
        println(runVTA(JRELibraryFolder))
    }

    def runCHA(file: File): String = {
        val projectWithoutIndy = Project(Java8Framework.ClassFiles(file))
        val callEdgesWithoutInvokedynamicResolution =
            projectWithoutIndy.get(CHACallGraphKey).callGraph.callEdgesCount
        val projectWithIndy =
            Project(Java8FrameworkWithLambdaSupport.ClassFiles(file))
        val callEdgesWithInvokedynamicResolution =
            projectWithIndy.get(CHACallGraphKey).callGraph.callEdgesCount

        val report = s"Running CHACallGraph on ${file}:\n"+
            s"Total # of invokedynamic instructions: ${countInvokedynamics(projectWithoutIndy)}\n"+
            s"\t${callEdgesWithoutInvokedynamicResolution} call edges without "+
            "resolving invokedynamic instructions and\n"+
            s"\t${callEdgesWithInvokedynamicResolution} call edges with "+
            "resolving invokedynamic instructions.\n"

        report
    }

    def runVTA(file: File): String = {
        val projectWithoutIndy = Project(Java8Framework.ClassFiles(file))
        val callEdgesWithoutInvokedynamicResolution =
            projectWithoutIndy.get(VTACallGraphKey).callGraph.callEdgesCount

        val projectWithIndy = Project(Java8FrameworkWithLambdaSupport.ClassFiles(file))
        val callEdgesWithInvokedynamicResolution =
            projectWithIndy.get(VTACallGraphKey).callGraph.callEdgesCount

        val report = s"Running VTACallGraph on ${file}:\n"+
            s"Total # of invokedynamic instructions: ${countInvokedynamics(projectWithoutIndy)}\n"+
            s"\t${callEdgesWithoutInvokedynamicResolution} call edges without "+
            "resolving invokedynamic instructions and\n"+
            s"\t${callEdgesWithInvokedynamicResolution} call edges with "+
            "resolving invokedynamic instructions."

        report
    }
}
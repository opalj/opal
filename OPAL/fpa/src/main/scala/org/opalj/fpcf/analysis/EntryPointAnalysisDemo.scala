/* BSD 2Clause License:
 * Copyright (c) 2009 - 2015
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
package fpcf
package analysis

import org.opalj.fpcf.MethodAnalysisDemo
import org.opalj.br.analyses.BasicReport
import java.net.URL
import org.opalj.br.analyses.SourceElementsPropertyStoreKey
import org.opalj.log.OPALLogger
import org.opalj.log.LogContext
import org.opalj.log.DefaultLogContext
import org.opalj.log.GlobalLogContext
import org.opalj.ai.analyses.cg.CallGraphFactory
import org.opalj.ai.domain.LogContextProvider
import org.opalj.log.ConsoleOPALLogger
import org.opalj.log.Warn

/**
 * @author Michael Reif
 */
object EntryPointAnalysisDemo extends MethodAnalysisDemo {

    OPALLogger.updateLogger(GlobalLogContext, new ConsoleOPALLogger(true, Warn))

    override def title: String =
        "entry point computation"

    override def description: String =
        "determines the factory methods of a library"

    val dependees = Seq(
        StaticMethodAccessibilityAnalysis,
        LibraryLeakageAnalysis,
        FactoryMethodAnalysis,
        InstantiabilityAnalysis,
        MethodAccessibilityAnalysis
    )

    override def doAnalyze(
        project:       org.opalj.br.analyses.Project[URL],
        parameters:    Seq[String],
        isInterrupted: () ⇒ Boolean
    ): BasicReport = {

        val propertyStore = project.get(SourceElementsPropertyStoreKey)

        val oldEntryPoints = CallGraphFactory.defaultEntryPointsForLibraries(project).size
        val projectInfo =
            s"JDK #methods ${project.methodsCount}"+
                s"old #entryPoints ${oldEntryPoints}"+
                s"old #nonEntryPoints ${project.methodsCount - oldEntryPoints}"

        var analysisTime = org.opalj.util.Seconds.None
        org.opalj.util.PerformanceEvaluation.time {
            var fpaThreads = Seq.empty[Thread]
            dependees foreach { fpa ⇒
                fpaThreads = fpaThreads :+ new Thread(
                    new Runnable { def run = fpa.analyze(project) }
                )
            }
            fpaThreads = fpaThreads :+ new Thread(
                new Runnable { def run = EntryPointsAnalysis.analyze(project) }
            )

            fpaThreads foreach (_.start)
            fpaThreads foreach (_.join)
            propertyStore.waitOnPropertyComputationCompletion( /*default: true*/ )
        } { t ⇒ analysisTime = t.toSeconds }

        val entryPoints = entitiesByProperty(IsEntryPoint)(propertyStore)
        val noEntryPoints = entitiesByProperty(NoEntryPoint)(propertyStore)

        BasicReport(
            //            entryPointInfo +
            //                propertyStore.toString +
            projectInfo +
                s"\nsize: ${entryPoints.size}"+
                s"\nsize: ${noEntryPoints.size}"+
                "\nAnalysis time: "+analysisTime
        )
    }
}
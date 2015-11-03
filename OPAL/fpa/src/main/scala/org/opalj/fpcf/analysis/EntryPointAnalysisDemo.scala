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

import org.opalj.br.analyses.BasicReport
import java.net.URL
import org.opalj.br.analyses.SourceElementsPropertyStoreKey
import org.opalj.ai.analyses.cg.CallGraphFactory
import org.opalj.log.OPALLogger
import org.opalj.log.Info
import org.opalj.log.ConsoleOPALLogger
import org.opalj.log.GlobalLogContext

/**
 * @author Michael Reif
 */
object EntryPointAnalysisDemo extends MethodAnalysisDemo {

    OPALLogger.updateLogger(GlobalLogContext, new ConsoleOPALLogger(true, Info))

    override def title: String = "Determines the entry points for the given project."

    override def description: String =
        "Determines all methods that are initial entry points when construction a call graph."

    override def doAnalyze(
        project:       org.opalj.br.analyses.Project[URL],
        parameters:    Seq[String],
        isInterrupted: () ⇒ Boolean
    ): BasicReport = {

        var oldEntryPoints = 0

        var oldTime = org.opalj.util.Seconds.None
        org.opalj.util.PerformanceEvaluation.time {

            oldEntryPoints = CallGraphFactory.defaultEntryPointsForLibraries(project).size
        } { t ⇒ oldTime = t.toSeconds }
        //        val projectInfo =
        //            s"Overall #methods ${project.methodsCount} "+
        //                s"\nold #entryPoints ${oldEntryPoints} "+
        //                s"\nold #nonEntryPoints ${project.methodsCount - oldEntryPoints}"+
        //                s"\nanalysisTime: $oldTime\n\n"

        val executer = project.get(FPCFAnalysisManagerKey)

        var analysisTime = org.opalj.util.Seconds.None
        org.opalj.util.PerformanceEvaluation.time {

            executer.runAll(
                SimpleInstantiabilityAnalysis,
                LibraryLeakageAnalysis,
                //FactoryMethodAnalysis,
                //InstantiabilityAnalysis
                MethodAccessibilityAnalysis
            )

            executer.run(EntryPointsAnalysis)

        } { t ⇒ analysisTime = t.toSeconds }

        val propertyStore = project.get(SourceElementsPropertyStoreKey)

        val entryPoints = entitiesByProperty(IsEntryPoint)(propertyStore)
        //        val noEntryPoints = entitiesByProperty(NoEntryPoint)(propertyStore)

        val methodsCount: Double = project.methodsCount.toDouble
        def getPercentage(value: Int): String = "%1.2f" format (value.toDouble / methodsCount * 100d)

        val outputTable = s"\n\n#methods: ${project.methodsCount}\n"+
            s"#entry points: | $oldEntryPoints (old)     | ${entryPoints.size} (new)\n"+
            s"percentage:    | ${getPercentage(oldEntryPoints)}% (old)     | ${getPercentage(entryPoints.size)}% (new)\n"+
            s"analysisTime:  | $oldTime (old) | ${analysisTime} (new)"

        BasicReport(
            outputTable
        )
    }
}
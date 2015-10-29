/* BSD 2-Clause License:
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

import org.opalj.br.analyses.SomeProject
import org.opalj.log.OPALLogger

/**
 * @author Michael Reif
 */
class FPCFAnalysisExecuter private (
        project: SomeProject
) {

    // Accesses to this field have to be synchronized
    private[this] final val registeredAnalyses = scala.collection.mutable.Set.empty[Int]

    //    private[this] def alreadyRegistered(
    //        analysis: FPCFAnalysisRunner[_]
    //    ): Boolean = this.synchronized {
    //        registeredAnalyses contains analysis.uniqueId
    //    }

    private[this] def registerAnalysis(
        analysisRunner: FPCFAnalysisRunner[_]
    ): Unit = this.synchronized {
        assert(!(registeredAnalyses contains analysisRunner.uniqueId),
                "given fpcf analysis is already registered for this specific project")
        registeredAnalyses += analysisRunner.uniqueId
    }

    def run(
        analysisRunner: FPCFAnalysisRunner[_]
    ): Unit = this.synchronized {
        if (!(registeredAnalyses contains analysisRunner.uniqueId)) {
            registerAnalysis(analysisRunner)
            analysisRunner.doStart(project)
        } else
            OPALLogger.error(
                "internal",
                s"Given analysis(id: ${analysisRunner.uniqueId})is already registerd for this specific project"
            )(
                    project.logContext
                )
    }
}

/**
 * Companion object of FPCFAnalysisFactory.
 */
object FPCFAnalysisExecuter {

    def apply(project: SomeProject): FPCFAnalysisExecuter = {
        new FPCFAnalysisExecuter(project)
    }
}
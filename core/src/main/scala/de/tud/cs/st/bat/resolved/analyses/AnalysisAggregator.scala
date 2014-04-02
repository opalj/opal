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
package de.tud.cs.st
package bat
package resolved
package analyses

import java.net.URL

/**
 * Aggregates several analyses such that they are treated as one afterwards.
 *
 * ==Thread Safety==
 * This class is thread safe.
 *
 * ==Implementation Note==
 * If you extend this class, make sure that all access to this classes (mutable) fields/
 * mutable data structures is synchronized on `analyses`.
 *
 * @author Michael Eichberg
 */
class AnalysisAggregator[Source, AnalysisResult]
        extends Analysis[Source, Iterable[AnalysisResult]] {

    import scala.collection.mutable.Set

    protected[this] var analyses = List.empty[Analysis[Source, AnalysisResult]]

    protected[this] var analyzeInParallel = false

    def register(analysis: Analysis[Source, AnalysisResult]) {
        this.synchronized(analyses = analysis :: analyses)
    }

    def setAnalyzeInParallel(analyzeInParallel: Boolean) {
        this.synchronized(this.analyzeInParallel = analyzeInParallel)
    }

    def analyze(
        project: Project[Source],
        parameters: Seq[String]): Iterable[AnalysisResult] =
        this.synchronized {
            if (analyzeInParallel) {
                (
                    for (analysis ← analyses.par) yield {
                        analysis.analyze(project, parameters)
                    }
                ).seq
            } else {
                for (analysis ← analyses) yield { analysis.analyze(project, parameters) }
            }
        }

    override def title: String = "Analysis Collection"

    override def copyright: String =
        this.synchronized {
            "Copyrights of the analyses;\n"+
                analyses.map("\t"+_.copyright).mkString("\"")
        }

    def description: String =
        this.synchronized {
            "Executes the following analyses:\n"+analyses.map("\t"+_.title).mkString("\n")
        }

}

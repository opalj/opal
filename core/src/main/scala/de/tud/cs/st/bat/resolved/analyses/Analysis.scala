/* License (BSD Style License):
 * Copyright (c) 2009 - 2013
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
 *  - Neither the name of the Software Technology Group or Technische
 *    Universität Darmstadt nor the names of its contributors may be used to
 *    endorse or promote products derived from this software without specific
 *    prior written permission.
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

/**
 * Common trait that needs to be mixed in by analyses that want to use the general
 * analysis framework [[de.tud.cs.st.bat.resolved.analyses.AnalysisExecutor]].
 *
 * ==Conceptual Idea==
 * Each analysis can produce some result. E.g., a text describing a scenario that
 * leads to a bug, a graph, a report that identifies a specific line or a combination
 * thereof.
 *
 * However, an analysis should never rely on the location of a resource. If an analysis
 * needs access to further resource, it should use the `Project` class.
 *
 * @see [[de.tud.cs.st.bat.resolved.analyses.SingleOptionalResultAnalysis]]
 * @see [[de.tud.cs.st.bat.resolved.analyses.MultipleResultsAnalysis]]
 * @author Michael Eichberg
 */
trait Analysis[-Source, +AnalysisResult] {

    /**
     * Analyzes the given project and reports the result(s).
     */
    def analyze(project: Project[Source]): AnalysisResult

    /**
     * A textual description of this analysis.
     *
     * The description should discuss:
     *  * the goal of the analysis
     *  * weaknesses of the analysis; i.e., whether the analysis may report false
     *    positives or may not report existing bugs (i.e., whether the analysis is
     *    subject to false negatives.)
     *  * if applicable, it should discuss what the developer could/should do in general
     *    to remedy the situation
     *  * if applicable it should discuss the severeness of the found results. I.e.,
     *    if immediate action is typically required, because a bug was found that will
     *    show up at runtime or whether it is a security bug.
     */
    def description: String

    /**
     * The copyright statement less than 124 character with no line-breaks.
     */
    def copyright: String

    /**
     * A short descriptive title less than 64 characters with no line-breaks.
     */
    def title: String
}

trait SingleOptionalResultAnalysis[-Source, +AnalysisResult]
        extends Analysis[Source, Option[AnalysisResult]] {
}

trait MultipleResultsAnalysis[-Source, +AnalysisResult]
        extends Analysis[Source, Iterable[AnalysisResult]] {
}

import java.net.URL

/**
 * Aggregates several analyses such that they are treated as one afterwards.
 *
 * ==Thread Safety==
 * This class is thread safe.
 */
class AnalysisAggregator[Source, AnalysisResult]
        extends Analysis[Source, Iterable[AnalysisResult]] {

    import scala.collection.mutable.Set

    protected[this] val analyses = Set[Analysis[Source, AnalysisResult]]()

    protected[this] var analyzeInParallel = false

    def register(analysis: Analysis[Source, AnalysisResult]) {
        analyses.synchronized(analyses += analysis)
    }

    def setAnalyzeInParallel(analyzeInParallel: Boolean) {
        analyses.synchronized(this.analyzeInParallel = analyzeInParallel)
    }

    def analyze(project: Project[Source]): Iterable[AnalysisResult] =
        analyses.synchronized {
            if (analyzeInParallel) {
                (for (analysis ← analyses.par) yield { analysis.analyze(project) }).seq
            } else {
                for (analysis ← analyses) yield { analysis.analyze(project) }
            }
        }

    def title: String = "Analysis Collection"

    def description: String =
        analyses.synchronized {
            "Executes the following analyses:\n"+analyses.map("\t"+_.title).mkString("\n")
        }

    def copyright: String =
        analyses.synchronized {
            "Copyrights of the analyses;\n"+
                analyses.map("\t"+_.copyright).mkString("\"")

        }

}

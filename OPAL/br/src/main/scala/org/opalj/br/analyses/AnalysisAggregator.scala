/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package analyses

/**
 * Aggregates several analyses such that they are treated as one afterwards.
 *
 * ==Thread Safety==
 * This class is thread safe.
 *
 * ==Implementation Note==
 * If you extend this class, make sure that all access to this class' (mutable) fields/
 * mutable data structures is synchronized on `this`.
 *
 * @author Michael Eichberg
 */
class AnalysisAggregator[Source, AnalysisResult]
    extends Analysis[Source, Iterable[AnalysisResult]] {

    protected[this] var analyses = List.empty[Analysis[Source, AnalysisResult]]

    protected[this] var analyzeInParallel = true

    def register(analysis: Analysis[Source, AnalysisResult]): Unit = this.synchronized {
        analyses = analysis :: analyses
    }

    def setAnalyzeInParallel(analyzeInParallel: Boolean): Unit = this.synchronized {
        this.analyzeInParallel = analyzeInParallel
    }

    def analyze(
        project:                Project[Source],
        parameters:             Seq[String],
        initProgressManagement: (Int) ⇒ ProgressManagement
    ): Iterable[AnalysisResult] = this.synchronized {
        if (analyzeInParallel) {
            {
                for (analysis ← analyses.par) yield {
                    analysis.analyze(project, parameters, initProgressManagement)
                }
            }.seq
        } else {
            for (analysis ← analyses) yield {
                analysis.analyze(project, parameters, initProgressManagement)
            }
        }

    }

    override def title: String = "Analysis Collection"

    override def copyright: String = this.synchronized {
        "Copyrights of the analyses;\n"+analyses.map("\t"+_.copyright).mkString("\"")
    }

    override def description: String = this.synchronized {
        "Executes the following analyses:\n"+analyses.map("\t"+_.title).mkString("\n")
    }

}

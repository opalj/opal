/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package analyses

/**
 * @see [[org.opalj.br.analyses]] for several predefined converter functions.
 *
 * @tparam Source The type of the underlying source file.
 *
 * @author Michael Eichberg
 */
case class ReportableAnalysisAdapter[Source, AnalysisResult](
        analysis:  Analysis[Source, AnalysisResult],
        converter: AnalysisResult ⇒ String
) extends Analysis[Source, ReportableAnalysisResult] {

    override def description = analysis.description
    override def title = analysis.title
    override def copyright = analysis.copyright

    override def analyze(
        project:                Project[Source],
        parameters:             Seq[String],
        initProgressManagement: (Int) ⇒ ProgressManagement
    ): ReportableAnalysisResult = {
        new BasicReport(converter(analysis.analyze(project, parameters, initProgressManagement)))
    }
}

object ReportableAnalysisAdapter {

    def apply[Source](
        analysis: Analysis[Source, Iterable[ReportableAnalysisResult]]
    ): Analysis[Source, ReportableAnalysisResult] = {

        ReportableAnalysisAdapter(
            analysis,
            (results: Iterable[ReportableAnalysisResult]) ⇒
                results.map(_.toConsoleString).mkString("\n")
        )

    }
}
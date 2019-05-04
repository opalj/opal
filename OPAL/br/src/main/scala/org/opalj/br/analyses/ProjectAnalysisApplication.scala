/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package analyses

import scala.language.implicitConversions

import java.net.URL

/**
 * Default implementation of the [[AnalysisApplication]] trait which facilitates the
 * development of analyses which are executed in one step.
 *
 * @author Michael Eichberg
 */
abstract class ProjectAnalysisApplication
    extends AnalysisApplication
    with OneStepAnalysis[URL, ReportableAnalysisResult] {

    implicit def String2BasicReport(report: String): BasicReport = BasicReport(report)

    final override val analysis: ProjectAnalysisApplication = this

}

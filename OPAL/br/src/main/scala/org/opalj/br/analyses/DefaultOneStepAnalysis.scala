/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package analyses

import java.net.URL

/**
 * Default implementation of the [[AnalysisExecutor]] which facilitates the
 * development of analyses which are executed in one step.
 *
 * @author Michael Eichberg
 */
abstract class DefaultOneStepAnalysis
    extends AnalysisExecutor
    with OneStepAnalysis[URL, ReportableAnalysisResult] {

    final override val analysis: DefaultOneStepAnalysis = this

}

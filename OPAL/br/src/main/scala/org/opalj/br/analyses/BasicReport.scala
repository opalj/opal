/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package analyses

/**
 * Result of some analysis that just consists of some text.
 *
 * @param toConsoleString A string printed to the console.
 * @author Michael Eichberg
 */
case class BasicReport(toConsoleString: String) extends ReportableAnalysisResult

/**
 * Defines factory methods for BasicReports.
 */
object BasicReport {

    def apply(messages: Iterable[String]): BasicReport = BasicReport(messages.mkString("\n"))

}

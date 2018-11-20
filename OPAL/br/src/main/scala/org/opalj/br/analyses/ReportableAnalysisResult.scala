/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package analyses

/**
 * Result of analyses that can be meaningfully represented using text.
 *
 * @author Michael Eichberg
 */
trait ReportableAnalysisResult {

    /**
     * The results of the analysis in a form suitable for printing it to the
     * command line.
     *
     * If you are generating output related to (a line in) a class file, use
     * a format as used by other compilers, e.g., CLANG and GCC:
     * <pre>
     * FILENAME:[LINE:[COLUMN:]] TYPE: MESSAGE
     * </pre>
     * where FILENAME denotes the name of the file, LINE is the line number if available,
     * COLUMN is the column – which is usually not available when you analyze class files
     * and TYPE identifies the type of the message (e.g., "note", "warning", "error",
     * "fatal error").
     *
     * Line and column information is optional.
     *
     * If the real filename is not available, use the fully qualified name of the class
     * in binary notation (i.e., using "/" to separate the package qualifiers)
     * with the suffice ".class" appended.
     *
     * Note that the space after the location information is required.
     *
     * ==Example==
     * <pre>
     * demo/Buggy.class:100: warning: protected field in final class
     * </pre>
     */
    def toConsoleString: String
}

object ReportableAnalysisResult {

    def asReport(reports: Iterable[ReportableAnalysisResult]): ReportableAnalysisResult = {
        new ReportableAnalysisResult {
            def toConsoleString: String = reports.view.map(_.toConsoleString).mkString("\n")
        }
    }

}

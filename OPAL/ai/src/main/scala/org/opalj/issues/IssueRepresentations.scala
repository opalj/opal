/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package issues

import scala.xml.Node

import play.api.libs.json.JsValue

import org.opalj.br.analyses.ReportableAnalysisResult

/**
 * Definition of the representations that can be generated from a given issue (or parts thereof).
 *
 * @author Michael Eichberg
 */
trait IssueRepresentations extends ReportableAnalysisResult {

    /**
     * An (x)HTML5 representation of the issue, well suited for browser output.
     */
    def toXHTML(basicInfoOnly: Boolean): Node

    /**
     * A representation of this issue well suited for output to the Eclipse IDE console.
     */
    def toEclipseConsoleString: String

    /**
     * A representation of this issue using the Issues Description Language (which is a JSON
     * dialect.)
     */
    def toIDL: JsValue

    /**
     * Representation of this issue well suited for console output if the
     * console supports ANSI color escapes.
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
     * If the real filename is not available use the fully qualified name of the class
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
    def toAnsiColoredString: String

    /**
     * A basic representation of this issue well suited for console output.
     */
    def toConsoleString: String = {
        import scala.Console._
        toAnsiColoredString.
            replace(RED, "").
            replace(BLACK, "").
            replace(YELLOW, "").
            replace(BLUE, "").
            replace(BOLD, "").
            replace(RESET, "")
    }

}

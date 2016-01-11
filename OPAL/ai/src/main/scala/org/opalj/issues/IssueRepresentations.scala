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
package org.opalj
package issues

import scala.xml.Node
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
     * A representation of this issue using the Issues Description Language.
     */
    def toIDL: String

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


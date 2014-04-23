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
import java.io.File

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
     * If you are generating output related to (a line in) a class file use
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
     * in binary notation (i.e., using "/" to seperate the package qualifiers)
     * with the suffice ".class" appended.
     *
     * Note that the space after the location information is required.
     *
     * ==Example==
     * <pre>
     * demo/Buggy.class:100: warning: protected field in final class
     * </pre>
     */
    def consoleReport: String
}

object ReportableAnalysisResult {

    def asReport(reports: Iterable[ReportableAnalysisResult]) =
        new ReportableAnalysisResult {
            def consoleReport: String = reports.view.map(_.consoleReport).mkString("\n")
        }

}

/**
 * Result of some analysis that just consists of some text.
 */
case class BasicReport(message: String) extends ReportableAnalysisResult {
    def consoleReport = message
}

/**
 * Defines factory methods for BasicReports.
 */
object BasicReport {
    def apply(messages: Iterable[String]): BasicReport = {
        BasicReport(messages.mkString("\n"))
    }
}

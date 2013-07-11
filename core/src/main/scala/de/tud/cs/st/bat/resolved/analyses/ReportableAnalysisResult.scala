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

import reader.Java7Framework

import java.net.URL
import java.io.File

/**
 * Marker trait for analysis results that can be reported to the (end-)user.
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
 * Encapsulates a basic report of some issue related to a specific method or line of code.
 */
case class BasicReport(
    source: Option[String],
    line: Option[Int],
    column: Option[Int],
    messageType: String,
    message: String)
        extends ReportableAnalysisResult {

    def consoleReport(): String = {
        source.getOrElse("!<PROJECT_EXTERNAL>:") +
            line.map(_+":").getOrElse("") +
            column.map(_+": ").getOrElse(" ") +
            messageType+": "+
            message

    }
}

object BasicReport {

    implicit def URLtoSomeString(url: URL): Some[String] = {
        if (url.getProtocol() == "file") {
            Some(url.getFile)
        } else {
            Some(url.toExternalForm())
        }
    }

    def apply(source: String, messageType: String, message: String) {

        BasicReport(Some(source), None, None, messageType, message)
    }

    /**
     *
     */
    def apply(
        source: String,
        method: Option[Method],
        code: Code,
        pc: Int,
        messageType: String, message: String) {

        val location = code.lookupLineNumber(pc)
        if (location.isDefined)
            BasicReport(Some(source), location, None, messageType, message)
        else if (method.isDefined)
            BasicReport(Some(source+":!<"+method.get.toJava+">: "), None, None, messageType, message)
        else
            BasicReport(Some(source), None, None, messageType, message)
    }

}




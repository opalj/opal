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
    def consoleReport() = message
}

/**
 * Defines factory methods for BasicReports.
 */
object BasicReport {
    def apply(messages: Iterable[String]): BasicReport = {
        BasicReport(messages.mkString("\n"))
    }
}

/**
 * Each report takes a severity parameter, which can be one of these pre-defined objects:
 *  - [[Severity.Error]]
 *  - [[Severity.Warning]]
 *  - [[Severity.Info]]
 *
 * @author Daniel Klauer
 */
abstract class Severity {
    /**
     * Returns a string using text and ANSI color codes suitable for console output to
     * allow humans to quickly identify the corresponding severity.
     * 
     * @param suffix An additional suffix string that will be appended to the severity
     * text and will be colored in the same way. This is useful, for example, to append
     * ": " to achieve a formatting such as "<severity>: " where this whole string uses
     * the severity's color. This matches the colored formatting done by clang.
     * 
     * @return Human-readable string identifying this severity level, ready to be printed
     * to the console.
     */
    def toAnsiColoredString(suffix: String): String
}

/**
 * Object used as wrapper namespace for the individual severity objects, to avoid polluting the
 * global namespace.
 *
 * @author Daniel Klauer
 */
object Severity {
    /**
     * Should be used when reporting an issue that definitely is a bug.
     */
    case object Error extends Severity {
        def toAnsiColoredString(suffix: String): String =
            Console.BOLD + Console.RED+"error"+suffix+Console.RESET
    }

    /**
     * Should be used when reporting an issue that could potentially be a bug under
     * certain circumstances.
     */
    case object Warning extends Severity {
        def toAnsiColoredString(suffix: String): String =
            Console.BOLD + Console.YELLOW+"warning"+suffix+Console.RESET
    }

    /**
     * Should be used when reporting non-serious information. This refers to issues that
     * neither are bugs nor could lead to bugs, but may still be worth fixing (for example
     * unused fields).
     */
    case object Info extends Severity {
        def toAnsiColoredString(suffix: String): String =
            Console.BOLD + Console.BLUE+"info"+suffix+Console.RESET
    }
}

/**
 * Common superclass of all reporters that depend on the source file's of a specific
 * class file.
 */
abstract class SourceLocationBasedReport[+S] {

    def source: Option[S]

    /**
     * Retrieves the `source` as a human-readable string for use in console reports.
     * Every `SourceLocationBasedReport` should use this as a prefix in its console report string.
     */
    protected def getSourceAsString(locationIdentifier: (S) ⇒ String): String =
        source.map(locationIdentifier(_)).getOrElse("<external>")

    def consoleReport(locationIdentifier: (S) ⇒ String): String
}

/**
 * A report related to a specific class.
 */
case class ClassBasedReport[+S](
    source: Option[S],
    severity: Severity,
    classType: ObjectType,
    message: String)
        extends SourceLocationBasedReport[S] {

    def consoleReport(locationIdentifier: (S) ⇒ String): String = {
        getSourceAsString(locationIdentifier)+": "+
            severity.toAnsiColoredString(": ")+
            "class "+classType.toJava+": "+
            message
    }
}

/**
 * A report related to a specific method.
 */
case class MethodBasedReport[+S](
    source: Option[S],
    severity: Severity,
    methodDescriptor: MethodDescriptor,
    methodName: String,
    message: String)
        extends SourceLocationBasedReport[S] {

    def consoleReport(locationIdentifier: (S) ⇒ String): String = {
        getSourceAsString(locationIdentifier)+": "+
            severity.toAnsiColoredString(": ")+
            "method "+methodName+": "+
            message
    }
}

/**
 * Defines factory methods for MethodBasedReports.
 */
object MethodBasedReport {
    def apply[S](
        source: Option[S],
        severity: Severity,
        method: Method,
        message: String): MethodBasedReport[S] = {
        new MethodBasedReport(source, severity, method.descriptor, method.name, message)
    }
}

/**
 * A report related to a specific field.
 */
case class FieldBasedReport[+S](
    source: Option[S],
    severity: Severity,
    declaringClass: ObjectType,
    fieldType: Option[Type],
    fieldName: String,
    message: String)
        extends SourceLocationBasedReport[S] {

    def consoleReport(locationIdentifier: (S) ⇒ String): String = {
        getSourceAsString(locationIdentifier)+": "+
            severity.toAnsiColoredString(": ")+
            "field "+declaringClass.fqn+"."+fieldName+": "+
            message
    }
}

/**
 * Defines factory methods for FieldBasedReports.
 */
object FieldBasedReport {
    def apply[S](
        source: Option[S],
        severity: Severity,
        declaringClass: ObjectType,
        field: Field,
        message: String): FieldBasedReport[S] = {
        new FieldBasedReport(source, severity, declaringClass,
            Some(field.fieldType), field.name, message)
    }
}

/**
 * A report related to a specific line and column.
 */
case class LineAndColumnBasedReport[+S](
    source: Option[S],
    severity: Severity,
    line: Option[Int],
    column: Option[Int],
    message: String)
        extends SourceLocationBasedReport[S] {

    def consoleReport(locationIdentifier: (S) ⇒ String): String = {
        getSourceAsString(locationIdentifier)+":"+
            line.map(_+":").getOrElse("") +
            column.map(_+": ").getOrElse(" ") +
            severity.toAnsiColoredString(": ") +
            message
    }
}

/**
 * @see [[de.tud.cs.st.bat.resolved.analyses]] for several predefined converter functions.
 */
case class ReportableAnalysisAdapter[Source, AnalysisResult](
    analysis: Analysis[Source, AnalysisResult],
    converter: AnalysisResult ⇒ String)
        extends Analysis[Source, ReportableAnalysisResult] {

    def description = analysis.description
    override def title = analysis.title
    override def copyright = analysis.copyright
    def analyze(
        project: Project[Source],
        parameters: Seq[String]): ReportableAnalysisResult = {
        new BasicReport(converter(analysis.analyze(project, parameters)))
    }
}

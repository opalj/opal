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

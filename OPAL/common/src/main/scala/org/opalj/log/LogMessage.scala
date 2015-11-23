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
package log

/**
 * Description of a log message.
 *
 * ==Implementation Guidelines==
 * A LogMessage should never contain a direct reference to a [[LogContext]] object.
 *
 * @author Michael Eichberg
 */
trait LogMessage {

    /**
     * The log level.
     */
    def level: Level

    /**
     * The category to which this method belongs. E.g. "project configuration" to
     * signal that the message is related to the project configuration and it is – hence -
     * an issue that probably needs to be fixed by the developer. Another category
     * might be "internal (error)" to signal that an error occurred that might need
     * to be fixed by the developer of the respective analysis.
     */
    def category: Option[String]

    /**
     * The log message. An unformatted string that may contain line breaks
     * and tabs.
     */
    def message: String

    private def categoryToConsoleOutput: String =
        category.map(c ⇒ s"[$c]").getOrElse("")

    /**
     * Creates a string representation of the log message that is well-suited for
     * console output.
     */
    def toConsoleOutput(ansiColored: Boolean): String = {
        val (lnStart, lnEnd) =
            if (ansiColored) {
                (
                    s"${level.ansiColorEscape}[${level.id}]${categoryToConsoleOutput} ",
                    Console.RESET
                )
            } else {
                (s"[${level.id}]${categoryToConsoleOutput} ",
                    "")
            }

        message.split('\n').map(ln ⇒ lnStart + ln + lnEnd).mkString("\n")
    }
}

/**
 * Default implementation of a log message.
 *
 * @author Michael Eichberg
 */
case class BasicLogMessage(
    level:   Level  = Info,
    message: String
)
        extends LogMessage {

    def category = None
}

/**
 * Default implementation of a log message.
 *
 * @author Michael Eichberg
 */
case class StandardLogMessage(
    level:    Level          = Info,
    category: Option[String] = None,
    message:  String
)
        extends LogMessage

case class ExceptionLogMessage(
    level:       Level          = Info,
    category:    Option[String] = None,
    baseMessage: String,
    t:           Throwable
)
        extends LogMessage {

    def message = {

        def exceptionToMessage(t: Throwable): String = {
            val stacktrace = t.getStackTrace.mkString("\t", "\n\t", "")
            val message = t.getClass.toString()+": "+t.getLocalizedMessage ++ "\n"+stacktrace
            if (t.getCause != null)
                message+"\n"+exceptionToMessage(t.getCause)
            else
                message
        }
        baseMessage+"\n"+exceptionToMessage(t)
    }
}

/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2017
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
 * Common super trait of all log levels.
 *
 * @author Michael Eichberg
 */
sealed abstract class Level {

    def apply(info: String): LogMessage

    def apply(category: String, info: String): LogMessage

    def ansiColorEscape: String

    def id: String

    def value: Int
}

/**
 * Factory for info level log messages.
 *
 * @see [[OPALLogger$]] for usage instructions.
 */
case object Info extends Level {

    def apply(info: String): LogMessage = BasicLogMessage(message = info)

    def apply(category: String, info: String): LogMessage = {
        new StandardLogMessage(Info, Some(category), info)
    }

    def ansiColorEscape: String = ""

    def id: String = "info"

    def value: Int = 0
}
/**
 * Factory for warn level log messages.
 *
 * @see [[OPALLogger$]] for usage instructions.
 */
case object Warn extends Level {

    def apply(info: String): LogMessage = BasicLogMessage(Warn, info)

    def apply(category: String, info: String): LogMessage = {
        new StandardLogMessage(Warn, Some(category), info)
    }

    def ansiColorEscape: String = Console.BLUE

    def id: String = "warn"

    def value: Int = 1000
}

/**
 * Factory for error level log messages.
 *
 * @see [[OPALLogger$]] for usage instructions.
 */
case object Error extends Level {

    def apply(info: String): LogMessage = BasicLogMessage(Error, info)

    def apply(category: String, info: String): LogMessage = {
        new StandardLogMessage(Error, Some(category), info)
    }

    def apply(category: String, info: String, t: Throwable): LogMessage = {
        try {
            new ExceptionLogMessage(Error, Some(category), info, t)
        } catch {
            case it: Throwable ⇒
                Console.err.println(
                    s"[fatal][OPAL] logging [error][$category] $info: ${t.getMessage} failed:"
                )
                it.printStackTrace(Console.err)
                BasicLogMessage(Fatal, it.getMessage)
        }
    }

    def ansiColorEscape: String = Console.RED

    def id: String = "error"

    def value: Int = Int.MaxValue - 1000
}

case object Fatal extends Level {

    def apply(info: String): LogMessage = new BasicLogMessage(Fatal, info)

    def apply(category: String, info: String): LogMessage = {
        new StandardLogMessage(Fatal, Some(category), info)
    }

    def ansiColorEscape: String = Console.RED + Console.YELLOW_B

    def id: String = "fatal"

    def value: Int = Int.MaxValue
}

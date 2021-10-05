/* BSD 2-Clause License - see OPAL/LICENSE for details. */
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

    final val value: Int = 0
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

    final val value: Int = 1000
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
            case it: Throwable =>
                Console.err.println(
                    s"[fatal][OPAL] logging [error][$category] $info: ${t.getMessage} failed:"
                )
                it.printStackTrace(Console.err)
                BasicLogMessage(Fatal, it.getMessage)
        }
    }

    def ansiColorEscape: String = Console.RED

    def id: String = "error"

    final val value: Int = Int.MaxValue - 1000
}

case object Fatal extends Level {

    def apply(info: String): LogMessage = new BasicLogMessage(Fatal, info)

    def apply(category: String, info: String): LogMessage = {
        new StandardLogMessage(Fatal, Some(category), info)
    }

    def ansiColorEscape: String = Console.RED + Console.YELLOW_B

    def id: String = "fatal"

    final val value: Int = Int.MaxValue
}

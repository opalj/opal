/* BSD 2-Clause License - see OPAL/LICENSE for details. */
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
     * The category to which this method belongs. E.g., "project configuration" to
     * signal that the message is related to the project configuration and it is – hence -
     * an issue that probably needs to be fixed by the developer. Another category
     * might be "internal (error)" to signal that an error occurred that might need
     * to be fixed by the developer of the respective analysis.
     */
    def category: Option[String]

    /**
     * The log message. An unformatted string that may contain line breaks and tabs.
     *
     * If the message starts with "\r", the "\r" is moved to the beginning.
     */
    def message: String

    private def categoryToConsoleOutput: String = category.map(c => s"[$c]").getOrElse("")

    /**
     * Creates a string representation of the log message that is well-suited for
     * console output.
     */
    def toConsoleOutput(ansiColored: Boolean): String = {
        val (lnStart, lnEnd) =
            if (ansiColored) {
                (s"${level.ansiColorEscape}[${level.id}]$categoryToConsoleOutput ", Console.RESET)
            } else {
                (s"[${level.id}]$categoryToConsoleOutput ", "")
            }

        message.split('\n').map { ln =>
            var cr = ""
            var rawln = ln
            if (ln.length > 0 && ln.charAt(0) == '\r') {
                cr = "\r"
                rawln = ln.substring(1)
            }
            cr + lnStart + rawln + lnEnd
        }.mkString("\n")
    }
}

object LogMessage {

    def plainInfo(info: String): LogMessage = new LogMessage {
        override def level: Level = Info
        override def category: Option[String] = None
        override def message: String = info
        override def toConsoleOutput(ansiColored: Boolean): String = {
            info
        }
    }

}

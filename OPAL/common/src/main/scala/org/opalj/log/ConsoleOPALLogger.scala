/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package log

/**
 * The console logger is a very basic logger that ignores the context.
 *
 * @author Michael Eichberg
 */
class ConsoleOPALLogger(val ansiColored: Boolean, val minLogLevel: Int) extends OPALLogger {

    def this(ansiColored: Boolean = true, minLogLevel: Level = Info) =
        this(ansiColored, minLogLevel.value)

    def log(message: LogMessage)(implicit ctx: LogContext): Unit = {
        val messageLevel = message.level
        if (messageLevel.value < minLogLevel)
            return ;

        val stream = if (messageLevel.value >= Error.value) Console.err else Console.out
        val theMessage = message.toConsoleOutput(ansiColored)
        if (theMessage.size > 0 && theMessage.charAt(0) == '\r')
            stream.print(theMessage)
        else
            stream.println(theMessage)
    }

}

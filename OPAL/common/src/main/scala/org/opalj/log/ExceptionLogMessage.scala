/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package log

case class ExceptionLogMessage(
        level:       Level          = Info,
        category:    Option[String] = None,
        baseMessage: String,
        t:           Throwable
) extends LogMessage {

    def message: String = {
        def exceptionToMessage(prefix: String, t: Throwable): String = {
            var message = prefix+"\n"
            val stacktrace = t.getStackTrace.mkString("\t", "\n\t", "")
            message += t.getClass.toString+": "+t.getLocalizedMessage ++ "\n"+stacktrace
            if (t.getCause != null) {
                message = exceptionToMessage(message+"\n", t.getCause)
            }
            if (t.getSuppressed != null) {
                t.getSuppressed foreach { t =>
                    message = exceptionToMessage(message, t)
                }
            }
            message
        }
        exceptionToMessage(baseMessage, t)
    }
}

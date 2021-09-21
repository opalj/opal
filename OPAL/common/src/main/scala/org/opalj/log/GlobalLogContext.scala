/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package log

/**
 * The global log context which should be used to log global messages.
 *
 * This context is automatically registered with the OPALLogger framework and uses, by default,
 * a [[ConsoleOPALLogger]].
 *
 * @author Michael Eichberg
 */
case object GlobalLogContext extends LogContext {

    OPALLogger.register(this, new ConsoleOPALLogger(ansiColored = true))

    def newInstance: LogContext = throw new UnsupportedOperationException

    override def successor: LogContext = this

}

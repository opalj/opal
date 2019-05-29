/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package log

/**
 * An instance of [[OPALLogger]] that does not perform _any logging_ .
 *
 * In general, it is HIGHLY recommended to use the `ConsoleOPALLogger` and
 * to the set the minimium log level to `Error`.
 *
 * @author Florian Kübler
 */
object DevNullLogger extends OPALLogger {

    override def log(message: LogMessage)(implicit ctx: LogContext): Unit = {}

}

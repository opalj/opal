/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package log

/**
  * An instance of [[OPALLogger]] that does not perform any logging.
  */
object DevNullLogger extends OPALLogger {
    override def log(message: LogMessage)(implicit ctx: LogContext): Unit = {}
}

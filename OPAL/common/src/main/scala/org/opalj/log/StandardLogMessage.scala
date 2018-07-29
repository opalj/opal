/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package log

/**
 * Default implementation of a log message.
 *
 * @author Michael Eichberg
 */
case class StandardLogMessage(
        level:    Level          = Info,
        category: Option[String] = None,
        message:  String
) extends LogMessage

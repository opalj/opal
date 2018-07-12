/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package log

/**
 * Default implementation of a log message.
 *
 * @author Michael Eichberg
 */
case class BasicLogMessage(level: Level = Info, message: String) extends LogMessage {

    def category: Option[String] = None

}

/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package log

case class StandardLogContext private ( final val startTime: Long) extends LogContext {

    def this() = this(startTime = System.currentTimeMillis())

    override def toString: String = s"LogContext(${startTime.toString().drop(6)})"

    override def newInstance: LogContext = new StandardLogContext(this.startTime)

}

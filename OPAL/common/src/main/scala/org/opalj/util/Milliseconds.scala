/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package util

import play.api.libs.json.JsNumber
import play.api.libs.json.JsPath
import play.api.libs.json.Reads
import play.api.libs.json.Writes

/**
 * Represents a time span of `n` milliseconds.
 *
 * @author Michael Eichberg
 */
class Milliseconds(val timeSpan: Long) extends AnyVal with Serializable {

    final def +(other: Milliseconds): Milliseconds = {
        new Milliseconds(this.timeSpan + other.timeSpan)
    }

    final def -(other: Milliseconds): Milliseconds = {
        new Milliseconds(this.timeSpan - other.timeSpan)
    }

    /**
     * Converts the specified number of milliseconds into seconds.
     */
    final def toSeconds: Seconds = new Seconds(timeSpan.toDouble / 1000.0d)

    final def toNanoseconds: Nanoseconds = new Nanoseconds(timeSpan * 1000L * 1000L)

    def toString(withUnit: Boolean): String = {
        if (withUnit) {
            s"$timeSpan ms"
        } else {
            timeSpan.toString
        }
    }

    override def toString: String = toString(withUnit = true)

}

/**
 * Defines factory methods and constants related to time spans in [[Milliseconds]].
 *
 * @author Michael Eichberg
 */
object Milliseconds {

    implicit val millisecondsWrites = new Writes[Milliseconds] {
        def writes(millisecond: Milliseconds) = JsNumber(millisecond.timeSpan)
    }

    implicit val nanosecondsReads: Reads[Milliseconds] = JsPath.read[Long].map(Milliseconds.apply)

    final val None: Milliseconds = new Milliseconds(0L)

    def apply(timeSpan: Long): Milliseconds = new Milliseconds(timeSpan)

    /**
     * Converts the specified time span and converts it into milliseconds.
     */
    final def TimeSpan(
        startTimeInMilliseconds: Long,
        endTimeInMilliseconds:   Long
    ): Milliseconds = {
        new Milliseconds(startTimeInMilliseconds - endTimeInMilliseconds)
    }

}

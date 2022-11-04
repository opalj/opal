/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package util

import play.api.libs.json.JsNumber
import play.api.libs.json.JsPath
import play.api.libs.json.Reads
import play.api.libs.json.Writes

/**
 * Represents a time span of `n` nanoseconds.
 *
 * @author Michael Eichberg
 */
class Nanoseconds(val timeSpan: Long) extends AnyVal with Serializable {

    final def +(other: Nanoseconds): Nanoseconds = {
        new Nanoseconds(this.timeSpan + other.timeSpan)
    }

    final def -(other: Nanoseconds): Nanoseconds = {
        new Nanoseconds(this.timeSpan - other.timeSpan)
    }

    /**
     * Conversion to [[Seconds]].
     */
    final def toSeconds: Seconds = {
        new Seconds(timeSpan.toDouble / 1000.0d / 1000.0d / 1000.0d)
    }

    /**
     * Conversion to [[Milliseconds]].
     */
    final def toMilliseconds: Milliseconds = {
        new Milliseconds(timeSpan / (1000 * 1000))
    }

    def toString(withUnit: Boolean): String = {
        if (withUnit) s"$timeSpan ns" else timeSpan.toString
    }

    override def toString: String = toString(withUnit = true)
}
/**
 * Defines factory methods and constants related to time spans in [[Nanoseconds]].
 *
 * @author Michael Eichberg
 */
object Nanoseconds {
    implicit val nanosecondsWrites = new Writes[Nanoseconds] {
        def writes(nanosecond: Nanoseconds) = JsNumber(nanosecond.timeSpan)
    }

    implicit val nanosecondsReads: Reads[Nanoseconds] = JsPath.read[Long].map(Nanoseconds.apply)

    final val None: Nanoseconds = new Nanoseconds(0L)

    def apply(timeSpan: Long): Nanoseconds = new Nanoseconds(timeSpan)

    /**
     * Converts the specified time span and converts it into seconds.
     */
    final def TimeSpan(
        startTimeInNanoseconds: Long,
        endTimeInNanoseconds:   Long
    ): Nanoseconds = {
        new Nanoseconds(endTimeInNanoseconds - startTimeInNanoseconds)
    }

}

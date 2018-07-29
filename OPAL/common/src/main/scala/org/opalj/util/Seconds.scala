/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package util

import play.api.libs.json.JsNumber
import play.api.libs.json.JsPath
import play.api.libs.json.Reads
import play.api.libs.json.Writes

/**
 * Represents a time span of `n` seconds.
 *
 * @author Michael Eichberg
 */
class Seconds(val timeSpan: Double) extends AnyVal with Serializable {

    def toString(withUnit: Boolean): String = {
        val time = f"$timeSpan%.4f"
        if (withUnit) {
            time+" s"
        } else {
            time
        }
    }

    def +(other: Seconds): Seconds = new Seconds(this.timeSpan + other.timeSpan)

    final def toNanoseconds: Nanoseconds = {
        new Nanoseconds((timeSpan * 1000.0d * 1000.0d * 1000.0d).toLong)
    }

    /**
     * Conversion to [[Milliseconds]].
     */
    final def toMilliseconds: Milliseconds = {
        new Milliseconds((timeSpan * 1000).toLong)
    }

    override def toString: String = toString(withUnit = true)

}
/**
 * Common constants related to seconds.
 *
 * @author Michael Eichberg
 */
object Seconds {

    implicit val secondsWrites = new Writes[Seconds] {
        def writes(second: Seconds) = JsNumber(second.timeSpan)
    }

    implicit val secondsReads: Reads[Seconds] = JsPath.read[Double].map(Seconds.apply)

    def apply(timeSpan: Double): Seconds = new Seconds(timeSpan)

    final val None: Seconds = new Seconds(0d)

}

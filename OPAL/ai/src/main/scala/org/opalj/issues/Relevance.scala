/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package issues

import scala.Console.YELLOW
import scala.Console.RESET
import scala.Console.RED

import play.api.libs.json.Json
import play.api.libs.json.JsValue

/**
 * Describes the overall relevance of a finding.
 *
 * When calculating the relevance of a finding you should take all
 * properties of the associated issue into consideration:
 *  - kind of issue
 *  - category of issue
 *  - accuracy of the analysis
 *
 * @param value A value between 0 (undetermined), 1 (not relevant) and 100 (absolutely relevant).
 *
 * @author Michael Eichberg
 */
final case class Relevance(value: Int) extends AnyVal {

    def merge(other: Relevance): Relevance = new Relevance(Math.max(this.value, other.value))

    def name: String = Relevance.toCategoryName(this)

    def toEclipseConsoleString: String = s"[$name]"

    /**
     * The lower the value, the "whiter" the color. If the value is 100
     * then the color will be black.
     */
    def toHTMLColor = {
        if (value >= 80)
            "rgb(135, 4, 10)"
        else if (value >= 40)
            "rgb(202, 136, 4)"
        else {
            val rgbValue = (0 + (100 - value) * 1.3).toInt
            s"rgb($rgbValue,$rgbValue,$rgbValue)"
        }
    }

    def toAnsiColoredString: String = {
        if (value > 65)
            RED + name + RESET
        else if (value > 32)
            YELLOW + name + RESET
        else
            Relevance.toCategoryName(this)
    }

    def toIDL: JsValue = Json.obj("name" -> name, "value" -> value)

}

/**
 * Collection of pre-configured relevance levels.
 */
object Relevance {

    final val OfUtmostRelevance = Relevance(99)

    final val VeryHigh = Relevance(80)

    final val High = Relevance(75)

    final val Moderate = Relevance(50)
    final val DefaultRelevance = Moderate

    final val UselessDefensiveProgramming = Relevance(40)

    final val Low = Relevance(30)

    final val VeryLow = Relevance(10)

    /**
     * A finding related to a common programming idiom.
     *
     * For example, a finding that always produces '''dead
     * code/suspicious code''' from the point of view of a static analysis.
     * E.e., a dead default branch in an exception that always just throws an exception.
     */
    final val CommonIdiom = Relevance(3)

    /**
     *  An assertion was proven to always hold.
     */
    final val ProvenAssertion = Relevance(2)

    final val OfNoRelevance = Relevance(1)

    /**
     *  A finding that is most likely not related to the source code.
     */
    final val TechnicalArtifact = OfNoRelevance

    final val Undetermined = Relevance(0)

    def toCategoryName(relevance: Relevance): String = {
        val r = relevance.value
        if (r >= OfUtmostRelevance.value) "extreme"
        else if (r >= VeryHigh.value) "very high"
        else if (r >= High.value) "high"
        else if (r >= Moderate.value) "moderate"
        else if (r >= Low.value) "low"
        else if (r >= VeryLow.value) "very low"
        else if (r >= CommonIdiom.value) "irrelevant [common programming idiom]"
        else if (r >= ProvenAssertion.value) "irrelevant [proven assertion]"
        else if (r >= OfNoRelevance.value) "irrelevant [technical/compile time artifact]"
        else "undetermined"

    }
}

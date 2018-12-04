/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.string_definition.properties

/**
 * @param possibleStrings Only relevant for some [[StringConstancyType]]s, i.e., sometimes this
 *                        parameter can be omitted.
 * @author Patrick Mell
 */
case class StringConstancyInformation(
        constancyLevel:  StringConstancyLevel.Value,
        constancyType:   StringConstancyType.Value,
        possibleStrings: String                     = ""
)

/**
 * Provides a collection of instance-independent but string-constancy related values.
 */
object StringConstancyInformation {

    /**
     * This string stores the value that is to be used when a string is dynamic, i.e., can have
     * arbitrary values.
     */
    val UnknownWordSymbol: String = "\\w"

    /**
     * The stringified version of a (dynamic) integer value.
     */
    val IntValue: String = "[AnIntegerValue]"

    /**
     * The stringified version of a (dynamic) float value.
     */
    val FloatValue: String = "[AFloatValue]"

    /**
     * A value to be used when the number of an element, that is repeated, is unknown.
     */
    val InfiniteRepetitionSymbol: String = "*"

}

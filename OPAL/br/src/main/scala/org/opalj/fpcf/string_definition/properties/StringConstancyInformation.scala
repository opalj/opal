/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.string_definition.properties

/**
 *
 * @author Patrick Mell
 */
case class StringConstancyInformation(
    constancyLevel: StringConstancyLevel.Value, possibleStrings: String
)

object StringConstancyInformation {

    /**
     * This string stores the value that is to be used when a string is dynamic, i.e., can have
     * arbitrary values.
     */
    val UnknownWordSymbol: String = "\\w"

    /**
     * A value to be used when the number of an element, that is repeated, is unknown.
     */
    val InfiniteRepetitionSymbol: String = "*"

}

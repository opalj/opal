/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.string_definition.properties

/**
 *
 * @author Patrick Mell
 */
class StringConstancyInformation(
    val constancyLevel: StringConstancyLevel.Value, val possibleStrings: String
)

object StringConstancyInformation {
    def apply(
        constancyLevel: StringConstancyLevel.Value, possibleStrings: String
    ): StringConstancyInformation = new StringConstancyInformation(constancyLevel, possibleStrings)
}

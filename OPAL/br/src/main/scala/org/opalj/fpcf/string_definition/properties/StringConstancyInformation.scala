/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.string_definition.properties

/**
 *
 * @author Patrick Mell
 */
case class StringConstancyInformation(
    constancyLevel: StringConstancyLevel.Value, possibleStrings: String
)

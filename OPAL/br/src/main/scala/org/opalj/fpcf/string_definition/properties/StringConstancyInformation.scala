/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.string_definition.properties

import org.opalj.fpcf.properties.StringConstancyProperty

/**
 * @param possibleStrings Only relevant for some [[StringConstancyType]]s, i.e., sometimes this
 *                        parameter can be omitted.
 * @author Patrick Mell
 */
case class StringConstancyInformation(
    constancyLevel:  StringConstancyLevel.Value = StringConstancyLevel.DYNAMIC,
    constancyType:   StringConstancyType.Value  = StringConstancyType.APPEND,
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

    /**
     * Takes a list of [[StringConstancyInformation]] and reduces them to a single one by or-ing
     * them together (the level is determined by finding the most general level; the type is set to
     * [[StringConstancyType.APPEND]] and the possible strings are concatenated using a pipe and
     * then enclosed by brackets.
     *
     * @param scis The information to reduce. If a list with one element is passed, this element is
     *             returned (without being modified in any way); a list with > 1 element is reduced
     *             as described above; the empty list will throw an error!
     * @return Returns the reduced information in the fashion described above.
     */
    def reduceMultiple(scis: List[StringConstancyInformation]): StringConstancyInformation = {
        scis.length match {
            // The list may be empty, e.g., if the UVar passed to the analysis, refers to a
            // VirtualFunctionCall (they are not interpreted => an empty list is returned) => return
            // the corresponding information
            case 0 ⇒ StringConstancyProperty.lowerBound.stringConstancyInformation
            case 1 ⇒ scis.head
            case _ ⇒ // Reduce
                val reduced = scis.reduceLeft((o, n) ⇒ StringConstancyInformation(
                    StringConstancyLevel.determineMoreGeneral(o.constancyLevel, n.constancyLevel),
                    StringConstancyType.APPEND,
                    s"${o.possibleStrings}|${n.possibleStrings}"
                ))
                // Modify possibleStrings value
                StringConstancyInformation(
                    reduced.constancyLevel, reduced.constancyType, s"(${reduced.possibleStrings})"
                )
        }
    }

}

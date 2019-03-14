/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.br.fpcf.properties.string_definition

/**
 * @param possibleStrings Only relevant for some [[StringConstancyType]]s, i.e., sometimes this
 *                        parameter can be omitted.
 *
 * @author Patrick Mell
 */
case class StringConstancyInformation(
        constancyLevel:  StringConstancyLevel.Value = StringConstancyLevel.DYNAMIC,
        constancyType:   StringConstancyType.Value  = StringConstancyType.APPEND,
        possibleStrings: String                     = ""
) {

    /**
     * Checks whether the instance is the neutral element.
     *
     * @return Returns `true` iff [[constancyLevel]] equals [[StringConstancyLevel.CONSTANT]],
     *         [[constancyType]] equals [[StringConstancyType.APPEND]], and
     *         [[possibleStrings]] equals the empty string.
     */
    def isTheNeutralElement: Boolean =
        constancyLevel == StringConstancyLevel.CONSTANT &&
            constancyType == StringConstancyType.APPEND &&
            possibleStrings == ""

}

/**
 * Provides a collection of instance-independent but string-constancy related values.
 */
object StringConstancyInformation {

    /**
     * This string stores the value that is to be used when a string is dynamic, i.e., can have
     * arbitrary values.
     */
    val UnknownWordSymbol: String = ".*"

    /**
     * The stringified version of a (dynamic) integer value.
     */
    val IntValue: String = "^-?\\d+$"

    /**
     * The stringified version of a (dynamic) float value.
     */
    val FloatValue: String = "^-?\\d*\\.{0,1}\\d+$"

    /**
     * A value to be used when the number of an element, that is repeated, is unknown.
     */
    val InfiniteRepetitionSymbol: String = "*"

    /**
     * A value to be used to indicate that a string expression might be null.
     */
    val NullStringValue: String = "^null$"

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
    def reduceMultiple(scis: Iterable[StringConstancyInformation]): StringConstancyInformation = {
        val relScis = scis.filter(!_.isTheNeutralElement)
        relScis.size match {
            // The list may be empty, e.g., if the UVar passed to the analysis, refers to a
            // VirtualFunctionCall (they are not interpreted => an empty list is returned) => return
            // the neutral element
            case 0 ⇒ StringConstancyInformation.getNeutralElement
            case 1 ⇒ relScis.head
            case _ ⇒ // Reduce
                val reduced = relScis.reduceLeft((o, n) ⇒
                    StringConstancyInformation(
                        StringConstancyLevel.determineMoreGeneral(
                            o.constancyLevel, n.constancyLevel
                        ),
                        StringConstancyType.APPEND,
                        s"${o.possibleStrings}|${n.possibleStrings}"
                    ))
                // Add parentheses to possibleStrings value (to indicate a choice)
                StringConstancyInformation(
                    reduced.constancyLevel, reduced.constancyType, s"(${reduced.possibleStrings})"
                )
        }
    }

    /**
     * @return Returns a [[StringConstancyInformation]] element that corresponds to the lower bound
     *         from a lattice-based point of view.
     */
    def lb: StringConstancyInformation = StringConstancyInformation(
        StringConstancyLevel.DYNAMIC,
        StringConstancyType.APPEND,
        StringConstancyInformation.UnknownWordSymbol
    )

    /**
     * @return Returns the / a neutral [[StringConstancyInformation]] element, i.e., an element for
     *         which [[StringConstancyInformation.isTheNeutralElement]] is `true`.
     */
    def getNeutralElement: StringConstancyInformation =
        StringConstancyInformation(StringConstancyLevel.CONSTANT)

    /**
     * @return Returns a [[StringConstancyInformation]] element to indicate a `null` value.
     */
    def getNullElement: StringConstancyInformation =
        StringConstancyInformation(StringConstancyLevel.CONSTANT, possibleStrings = NullStringValue)

}

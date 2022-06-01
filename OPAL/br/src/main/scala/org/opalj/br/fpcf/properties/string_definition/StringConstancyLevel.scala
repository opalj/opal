/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.br.fpcf.properties.string_definition

/**
 * Values in this enumeration represent the granularity of used strings.
 *
 * @author Patrick Mell
 */
object StringConstancyLevel extends Enumeration {

    type StringConstancyLevel = StringConstancyLevel.Value

    /**
     * This level indicates that a string has a constant value at a given read operation.
     */
    final val CONSTANT = Value("constant")

    /**
     * This level indicates that a string is partially constant (constant + dynamic part) at some
     * read operation, that is, the initial value of a string variable needs to be preserved. For
     * instance, it is fine if a string variable is modified after its initialization by
     * appending another string, s2. Later, s2 might be removed partially or entirely without
     * violating the constraints of this level.
     */
    final val PARTIALLY_CONSTANT = Value("partially_constant")

    /**
     * This level indicates that a string at some read operations has an unpredictable value.
     */
    final val DYNAMIC = Value("dynamic")

    /**
     * Returns the more general StringConstancyLevel of the two given levels. DYNAMIC is more
     * general than PARTIALLY_CONSTANT which is more general than CONSTANT.
     *
     * @param level1 The first level.
     * @param level2 The second level.
     * @return Returns the more general level of both given inputs.
     */
    def determineMoreGeneral(
        level1: StringConstancyLevel, level2: StringConstancyLevel
    ): StringConstancyLevel = {
        if (level1 == DYNAMIC || level2 == DYNAMIC) {
            DYNAMIC
        } else if (level1 == PARTIALLY_CONSTANT || level2 == PARTIALLY_CONSTANT) {
            PARTIALLY_CONSTANT
        } else {
            CONSTANT
        }
    }

    /**
     * Returns the StringConstancyLevel of a concatenation of two values.
     * CONSTANT + CONSTANT = CONSTANT
     * DYNAMIC + DYNAMIC = DYNAMIC
     * CONSTANT + DYNAMIC = PARTIALLY_CONSTANT
     * PARTIALLY_CONSTANT + {DYNAMIC, CONSTANT} = PARTIALLY_CONSTANT
     *
     * @param level1 The first level.
     * @param level2 The second level.
     * @return Returns the level for a concatenation.
     */
    def determineForConcat(
        level1: StringConstancyLevel, level2: StringConstancyLevel
    ): StringConstancyLevel = {
        if (level1 == PARTIALLY_CONSTANT || level2 == PARTIALLY_CONSTANT) {
            PARTIALLY_CONSTANT
        } else if ((level1 == CONSTANT && level2 == DYNAMIC) ||
            (level1 == DYNAMIC && level2 == CONSTANT)) {
            PARTIALLY_CONSTANT
        } else {
            level1
        }
    }

}

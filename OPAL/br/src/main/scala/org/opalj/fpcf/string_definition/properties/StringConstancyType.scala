/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.string_definition.properties

/**
 * Values in this enumeration represent the granularity of used strings.
 *
 * @author Patrick Mell
 */
object StringConstancyType extends Enumeration {

    type StringConstancyType = StringConstancyType.Value

    /**
     * This type is to be used when a string value is appended to another (and also when a certain
     * value represents an initialization, as an initialization can be seen as the concatenation
     * of the empty string with the init value).
     */
    final val APPEND = Value("append")

    /**
     * This type is to be used when a string value is reset, that is, the string is set to the empty
     * string (either by manually setting the value to the empty string or using a function like
     * [[StringBuilder#delete]]).
     */
    final val RESET = Value("reset")

}

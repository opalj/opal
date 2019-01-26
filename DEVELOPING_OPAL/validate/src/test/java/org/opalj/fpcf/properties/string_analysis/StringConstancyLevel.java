/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.properties.string_analysis;

/**
 * Java annotations do not work with Scala enums, such as
 * {@link org.opalj.br.fpcf.properties.string_definition.StringConstancyLevel}. Thus, this enum.
 *
 * @author Patrick Mell
 */
public enum StringConstancyLevel {

    // For details, see {@link org.opalj.fpcf.properties.StringConstancyLevel}.
    CONSTANT("constant"),
    PARTIALLY_CONSTANT("partially_constant"),
    DYNAMIC("dynamic");

    private final String value;

    StringConstancyLevel(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

}

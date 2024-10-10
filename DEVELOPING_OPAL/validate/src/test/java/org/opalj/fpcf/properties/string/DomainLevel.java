/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.properties.string;

/**
 * @see org.opalj.fpcf.fixtures.string.SimpleStringOps
 * @author Maximilian Rüsch
 */
public enum DomainLevel {

    L1("L1"),
    L2("L2");

    private final String value;

    DomainLevel(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}

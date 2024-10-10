/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.properties.string;

/**
 * @see org.opalj.fpcf.fixtures.string.SimpleStringOps
 * @author Maximilian Rüsch
 */
public enum Level {

    TRUTH("TRUTH"),
    L0("L0"),
    L1("L1"),
    L2("L2"),
    L3("L3");

    private final String value;

    Level(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}

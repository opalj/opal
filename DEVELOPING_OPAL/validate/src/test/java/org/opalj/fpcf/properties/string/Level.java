/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.properties.string;

/**
 * Restricts an annotation to certain string analysis level configurations. The value {@link Level#TRUTH } may be used
 * to explicitly define the ground truth that all test run configurations will fall back to if no more specific
 * annotation is found.
 * @see org.opalj.fpcf.fixtures.string.SimpleStringOps
 *
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

/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.properties.string;

/**
 * @see org.opalj.fpcf.fixtures.string.SimpleStringOps
 * @author Maximilian Rüsch
 */
public enum SoundnessMode {

    LOW("LOW"),
    HIGH("HIGH");

    private final String value;

    SoundnessMode(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}

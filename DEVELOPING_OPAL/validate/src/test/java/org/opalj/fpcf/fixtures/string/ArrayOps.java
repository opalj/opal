/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.string;

import org.opalj.fpcf.fixtures.string.tools.StringProvider;
import org.opalj.fpcf.properties.string_analysis.*;

/**
 * Various tests that test compatibility with array operations (mainly reads with specific array indexes).
 * Currently, the string analysis does not support array operations and thus fails for every level.
 *
 * @see SimpleStringOps
 */
public class ArrayOps {

    private String[] monthNames = { "January", "February", "March", getApril() };

    /**
     * Serves as the sink for string variables to be analyzed.
     */
    public void analyzeString(String s) {}

    @Constant(n = 0, levels = Level.TRUTH, value = "(java.lang.String|java.lang.StringBuilder|java.lang.System|java.lang.Runnable)")
    @Failure(n = 0, levels = { Level.L0, Level.L1, Level.L2, Level.L3 }, reason = "arrays are not supported")
    public void fromStringArray(int index) {
        String[] classes = {
                "java.lang.String", "java.lang.StringBuilder",
                "java.lang.System", "java.lang.Runnable"
        };
        if (index >= 0 && index < classes.length) {
            analyzeString(classes[index]);
        }
    }

    @Dynamic(n = 0, levels = Level.TRUTH, soundness = SoundnessMode.LOW,
            value = "(java.lang.Object|java.lang.Runtime|java.lang.Integer)")
    @Dynamic(n = 0, levels = Level.TRUTH, soundness = SoundnessMode.HIGH,
            value = "(java.lang.Object|java.lang.Runtime|java.lang.Integer|.*)")
    @Failure(n = 0, levels = { Level.L0, Level.L1, Level.L2, Level.L3 }, reason = "arrays are not supported")
    public void arrayStaticAndVirtualFunctionCalls(int i) {
        String[] classes = {
                "java.lang.Object",
                getRuntimeClassName(),
                StringProvider.getFQClassNameWithStringBuilder("java.lang", "Integer"),
                System.clearProperty("SomeClass")
        };
        analyzeString(classes[i]);
    }

    @Constant(n = 0, levels = Level.TRUTH, value = "(January|February|March|April)")
    @Failure(n = 0, levels = { Level.L0, Level.L1, Level.L2, Level.L3 }, reason = "arrays are not supported")
    public void getStringArrayField(int i) {
        analyzeString(monthNames[i]);
    }

    private String getRuntimeClassName() {
        return "java.lang.Runtime";
    }

    private String getApril() {
        return "April";
    }
}

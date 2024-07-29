/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.string_analysis;

import org.opalj.fpcf.fixtures.string_analysis.tools.StringProvider;
import org.opalj.fpcf.properties.string_analysis.*;

/**
 * @see SimpleStringOps
 */
public class ArrayOps {

    private String[] monthNames = { "January", "February", "March", getApril() };

    /**
     * Serves as the sink for string variables to be analyzed.
     */
    public void analyzeString(String s) {}

    @Constant(n = 0, value = "(java.lang.String|java.lang.StringBuilder|java.lang.System|java.lang.Runnable)", levels = Level.TRUTH)
    @Failure(n = 0, levels = { Level.L0, Level.L1 }, reason = "array accesses cannot be interpreted yet")
    public void fromStringArray(int index) {
        String[] classes = {
                "java.lang.String", "java.lang.StringBuilder",
                "java.lang.System", "java.lang.Runnable"
        };
        if (index >= 0 && index < classes.length) {
            analyzeString(classes[index]);
        }
    }

    @Dynamic(n = 0, levels = Level.TRUTH, value = "(java.lang.Object|java.lang.Runtime|java.lang.Integer|.*)")
    @Failure(n = 0, levels = { Level.L0, Level.L1 }, reason = "arrays are not supported")
    public void arrayStaticAndVirtualFunctionCalls(int i) {
        String[] classes = {
                "java.lang.Object",
                getRuntimeClassName(),
                StringProvider.getFQClassNameWithStringBuilder("java.lang", "Integer"),
                System.getProperty("SomeClass")
        };
        analyzeString(classes[i]);
    }

    @Constant(n = 0, levels = Level.TRUTH, value = "(January|February|March|April)")
    @Failure(n = 0, levels = { Level.L0, Level.L1 }, reason = "arrays are not supported")
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

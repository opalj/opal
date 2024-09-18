/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.string;

import org.opalj.fpcf.properties.string_analysis.*;

/**
 * Various tests that test whether detection of needing to resolve parameters for a given string works or not. Note that
 * there is a separate test file for various function calls that also partially covers this detection.
 *
 * @see FunctionCalls
 * @see SimpleStringOps
 */
public class FunctionParameter {

    /**
     * Serves as the sink for string variables to be analyzed.
     */
    public void analyzeString(String s) {}

    public void parameterCaller() {
        this.parameterRead("some-param-value", new StringBuilder("some-other-param-value"));
    }

    @Constant(n = 0, levels = Level.TRUTH, soundness = SoundnessMode.LOW, value = "some-param-value")
    @Dynamic(n = 0, levels = Level.TRUTH, soundness = SoundnessMode.HIGH, value = "(.*|some-param-value)",
            reason = "method is an entry point and thus has callers with unknown context")
    @Constant(n = 1, levels = Level.TRUTH, soundness = SoundnessMode.LOW, value = "some-other-param-value")
    @Dynamic(n = 1, levels = Level.TRUTH, soundness = SoundnessMode.HIGH, value = "(.*|some-other-param-value)",
            reason = "method is an entry point and thus has callers with unknown context")
    @Failure(n = 1, levels = Level.L0)
    @Constant(n = 2, levels = Level.TRUTH, soundness = SoundnessMode.LOW, value = "value=some-param-value")
    @PartiallyConstant(n = 2, levels = Level.TRUTH, soundness = SoundnessMode.HIGH, value = "value=(.*|some-param-value)",
            reason = "method is an entry point and thus has callers with unknown context")
    @Failure(n = 2, levels = Level.L0)
    @Constant(n = 3, levels = Level.TRUTH, soundness = SoundnessMode.LOW, value = "value=some-param-value-some-other-param-value")
    @PartiallyConstant(n = 3, levels = Level.TRUTH, soundness = SoundnessMode.HIGH, value = "value=(.*|some-param-value)-(.*|some-other-param-value)",
            reason = "method is an entry point and thus has callers with unknown context")
    @Failure(n = 3, levels = Level.L0)
    public void parameterRead(String stringValue, StringBuilder sbValue) {
        analyzeString(stringValue);
        analyzeString(sbValue.toString());

        StringBuilder sb = new StringBuilder("value=");
        System.out.println(sb.toString());
        sb.append(stringValue);
        analyzeString(sb.toString());

        sb.append("-");
        sb.append(sbValue.toString());
        analyzeString(sb.toString());
    }

    @Constant(n = 0, levels = Level.TRUTH, value = "java.lang.String")
    public void noParameterInformationRequiredTest(String s) {
        System.out.println(s);
        analyzeString("java.lang.String");
    }
}

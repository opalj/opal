/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.string;

import org.opalj.fpcf.fixtures.string.tools.StringProvider;
import org.opalj.fpcf.properties.string_analysis.*;

/**
 * @see SimpleStringOps
 */
public class FunctionCalls {

    /**
     * Serves as the sink for string variables to be analyzed.
     */
    public void analyzeString(String s) {}

    @Constant(n = 0, levels = Level.TRUTH, value = "java.lang.String")
    @Failure(n = 0, levels = Level.L0)
    @Constant(n = 1, levels = Level.TRUTH, value = "java.lang.Object")
    @Failure(n = 1, levels = Level.L0)
    public void simpleStringConcatWithStaticFunctionCalls() {
        analyzeString(StringProvider.concat("java.lang.", "String"));
        analyzeString(StringProvider.concat("java.", StringProvider.concat("lang.", "Object")));
    }

    @Constant(n = 0, levels = Level.TRUTH, value = "java.lang.StringBuilder")
    @Failure(n = 0, levels = { Level.L0, Level.L1 })
    public void fromFunctionCall() {
        analyzeString(getStringBuilderClassName());
    }

    @Constant(n = 0, levels = Level.TRUTH, value = "java.lang.StringBuilder")
    @Failure(n = 0, levels = Level.L0)
    @Invalid(n = 0, levels = Level.L1, soundness = SoundnessMode.LOW)
    @PartiallyConstant(n = 0, levels = Level.L1, soundness = SoundnessMode.HIGH, value = "java.lang..*")
    public void fromConstantAndFunctionCall() {
        String className = "java.lang.";
        System.out.println(className);
        className += getSimpleStringBuilderClassName();
        analyzeString(className);
    }

    @Constant(n = 0, levels = Level.TRUTH, value = "java.lang.Integer")
    @Failure(n = 0, levels = Level.L0)
    public void fromStaticMethodWithParamTest() {
        analyzeString(StringProvider.getFQClassNameWithStringBuilder("java.lang", "Integer"));
    }

    @Invalid(n = 0, levels = Level.TRUTH, reason = "the function has no return value, thus it does not return a string")
    public void functionWithNoReturnValue() {
        analyzeString(noReturnFunction());
    }

    /** Belongs to functionWithNoReturnValue. */
    public static String noReturnFunction() {
        throw new RuntimeException();
    }

    @Constant(n = 0, levels = Level.TRUTH, value = "Hello, World!")
    @Failure(n = 0, levels = Level.L0)
    @Constant(n = 1, levels = Level.TRUTH, value = "Hello, World?")
    @Failure(n = 1, levels = { Level.L0, Level.L1 })
    public void functionWithFunctionParameter() {
        analyzeString(addExclamationMark(getHelloWorld()));
        analyzeString(addQuestionMark(getHelloWorld()));
    }

    @Constant(n = 0, levels = Level.TRUTH, value = "(ERROR|java.lang.Object|java.lang.StringBuilder)")
    @Constant(n = 0, levels = { Level.L0, Level.L1 }, soundness = SoundnessMode.LOW, value = "ERROR")
    @Dynamic(n = 0, levels = { Level.L0, Level.L1 }, soundness = SoundnessMode.HIGH, value = "(.*|ERROR)")
    public void simpleNonVirtualFunctionCallTestWithIf(int i) {
        String s;
        if (i == 0) {
            s = getObjectClassName();
        } else if (i == 1) {
            s = getStringBuilderClassName();
        } else {
            s = "ERROR";
        }
        analyzeString(s);
    }

    @Constant(n = 0, levels = Level.TRUTH, value = "(ERROR|java.lang.Object|java.lang.StringBuilder)")
    @Failure(n = 0, levels = Level.L0)
    @Constant(n = 0, levels = Level.L1, soundness = SoundnessMode.LOW, value = "ERROR")
    @Dynamic(n = 0, levels = Level.L1, soundness = SoundnessMode.HIGH, value = "(.*|ERROR)")
    public void initFromNonVirtualFunctionCallTest(int i) {
        String s;
        if (i == 0) {
            s = getObjectClassName();
        } else if (i == 1) {
            s = getStringBuilderClassName();
        } else {
            s = "ERROR";
        }
        StringBuilder sb = new StringBuilder(s);
        analyzeString(sb.toString());
    }

    @Constant(n = 0, levels = Level.TRUTH, value = "It is (Hello, World|great)")
    @Failure(n = 0, levels = Level.L0)
    public void appendWithTwoDefSitesWithFuncCallTest(int i) {
        String s;
        if (i > 0) {
            s = "great";
        } else {
            s = getHelloWorld();
        }
        analyzeString(new StringBuilder("It is ").append(s).toString());
    }

    /**
     * A case where the single valid return value of the called function can be resolved without calling the function.
     */
    @Constant(n = 0, levels = Level.TRUTH, value = "val")
    @Failure(n = 0, levels = { Level.L0, Level.L1 }, domains = DomainLevel.L1)
    @Constant(n = 0, levels = { Level.L2, Level.L3 }, domains = DomainLevel.L1, value = "(One|java.lang.Object|val)")
    public void resolvableReturnValue() {
        analyzeString(resolvableReturnValueFunction("val", 42));
    }

    /**
     * Belongs to resolvableReturnValue.
     */
    private String resolvableReturnValueFunction(String s, int i) {
        switch (i) {
            case 0: return getObjectClassName();
            case 1: return "One";
            default: return s;
        }
    }

    @Constant(n = 0, levels = Level.TRUTH, value = "(One|java.lang.Object|val)")
    @Failure(n = 0, levels = { Level.L0, Level.L1 })
    public void severalReturnValuesTest1() {
        analyzeString(severalReturnValuesWithSwitchFunction("val", 42));
    }

    /** Belongs to severalReturnValuesTest1. */
    private String severalReturnValuesWithSwitchFunction(String s, int i) {
        switch (i) {
            case 0: return "One";
            case 1: return s;
            default: return getObjectClassName();
        }
    }

    @Constant(n = 0, levels = Level.TRUTH, value = "(Hello, World|that's odd)")
    @Failure(n = 0, levels = Level.L0)
    public void severalReturnValuesTest2() {
        analyzeString(severalReturnValuesWithIfElseFunction(42));
    }

    /** Belongs to severalReturnValuesTest2. */
    private static String severalReturnValuesWithIfElseFunction(int i) {
        // The ternary operator would create only a single "return" statement which is not what we want here
        if (i % 2 != 0) {
            return "that's odd";
        } else {
            return getHelloWorld();
        }
    }

    @Constant(n = 0, levels = Level.TRUTH, soundness = SoundnessMode.LOW, value = "(Hello, World|my.helper.Class)")
    @Dynamic(n = 0, levels = Level.TRUTH, soundness = SoundnessMode.HIGH, value = "(.*|Hello, World|my.helper.Class)")
    @Failure(n = 0, levels = Level.L0)
    public String calleeWithFunctionParameter(String s, float i) {
        analyzeString(s);
        return s;
    }

    @Constant(n = 0, levels = Level.TRUTH, value = "Hello, World")
    @Failure(n = 0, levels = { Level.L0, Level.L1 })
    public void firstCallerForCalleeWithFunctionParameter() {
        String s = calleeWithFunctionParameter(getHelloWorldProxy(), 900);
        analyzeString(s);
    }

    public void secondCallerForCalleeWithFunctionParameter() {
        calleeWithFunctionParameter(getHelperClassProxy(), 900);
    }

    @Constant(n = 0, levels = Level.TRUTH, soundness = SoundnessMode.LOW, value = "(Hello, World|my.helper.Class)")
    @Dynamic(n = 0, levels = Level.TRUTH, soundness = SoundnessMode.HIGH, value = "(.*|Hello, World|my.helper.Class)")
    @Failure(n = 0, levels = Level.L0)
    public String calleeWithFunctionParameterMultipleCallsInSameMethodTest(String s, float i) {
        analyzeString(s);
        return s;
    }

    public void callerForCalleeWithFunctionParameterMultipleCallsInSameMethodTest() {
        calleeWithFunctionParameterMultipleCallsInSameMethodTest(getHelloWorldProxy(), 900);
        calleeWithFunctionParameterMultipleCallsInSameMethodTest(getHelperClassProxy(), 900);
    }

    @Constant(n = 0, levels = Level.TRUTH, soundness = SoundnessMode.LOW, value = "(string.1|string.2)")
    @Dynamic(n = 0, levels = Level.TRUTH, soundness = SoundnessMode.HIGH, value = "(.*|string.1|string.2)")
    public String calleeWithStringParameterMultipleCallsInSameMethodTest(String s, float i) {
        analyzeString(s);
        return s;
    }

    public void callerForCalleeWithStringParameterMultipleCallsInSameMethodTest() {
        calleeWithStringParameterMultipleCallsInSameMethodTest("string.1", 900);
        calleeWithStringParameterMultipleCallsInSameMethodTest("string.2", 900);
    }

    public static String getHelloWorldProxy() {
        return getHelloWorld();
    }

    public static String getHelperClassProxy() {
        return getHelperClass();
    }

    private static String getHelloWorld() {
        return "Hello, World";
    }

    private static String getHelperClass() {
        return "my.helper.Class";
    }

    private String getStringBuilderClassName() {
        return "java.lang.StringBuilder";
    }

    private String getSimpleStringBuilderClassName() {
        return "StringBuilder";
    }

    private String getObjectClassName() {
        return "java.lang.Object";
    }

    private static String addExclamationMark(String s) {
        return s + "!";
    }

    private String addQuestionMark(String s) {
        return s + "?";
    }
}

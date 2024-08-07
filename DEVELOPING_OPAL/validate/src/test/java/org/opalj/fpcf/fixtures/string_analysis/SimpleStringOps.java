/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.string_analysis;

import org.opalj.fpcf.properties.string_analysis.*;

/**
 * All files in this package define various tests for the string analysis. The following things are to be considered
 * when adding test cases:
 * <ul>
 * <li> The asterisk symbol (*) is used to indicate that a string (or part of it) can occur >= 0 times. </li>
 * <li> Question marks (?) are used to indicate that a string (or part of it) can occur either zero times or once. </li>
 * <li> The string "\w" is used to indicate that a string (or part of it) is unknown / arbitrary, i.e., it cannot be approximated. </li>
 * <li> The pipe symbol is used to indicate that a string (or part of it) consists of one of several options (but definitely one of these values). </li>
 * <li> Brackets ("(" and ")") are used for nesting and grouping string expressions. </li>
 * <li>
 * The string "^-?\d+$" represents (positive and negative) integer numbers. This RegExp has been taken
 * from https://www.freeformatter.com/java-regex-tester.html#examples as of 2019-02-02.
 * </li>
 * <li>
 * The string "^-?\\d*\\.{0,1}\\d+$" represents (positive and negative) float and double numbers.
 * This RegExp has been taken from https://www.freeformatter.com/java-regex-tester.html#examples as
 * of 2019-02-02.
 * </li>
 * </ul>
 * <p>
 * Thus, you should avoid the following characters / strings to occur in "expectedStrings":
 * {*, ?, \w, |}. In the future, "expectedStrings" might be parsed back into a StringTree. Thus, to
 * be on the safe side, brackets should be avoided as well.
 * <p>
 * On order to trigger the analysis for a particular string or String{Buffer, Builder} call the
 * <i>analyzeString</i> method with the variable to be analyzed. It is legal to have multiple
 * calls to <i>analyzeString</i> within the same test method.
 *
 * @author Maximilian Rüsch
 */
public class SimpleStringOps {

    /**
     * Serves as the sink for string variables to be analyzed.
     */
    public void analyzeString(String s) {}

    @Constant(n = 0, levels = Level.TRUTH, value = "java.lang.String")
    @Constant(n = 1, levels = Level.TRUTH, value = "java.lang.String")
    public void constantStringReads() {
        analyzeString("java.lang.String");

        String className = "java.lang.String";
        analyzeString(className);
    }

    @Constant(n = 0, levels = Level.TRUTH, value = "c")
    @Failure(n = 0, levels = Level.L0)
    @Constant(n = 1, levels = Level.TRUTH, value = "42.3")
    @Failure(n = 1, levels = Level.L0)
    @Constant(n = 2, levels = Level.TRUTH, value = "java.lang.Runtime")
    @Failure(n = 2, levels = { Level.L0, Level.L1 })
    public void valueOfTest() {
        analyzeString(String.valueOf('c'));
        analyzeString(String.valueOf((float) 42.3));
        analyzeString(String.valueOf(getRuntimeClassName()));
    }

    @Constant(n = 0, levels = Level.TRUTH, value = "java.lang.String")
    @Failure(n = 0, levels = Level.L0)
    @Constant(n = 1, levels = Level.TRUTH, value = "java.lang.Object")
    @Failure(n = 1, levels = Level.L0)
    public void simpleStringConcat() {
        String className1 = "java.lang.";
        System.out.println(className1);
        className1 += "String";
        analyzeString(className1);

        String className2 = "java.";
        System.out.println(className2);
        className2 += "lang.";
        System.out.println(className2);
        className2 += "Object";
        analyzeString(className2);
    }

    @Constant(n = 0, levels = Level.TRUTH, value = "va.")
    @Failure(n = 0, levels = Level.L0)
    @Constant(n = 1, levels = Level.TRUTH, value = "va.lang.")
    @Failure(n = 1, levels = Level.L0)
    public void simpleSubstring() {
        String someString = "java.lang.";
        analyzeString(someString.substring(2, 5));
        analyzeString(someString.substring(2));
    }

    @Constant(n = 0, levels = Level.TRUTH, value = "(java.lang.Runtime|java.lang.System)")
    public void multipleConstantDefSites(boolean cond) {
        String s;
        if (cond) {
            s = "java.lang.System";
        } else {
            s = "java.lang.Runtime";
        }
        analyzeString(s);
    }

    @Constant(n = 0, levels = Level.TRUTH, value = "It is (great|not great)")
    @Failure(n = 0, levels = Level.L0)
    public void appendWithTwoDefSites(int i) {
        String s;
        if (i > 0) {
            s = "great";
        } else {
            s = "not great";
        }
        analyzeString(new StringBuilder("It is ").append(s).toString());
    }

    @Constant(n = 0, levels = Level.TRUTH, value = "(Some|SomeOther)")
    @Constant(n = 0, levels = Level.L0, soundness = SoundnessMode.LOW, value = "Some")
    @Dynamic(n = 0, levels = Level.L0, soundness = SoundnessMode.HIGH, value = "(.*|Some)")
    @Dynamic(n = 1, levels = Level.TRUTH, value = "(.*|Some)")
    @PartiallyConstant(n = 2, levels = Level.TRUTH, value = "(Some.*|SomeOther)")
    @Failure(n = 2, levels = Level.L0)
    public void ternaryOperators(boolean flag, String param) {
        String s1 = "Some";
        String s2 = s1 + "Other";

        analyzeString(flag ? s1 : s2);
        analyzeString(flag ? s1 : param);
        analyzeString(flag ? s1 + param : s2);
    }

    @Constant(n = 0, levels = Level.TRUTH, value = "(a|ab|ac)")
    @Failure(n = 0, levels = Level.L0)
    public void switchRelevantAndIrrelevant(int value) {
        StringBuilder sb = new StringBuilder("a");
        switch (value) {
            case 0:
                sb.append("b");
                break;
            case 1:
                sb.append("c");
                break;
            case 3:
                break;
            case 4:
                break;
        }
        analyzeString(sb.toString());
    }

    @Constant(n = 0, levels = Level.TRUTH, value = "(a|ab|ac|ad)")
    @Failure(n = 0, levels = Level.L0)
    public void switchRelevantAndIrrelevantWithRelevantDefault(int value) {
        StringBuilder sb = new StringBuilder("a");
        switch (value) {
            case 0:
                sb.append("b");
                break;
            case 1:
                sb.append("c");
                break;
            case 2:
                break;
            case 3:
                break;
            default:
                sb.append("d");
                break;
        }
        analyzeString(sb.toString());
    }

    @Constant(n = 0, levels = Level.TRUTH, value = "(a|ab|ac)")
    @Failure(n = 0, levels = Level.L0)
    public void switchRelevantAndIrrelevantWithIrrelevantDefault(int value) {
        StringBuilder sb = new StringBuilder("a");
        switch (value) {
            case 0:
                sb.append("b");
                break;
            case 1:
                sb.append("c");
                break;
            case 2:
                break;
            case 3:
                break;
            default:
                break;
        }
        analyzeString(sb.toString());
    }

    @Constant(n = 0, levels = Level.TRUTH, value = "(ab|ac|ad)")
    @Failure(n = 0, levels = Level.L0)
    public void switchRelevantWithRelevantDefault(int value) {
        StringBuilder sb = new StringBuilder("a");
        switch (value) {
            case 0:
                sb.append("b");
                break;
            case 1:
                sb.append("c");
                break;
            default:
                sb.append("d");
                break;
        }
        analyzeString(sb.toString());
    }

    @Constant(n = 0, levels = Level.TRUTH, value = "(a|ab|ac|ad|af)")
    @Failure(n = 0, levels = Level.L0)
    public void switchNestedNoNestedDefault(int value, int value2) {
        StringBuilder sb = new StringBuilder("a");
        switch (value) {
            case 0:
                sb.append("b");
                break;
            case 1:
                switch (value2) {
                    case 0:
                        sb.append("c");
                        break;
                    case 1:
                        sb.append("d");
                        break;
                }
                break;
            default:
                sb.append("f");
                break;
        }
        analyzeString(sb.toString());
    }

    @Constant(n = 0, levels = Level.TRUTH, value = "(ab|ac|ad|ae|af)")
    @Failure(n = 0, levels = Level.L0)
    public void switchNestedWithNestedDefault(int value, int value2) {
        StringBuilder sb = new StringBuilder("a");
        switch (value) {
            case 0:
                sb.append("b");
                break;
            case 1:
                switch (value2) {
                    case 0:
                        sb.append("c");
                        break;
                    case 1:
                        sb.append("d");
                        break;
                    default:
                        sb.append("e");
                        break;
                }
                break;
            default:
                sb.append("f");
                break;
        }
        analyzeString(sb.toString());
    }

    /**
     * A more comprehensive case where multiple definition sites have to be considered each with a different string
     * generation mechanism
     */
    @Dynamic(n = 0, levels = Level.TRUTH, value = "(java.lang.Object|java.lang.Runtime|java.lang.System|java.lang.StringBuilder)")
    @Constant(n = 0, levels = Level.L0, soundness = SoundnessMode.LOW, value = "java.lang.System")
    @Dynamic(n = 0, levels = Level.L0, soundness = SoundnessMode.HIGH, value = "(.*|java.lang.System)")
    @Constant(n = 0, levels = Level.L1, soundness = SoundnessMode.LOW, value = "java.lang.System")
    @Dynamic(n = 0, levels = Level.L1, soundness = SoundnessMode.HIGH, value = "(.*|java.lang..*|java.lang.System)")
    @Constant(n = 0, levels = Level.L2, soundness = SoundnessMode.LOW, value = "(java.lang.StringBuilder|java.lang.StringBuilder|java.lang.System)")
    @Dynamic(n = 0, levels = Level.L2, soundness = SoundnessMode.HIGH, value = "(.*|java.lang.StringBuilder|java.lang.StringBuilder|java.lang.System)")
    public void multipleDefSites(int value) {
        String[] arr = new String[] { "java.lang.Object", getRuntimeClassName() };

        String s;
        switch (value) {
        case 0:
            s = arr[value];
            break;
        case 1:
            s = arr[value];
            break;
        case 3:
            s = "java.lang.System";
            break;
        case 4:
            s = "java.lang." + getSimpleStringBuilderClassName();
            break;
        default:
            s = getStringBuilderClassName();
        }

        analyzeString(s);
    }

    private String getRuntimeClassName() {
        return "java.lang.Runtime";
    }

    private String getStringBuilderClassName() {
        return "java.lang.StringBuilder";
    }

    private String getSimpleStringBuilderClassName() {
        return "StringBuilder";
    }
}

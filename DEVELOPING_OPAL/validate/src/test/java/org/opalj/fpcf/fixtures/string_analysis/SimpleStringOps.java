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

    // read-only string variable, trivial case
    @Constant(n = 0, value = "java.lang.String")
    @Constant(n = 1, value = "java.lang.String")
    public void constantStringReads() {
        analyzeString("java.lang.String");

        String className = "java.lang.String";
        analyzeString(className);
    }

    @Constant(n = 0, value = "c")
    @Constant(n = 1, value = "42.3")
    @Constant(n = 2, levels = Level.TRUTH, value = "java.lang.Runtime")
    @Failure(n = 2, levels = Level.L0)
    public void valueOfTest() {
        analyzeString(String.valueOf('c'));
        analyzeString(String.valueOf((float) 42.3));
        analyzeString(String.valueOf(getRuntimeClassName()));
    }

    // checks if a string value with append(s) is determined correctly
    @Constant(n = 0, value = "java.lang.String")
    @Constant(n = 1, value = "java.lang.Object")
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

    // checks if the substring of a constant string value is determined correctly
    @Constant(n = 0, value = "va.")
    @Constant(n = 1, value = "va.lang.")
    public void simpleSubstring() {
        String someString = "java.lang.";
        analyzeString(someString.substring(2, 5));
        analyzeString(someString.substring(2));
    }

    @Constant(n = 0, value = "(java.lang.System|java.lang.Runtime)")
    public void multipleConstantDefSites(boolean cond) {
        String s;
        if (cond) {
            s = "java.lang.System";
        } else {
            s = "java.lang.Runtime";
        }
        analyzeString(s);
    }

    @Constant(n = 0, value = "It is (great|not great)")
    public void appendWithTwoDefSites(int i) {
        String s;
        if (i > 0) {
            s = "great";
        } else {
            s = "not great";
        }
        analyzeString(new StringBuilder("It is ").append(s).toString());
    }

    @Constant(n = 0, value = "(Some|SomeOther)")
    @Dynamic(n = 1, value = "(.*|Some)")
    @PartiallyConstant(n = 2, value = "(SomeOther|Some.*)")
    public void ternaryOperators(boolean flag, String param) {
        String s1 = "Some";
        String s2 = s1 + "Other";

        analyzeString(flag ? s1 : s2);
        analyzeString(flag ? s1 : param);
        analyzeString(flag ? s1 + param : s2);
    }

    @Constant(n = 0, value = "(a|ab|ac)")
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

    @Constant(n = 0, value = "(ab|ac|a|ad)")
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

    @Constant(n = 0, value = "(a|ab|ac)")
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

    @Constant(n = 0, value = "(ab|ac|ad)")
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

    @Constant(n = 0, value = "(ab|ac|a|ad|af)")
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

    @Constant(n = 0, value = "(ab|ac|ad|ae|af)")
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
    @Dynamic(n = 0, levels = Level.L0, soundness = SoundnessMode.HIGH, value = "(.*|java.lang.System|java.lang..*)")
    @Constant(n = 0, levels = Level.L1, soundness = SoundnessMode.LOW, value = "(java.lang.System|java.lang.StringBuilder|java.lang.StringBuilder)")
    @Dynamic(n = 0, levels = Level.L1, soundness = SoundnessMode.HIGH, value = "(.*|java.lang.System|java.lang.StringBuilder|java.lang.StringBuilder)")
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

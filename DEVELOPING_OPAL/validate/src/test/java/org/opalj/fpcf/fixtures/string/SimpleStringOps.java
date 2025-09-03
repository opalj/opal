/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.string;

import org.opalj.fpcf.properties.string.*;

/**
 * All files in this package define various tests for the string analysis. The following things are to be considered
 * when adding test cases:
 *
 * <ul>
 *   <li>
 *     In order to trigger the analysis for a particular string variable, call the <i>analyzeString</i> method with the
 *     variable to be analyzed. Multiple calls to the sink <i>analyzeString</i> within the same test method are allowed.
 *   </li>
 *   <li>
 *     For a given sink call, the expected string and its constancy can be defined using one of these annotations:
 *     <ul>
 *       <li>
 *         {@link Invalid} The given string variable does not contain any string analyzable by the string
 *         analysis. Usually used as a fallback in low-soundness mode.
 *       </li>
 *       <li>
 *         {@link Constant} The given string variable contains only constant strings and its set of possible
 *         values is thus enumerable within finite time.
 *       </li>
 *       <li>
 *         {@link PartiallyConstant} The given string variable contains strings which have some constant part
 *         concatenated with some dynamic part. Its set of possible values is constrained but not enumerable
 *         within finite time.
 *       </li>
 *       <li>
 *         {@link Dynamic} The given string variable contains strings which only consist of dynamic information.
 *         Its set of possible values may be constrained but is definitely not enumerable within finite time.
 *         Usually used as a fallback in high-soundness mode.
 *       </li>
 *       <li>
 *         {@link Failure} Combines {@link Invalid} and {@link Dynamic} by generating the former for test runs in
 *         low-soundness mode and the latter for test runs in high-soundness mode.
 *       </li>
 *     </ul>
 *   </li>
 *   <li>
 *     For each test run configuration (different domain level, different soundness mode, different analysis level)
 *     exactly one such annotation should be defined for each test function. For every annotation, the following
 *     information should / can be given:
 *     <ul>
 *       <li>
 *         (Required) <code>sinkIndex = ?</code>: The index of the sink call that this annotation is defined for.
 *       </li>
 *       <li>
 *         (Required) <code>value = "?"</code>: The expected value (see below for format).
 *         Cannot be defined for {@link Invalid} annotations.
 *       </li>
 *       <li>
 *         (Required) <code>levels = ?</code>: One or multiple of {@link Level} to allow restricting an annotation
 *         to certain string analysis level configurations. The value {@link Level#TRUTH } may be used to explicitly
 *         define the ground truth that all test run configurations will fall back to if no more specific annotation
 *         is found.
 *       </li>
 *       <li>
 *         (Optional) <code>domains = ?</code>: One or multiple of {@link DomainLevel} to allow restricting an
 *         annotation to certain domain level configurations.
 *       </li>
 *       <li>
 *         (Optional) <code>soundness = ?</code>: One or multiple of {@link SoundnessMode} to allow restricting an
 *         annotation to certain soundness mode configurations.
 *       </li>
 *       <li>
 *         (Optional) <code>reason = "?"</code>: Some reasoning for the given annotation type and value. Not part of
 *         the test output.
 *       </li>
 *     </ul>
 *   </li>
 *   <li>
 *     Expected values for string variables should be given in a reduced regex format:
 *     <ul>
 *        <li> The asterisk symbol (*) is used to indicate that a string (or part of it) can occur >= 0 times. </li>
 *        <li>
 *          The pipe symbol is used to indicate that a string (or part of it) consists of one of several options
 *          (but definitely one of these values).
 *        </li>
 *        <li> Brackets ("(" and ")") are used for nesting and grouping string expressions. </li>
 *        <li>
 *          The string "-?\d+" represents (positive and negative) integer numbers. This RegExp has been taken from
 *          <a href="https://www.freeformatter.com/java-regex-tester.html#examples">www.freeformatter.com/java-regex-tester.html</a>
 *          as of 2019-02-02.
 *        </li>
 *        <li>
 *          The string "-?\\d*\\.{0,1}\\d+" represents (positive and negative) float and double numbers. This RegExp
 *          has been taken from
 *          <a href="https://www.freeformatter.com/java-regex-tester.html#examples">www.freeformatter.com/java-regex-tester.html</a>
 *          as of 2019-02-02.
 *        </li>
 *      </ul>
 *   </li>
 * </ul>
 * <p>
 * This file defines various tests related to simple operations on strings and presence of multiple def sites of such
 * strings.
 *
 * @author Maximilian Rüsch
 */
public class SimpleStringOps {

    /**
     * Serves as the sink for string variables to be analyzed.
     */
    public void analyzeString(String s) {}

    @Constant(sinkIndex = 0, levels = Level.TRUTH, value = "java.lang.String")
    @Constant(sinkIndex = 1, levels = Level.TRUTH, value = "java.lang.String")
    public void constantStringReads() {
        analyzeString("java.lang.String");

        String className = "java.lang.String";
        analyzeString(className);
    }

    @Constant(sinkIndex = 0, levels = Level.TRUTH, value = "c")
    @Failure(sinkIndex = 0, levels = Level.L0)
    @Constant(sinkIndex = 1, levels = Level.TRUTH, value = "42.3")
    @Failure(sinkIndex = 1, levels = Level.L0)
    @Constant(sinkIndex = 2, levels = Level.TRUTH, value = "java.lang.Runtime")
    @Failure(sinkIndex = 2, levels = { Level.L0, Level.L1 })
    public void valueOfTest() {
        analyzeString(String.valueOf('c'));
        analyzeString(String.valueOf((float) 42.3));
        analyzeString(String.valueOf(getRuntimeClassName()));
    }

    @Constant(sinkIndex = 0, levels = Level.TRUTH, value = "java.lang.String")
    @Failure(sinkIndex = 0, levels = Level.L0)
    @Constant(sinkIndex = 1, levels = Level.TRUTH, value = "java.lang.Object")
    @Failure(sinkIndex = 1, levels = Level.L0)
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

    @Constant(sinkIndex = 0, levels = Level.TRUTH, value = "va.")
    @Failure(sinkIndex = 0, levels = Level.L0)
    @Constant(sinkIndex = 1, levels = Level.TRUTH, value = "va.lang.")
    @Failure(sinkIndex = 1, levels = Level.L0)
    public void simpleSubstring() {
        String someString = "java.lang.";
        analyzeString(someString.substring(2, 5));
        analyzeString(someString.substring(2));
    }

    @Constant(sinkIndex = 0, levels = Level.TRUTH, value = "(java.lang.Runtime|java.lang.System)")
    public void multipleConstantDefSites(boolean cond) {
        String s;
        if (cond) {
            s = "java.lang.System";
        } else {
            s = "java.lang.Runtime";
        }
        analyzeString(s);
    }

    @Constant(sinkIndex = 0, levels = Level.TRUTH, value = "It is (great|not great)")
    @Failure(sinkIndex = 0, levels = Level.L0)
    public void appendWithTwoDefSites(int i) {
        String s;
        if (i > 0) {
            s = "great";
        } else {
            s = "not great";
        }
        analyzeString(new StringBuilder("It is ").append(s).toString());
    }

    @Constant(sinkIndex = 0, levels = Level.TRUTH, value = "(Some|SomeOther)")
    @Constant(sinkIndex = 0, levels = Level.L0, soundness = SoundnessMode.LOW, value = "Some")
    @Dynamic(sinkIndex = 0, levels = Level.L0, soundness = SoundnessMode.HIGH, value = "(.*|Some)")
    @Constant(sinkIndex = 1, levels = Level.TRUTH, value = "(Impostor|Some)")
    @Constant(sinkIndex = 2, levels = Level.TRUTH, value = "(SomeImpostor|SomeOther)")
    @Failure(sinkIndex = 2, levels = Level.L0)
    public void ternaryOperators(boolean flag) {
        String s1 = "Some";
        String s2 = s1 + "Other";
        String s3 = "Impostor";

        analyzeString(flag ? s1 : s2);
        analyzeString(flag ? s1 : s3);
        analyzeString(flag ? s1 + s3 : s2);
    }

    /**
     * A more comprehensive case where multiple definition sites have to be considered each with a different string
     * generation mechanism
     */
    @Constant(sinkIndex = 0, levels = Level.TRUTH, value = "(java.lang.Object|java.lang.Runtime|java.lang.System|java.lang.StringBuilder)")
    @Constant(sinkIndex = 0, levels = Level.L0, soundness = SoundnessMode.LOW, value = "java.lang.System")
    @Dynamic(sinkIndex = 0, levels = Level.L0, soundness = SoundnessMode.HIGH, value = "(.*|java.lang.System)")
    @Constant(sinkIndex = 0, levels = Level.L1, soundness = SoundnessMode.LOW, value = "java.lang.System")
    @Dynamic(sinkIndex = 0, levels = Level.L1, soundness = SoundnessMode.HIGH, value = "(.*|java.lang..*|java.lang.System)")
    @Constant(sinkIndex = 0, levels = { Level.L2, Level.L3 }, soundness = SoundnessMode.LOW, value = "(java.lang.StringBuilder|java.lang.StringBuilder|java.lang.System)")
    @Dynamic(sinkIndex = 0, levels = { Level.L2, Level.L3 }, soundness = SoundnessMode.HIGH, value = "(.*|java.lang.StringBuilder|java.lang.StringBuilder|java.lang.System)")
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

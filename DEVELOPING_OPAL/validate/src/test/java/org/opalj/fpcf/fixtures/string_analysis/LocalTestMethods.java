/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.string_analysis;

import org.opalj.fpcf.properties.string_analysis.StringDefinitions;
import org.opalj.fpcf.properties.string_analysis.StringDefinitionsCollection;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Random;

import static org.opalj.fpcf.properties.string_analysis.StringConstancyLevel.*;

/**
 * This file contains various tests for the LocalStringAnalysis. The following things are to be
 * considered when adding test cases:
 * <ul>
 * <li>
 * The asterisk symbol (*) is used to indicate that a string (or part of it) can occur >= 0 times.
 * </li>
 * <li>
 * Question marks (?) are used to indicate that a string (or part of it) can occur either zero
 * times or once.
 * </li>
 * <li>
 * The string "\w" is used to indicate that a string (or part of it) is unknown / arbitrary, i.e.,
 * it cannot be approximated.
 * </li>
 * <li>
 * The pipe symbol is used to indicate that a string (or part of it) consists of one of several
 * options (but definitely one of these values).
 * </li>
 * <li>
 * Brackets ("(" and "(") are used for nesting and grouping string expressions.
 * </li>
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
 * @author Patrick Mell
 */
public class LocalTestMethods {

    private String someStringField = "";
    public static final String MY_CONSTANT = "mine";

    /**
     * This method represents the test method which is serves as the trigger point for the
     * {@link org.opalj.fpcf.IntraproceduralStringAnalysisTest} to know which string read operation to
     * analyze.
     * Note that the {@link StringDefinitions} annotation is designed in a way to be able to capture
     * only one read operation. For how to get around this limitation, see the annotation.
     *
     * @param s Some string which is to be analyzed.
     */
    public void analyzeString(String s) {
    }

    @StringDefinitionsCollection(
            value = "read-only string variable, trivial case",
            stringDefinitions = {
                    @StringDefinitions(
                            expectedLevel = CONSTANT, expectedStrings = "java.lang.String"
                    ),
                    @StringDefinitions(
                            expectedLevel = CONSTANT, expectedStrings = "java.lang.String"
                    )
            }
    )
    public void constantStringReads() {
        analyzeString("java.lang.String");

        String className = "java.lang.String";
        analyzeString(className);
    }

    @StringDefinitionsCollection(
            value = "checks if a string value with append(s) is determined correctly",
            stringDefinitions = {
                    @StringDefinitions(
                            expectedLevel = CONSTANT, expectedStrings = "java.lang.String"
                    ),
                    @StringDefinitions(
                            expectedLevel = CONSTANT, expectedStrings = "java.lang.Object"
                    )
            }
    )
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

    @StringDefinitionsCollection(
            value = "checks if a string value with > 2 continuous appends is determined correctly",
            stringDefinitions = {
                    @StringDefinitions(
                            expectedLevel = CONSTANT, expectedStrings = "java.lang.String"
                    )
            })
    public void directAppendConcats() {
        StringBuilder sb = new StringBuilder("java");
        sb.append(".").append("lang").append(".").append("String");
        analyzeString(sb.toString());
    }

    @StringDefinitionsCollection(
            value = "at this point, function call cannot be handled => DYNAMIC",
            stringDefinitions = {
                    @StringDefinitions(
                            expectedLevel = DYNAMIC, expectedStrings = ".*"
                    )
            })
    public void fromFunctionCall() {
        String className = getStringBuilderClassName();
        analyzeString(className);
    }

    @StringDefinitionsCollection(
            value = "constant string + string from function call => PARTIALLY_CONSTANT",
            stringDefinitions = {
                    @StringDefinitions(
                            expectedLevel = PARTIALLY_CONSTANT, expectedStrings = "java.lang..*"
                    )
            })
    public void fromConstantAndFunctionCall() {
        String className = "java.lang.";
        System.out.println(className);
        className += getSimpleStringBuilderClassName();
        analyzeString(className);
    }

    @StringDefinitionsCollection(
            value = "array access with unknown index",
            stringDefinitions = {
                    @StringDefinitions(
                            expectedLevel = CONSTANT, expectedStrings = "(java.lang.String|"
                            + "java.lang.StringBuilder|java.lang.System|java.lang.Runnable)"
                    )
            })
    public void fromStringArray(int index) {
        String[] classes = {
                "java.lang.String", "java.lang.StringBuilder",
                "java.lang.System", "java.lang.Runnable"
        };
        if (index >= 0 && index < classes.length) {
            analyzeString(classes[index]);
        }
    }

    @StringDefinitionsCollection(
            value = "a simple case where multiple definition sites have to be considered",
            stringDefinitions = {
                    @StringDefinitions(
                            expectedLevel = CONSTANT,
                            expectedStrings = "(java.lang.System|java.lang.Runtime)"
                    )
            })
    public void multipleConstantDefSites(boolean cond) {
        String s;
        if (cond) {
            s = "java.lang.System";
        } else {
            s = "java.lang.Runtime";
        }
        analyzeString(s);
    }

    @StringDefinitionsCollection(
            value = "a more comprehensive case where multiple definition sites have to be "
                    + "considered each with a different string generation mechanism",
            stringDefinitions = {
                    @StringDefinitions(
                            expectedLevel = DYNAMIC,
                            expectedStrings = "((java.lang.Object|.*)|java.lang.System|"
                                    + "java.lang..*|.*)"
                    )
            })
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

    @StringDefinitionsCollection(
            value = "a case where multiple optional definition sites have to be considered.",
            stringDefinitions = {
                    @StringDefinitions(
                            expectedLevel = CONSTANT, expectedStrings = "a(b|c)?"
                    )
            })
    public void multipleOptionalAppendSites(int value) {
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

    @StringDefinitionsCollection(
            value = "if-else control structure which append to a string builder with an int expr "
                    + "and an int",
            stringDefinitions = {
                    @StringDefinitions(
                            expectedLevel = DYNAMIC, expectedStrings = "(x|^-?\\d+$)"
                    ),
                    @StringDefinitions(
                            expectedLevel = CONSTANT, expectedStrings = "(42-42|x)"
                    )
            })
    public void ifElseWithStringBuilderWithIntExpr() {
        StringBuilder sb1 = new StringBuilder();
        StringBuilder sb2 = new StringBuilder();
        int i = new Random().nextInt();
        if (i % 2 == 0) {
            sb1.append("x");
            sb2.append(42);
            sb2.append(-42);
        } else {
            sb1.append(i + 1);
            sb2.append("x");
        }
        analyzeString(sb1.toString());
        analyzeString(sb2.toString());
    }

    @StringDefinitionsCollection(
            value = "if-else control structure which append float and double values to a string "
                    + "builder",
            stringDefinitions = {
                    @StringDefinitions(
                            expectedLevel = PARTIALLY_CONSTANT,
                            expectedStrings = "(3.14|^-?\\d*\\.{0,1}\\d+$)2.71828"
                    )
            })
    public void ifElseWithStringBuilderWithFloatExpr() {
        StringBuilder sb1 = new StringBuilder();
        int i = new Random().nextInt();
        if (i % 2 == 0) {
            sb1.append(3.14);
        } else {
            sb1.append(new Random().nextFloat());
        }
        float e = (float) 2.71828;
        sb1.append(e);
        analyzeString(sb1.toString());
    }

    @StringDefinitionsCollection(
            value = "if-else control structure which append to a string builder",
            stringDefinitions = {
                    @StringDefinitions(
                            expectedLevel = CONSTANT, expectedStrings = "(a|b)"
                    ),
                    @StringDefinitions(
                            expectedLevel = CONSTANT, expectedStrings = "a(b|c)"
                    )
            })
    public void ifElseWithStringBuilder1() {
        StringBuilder sb1;
        StringBuilder sb2 = new StringBuilder("a");

        int i = new Random().nextInt();
        if (i % 2 == 0) {
            sb1 = new StringBuilder("a");
            sb2.append("b");
        } else {
            sb1 = new StringBuilder("b");
            sb2.append("c");
        }
        analyzeString(sb1.toString());
        analyzeString(sb2.toString());
    }

    @StringDefinitionsCollection(
            value = "if-else control structure which append to a string builder multiple times",
            stringDefinitions = {
                    @StringDefinitions(
                            expectedLevel = CONSTANT, expectedStrings = "a(bcd|xyz)"
                    )
            })
    public void ifElseWithStringBuilder3() {
        StringBuilder sb = new StringBuilder("a");
        int i = new Random().nextInt();
        if (i % 2 == 0) {
            sb.append("b");
            sb.append("c");
            sb.append("d");
        } else {
            sb.append("x");
            sb.append("y");
            sb.append("z");
        }
        analyzeString(sb.toString());
    }

    @StringDefinitionsCollection(
            value = "simple for loops with known and unknown bounds",
            stringDefinitions = {
                    // Currently, the analysis does not support determining loop ranges => a(b)*
                    @StringDefinitions(
                            expectedLevel = CONSTANT, expectedStrings = "a(b)*"
                    ),
                    @StringDefinitions(
                            expectedLevel = CONSTANT, expectedStrings = "a(b)*"
                    )
            })
    public void simpleForLoopWithKnownBounds() {
        StringBuilder sb = new StringBuilder("a");
        for (int i = 0; i < 10; i++) {
            sb.append("b");
        }
        analyzeString(sb.toString());

        int limit = new Random().nextInt();
        sb = new StringBuilder("a");
        for (int i = 0; i < limit; i++) {
            sb.append("b");
        }
        analyzeString(sb.toString());
    }

    @StringDefinitionsCollection(
            value = "if-else control structure within a for loop and with an append afterwards",
            stringDefinitions = {
                    @StringDefinitions(
                            expectedLevel = PARTIALLY_CONSTANT,
                            expectedStrings = "((x|^-?\\d+$))*yz"
                    )
            })
    public void ifElseInLoopWithAppendAfterwards() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 20; i++) {
            if (i % 2 == 0) {
                sb.append("x");
            } else {
                sb.append(i + 1);
            }
        }
        sb.append("yz");

        analyzeString(sb.toString());
    }

    @StringDefinitionsCollection(
            value = "if control structure without an else",
            stringDefinitions = {
                    @StringDefinitions(
                            expectedLevel = CONSTANT, expectedStrings = "a(b)?"
                    )
            })
    public void ifWithoutElse() {
        StringBuilder sb = new StringBuilder("a");
        int i = new Random().nextInt();
        if (i % 2 == 0) {
            sb.append("b");
        }
        analyzeString(sb.toString());
    }

    @StringDefinitionsCollection(
            value = "case with a nested loop where in the outer loop a StringBuilder is created "
                    + "that is later read",
            stringDefinitions = {
                    @StringDefinitions(
                            expectedLevel = CONSTANT, expectedStrings = "a(b)*"
                    )
            })
    public void nestedLoops(int range) {
        for (int i = 0; i < range; i++) {
            StringBuilder sb = new StringBuilder("a");
            for (int j = 0; j < range * range; j++) {
                sb.append("b");
            }
            analyzeString(sb.toString());
        }
    }

    @StringDefinitionsCollection(
            value = "some example that makes use of a StringBuffer instead of a StringBuilder",
            stringDefinitions = {
                    @StringDefinitions(
                            expectedLevel = PARTIALLY_CONSTANT,
                            expectedStrings = "((x|^-?\\d+$))*yz"
                    )
            })
    public void stringBufferExample() {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < 20; i++) {
            if (i % 2 == 0) {
                sb.append("x");
            } else {
                sb.append(i + 1);
            }
        }
        sb.append("yz");

        analyzeString(sb.toString());
    }

    @StringDefinitionsCollection(
            value = "while-true example",
            stringDefinitions = {
                    @StringDefinitions(
                            expectedLevel = CONSTANT, expectedStrings = "a(b)*"
                    )
            })
    public void whileWithBreak() {
        StringBuilder sb = new StringBuilder("a");
        while (true) {
            sb.append("b");
            if (sb.length() > 100) {
                break;
            }
        }
        analyzeString(sb.toString());
    }

    @StringDefinitionsCollection(
            value = "an example with a non-while-true loop containing a break",
            stringDefinitions = {
                    @StringDefinitions(
                            expectedLevel = CONSTANT, expectedStrings = "a(b)*"
                    )
            })
    public void whileWithBreak(int i) {
        StringBuilder sb = new StringBuilder("a");
        int j = 0;
        while (j < i) {
            sb.append("b");
            if (sb.length() > 100) {
                break;
            }
            j++;
        }
        analyzeString(sb.toString());
    }

    @StringDefinitionsCollection(
            value = "an extensive example with many control structures",
            stringDefinitions = {
                    @StringDefinitions(
                            expectedLevel = CONSTANT, expectedStrings = "(iv1|iv2): "
                    ),
                    @StringDefinitions(
                            expectedLevel = PARTIALLY_CONSTANT,
                            expectedStrings = "(iv1|iv2): ((great!)?)*(.*)?"
                    )
            })
    public void extensive(boolean cond) {
        StringBuilder sb = new StringBuilder();
        if (cond) {
            sb.append("iv1");
        } else {
            sb.append("iv2");
        }
        System.out.println(sb);
        sb.append(": ");

        analyzeString(sb.toString());

        Random random = new Random();
        while (random.nextFloat() > 5.) {
            if (random.nextInt() % 2 == 0) {
                sb.append("great!");
            }
        }

        if (sb.indexOf("great!") > -1) {
            sb.append(getRuntimeClassName());
        }

        analyzeString(sb.toString());
    }

    @StringDefinitionsCollection(
            value = "an example with a throw (and no try-catch-finally)",
            stringDefinitions = {
                    @StringDefinitions(
                            expectedLevel = PARTIALLY_CONSTANT, expectedStrings = "File Content:.*"
                    )
            })
    public void withThrow(String filename) throws IOException {
        StringBuilder sb = new StringBuilder("File Content:");
        String data = new String(Files.readAllBytes(Paths.get(filename)));
        sb.append(data);
        analyzeString(sb.toString());
    }

    @StringDefinitionsCollection(
            value = "case with a try-finally exception",
            // Currently, multiple expectedLevels and expectedStrings values are necessary because
            // the three-address code contains multiple calls to 'analyzeString' which are currently
            // not filtered out
            stringDefinitions = {
                    @StringDefinitions(
                            expectedLevel = PARTIALLY_CONSTANT,
                            expectedStrings = "File Content:(.*)?"
                    ),
                    @StringDefinitions(
                            expectedLevel = PARTIALLY_CONSTANT,
                            expectedStrings = "File Content:(.*)?"
                    ),
                    @StringDefinitions(
                            expectedLevel = PARTIALLY_CONSTANT,
                            expectedStrings = "File Content:(.*)?"
                    )
            })
    public void withException(String filename) {
        StringBuilder sb = new StringBuilder("File Content:");
        try {
            String data = new String(Files.readAllBytes(Paths.get(filename)));
            sb.append(data);
        } catch (Exception ignore) {
        } finally {
            analyzeString(sb.toString());
        }
    }

    @StringDefinitionsCollection(
            value = "case with a try-catch-finally exception",
            stringDefinitions = {
                    @StringDefinitions(
                            expectedLevel = PARTIALLY_CONSTANT, expectedStrings = "=====(.*|=====)"
                    ),
                    @StringDefinitions(
                            expectedLevel = PARTIALLY_CONSTANT, expectedStrings = "=====(.*|=====)"
                    ),
                    @StringDefinitions(
                            expectedLevel = PARTIALLY_CONSTANT, expectedStrings = "=====(.*|=====)"
                    )
            })
    public void tryCatchFinally(String filename) {
        StringBuilder sb = new StringBuilder("=====");
        try {
            String data = new String(Files.readAllBytes(Paths.get(filename)));
            sb.append(data);
        } catch (Exception ignore) {
            sb.append("=====");
        } finally {
            analyzeString(sb.toString());
        }
    }

    @StringDefinitionsCollection(
            value = "case with a try-catch-finally throwable",
            stringDefinitions = {
                    @StringDefinitions(
                            // Due to early stopping finding paths within DefaultPathFinder, the
                            // "EOS" can not be found for the first case (the difference to the case
                            // tryCatchFinally is that a second CatchNode is not present in the
                            // throwable case)
                            expectedLevel = PARTIALLY_CONSTANT, expectedStrings = "BOS:(.*|:EOS)"
                    ),
                    @StringDefinitions(
                            expectedLevel = PARTIALLY_CONSTANT, expectedStrings = "BOS:(.*|:EOS)"
                    ),
                    @StringDefinitions(
                            expectedLevel = PARTIALLY_CONSTANT, expectedStrings = "BOS:(.*|:EOS)"
                    )
            })
    public void tryCatchFinallyWithThrowable(String filename) {
        StringBuilder sb = new StringBuilder("BOS:");
        try {
            String data = new String(Files.readAllBytes(Paths.get(filename)));
            sb.append(data);
        } catch (Throwable t) {
            sb.append(":EOS");
        } finally {
            analyzeString(sb.toString());
        }
    }

    @StringDefinitionsCollection(
            value = "simple examples to clear a StringBuilder",
            stringDefinitions = {
                    @StringDefinitions(
                            expectedLevel = DYNAMIC, expectedStrings = ".*"
                    ),
                    @StringDefinitions(
                            expectedLevel = DYNAMIC, expectedStrings = ".*"
                    )
            })
    public void simpleClearExamples() {
        StringBuilder sb1 = new StringBuilder("init_value:");
        sb1.setLength(0);
        sb1.append(getStringBuilderClassName());

        StringBuilder sb2 = new StringBuilder("init_value:");
        System.out.println(sb2.toString());
        sb2 = new StringBuilder();
        sb2.append(getStringBuilderClassName());

        analyzeString(sb1.toString());
        analyzeString(sb2.toString());
    }

    @StringDefinitionsCollection(
            value = "a more advanced example with a StringBuilder#setLength to clear it",
            stringDefinitions = {
                    @StringDefinitions(
                            expectedLevel = CONSTANT,
                            expectedStrings = "(init_value:Hello, world!Goodbye|Goodbye)"
                    )
            })
    public void advancedClearExampleWithSetLength(int value) {
        StringBuilder sb = new StringBuilder("init_value:");
        if (value < 10) {
            sb.setLength(0);
        } else {
            sb.append("Hello, world!");
        }
        sb.append("Goodbye");
        analyzeString(sb.toString());
    }

    @StringDefinitionsCollection(
            value = "a simple and a little more advanced example with a StringBuilder#replace call",
            stringDefinitions = {
                    @StringDefinitions(
                            expectedLevel = DYNAMIC, expectedStrings = ".*"
                    ),
                    @StringDefinitions(
                            expectedLevel = PARTIALLY_CONSTANT,
                            expectedStrings = "(init_value:Hello, world!Goodbye|.*Goodbye)"
                    )
            })
    public void replaceExamples(int value) {
        StringBuilder sb1 = new StringBuilder("init_value");
        sb1.replace(0, 5, "replaced_value");
        analyzeString(sb1.toString());

        sb1 = new StringBuilder("init_value:");
        if (value < 10) {
            sb1.replace(0, value, "...");
        } else {
            sb1.append("Hello, world!");
        }
        sb1.append("Goodbye");
        analyzeString(sb1.toString());
    }

    @StringDefinitionsCollection(
            value = "loops that use breaks and continues (or both)",
            stringDefinitions = {
                    @StringDefinitions(
                            // The bytecode produces an "if" within an "if" inside the first loop,
                            // => two conds
                            expectedLevel = CONSTANT, expectedStrings = "abc(((d)?)?)*"
                    ),
                    @StringDefinitions(
                            expectedLevel = CONSTANT, expectedStrings = ""
                    ),
                    @StringDefinitions(
                            expectedLevel = DYNAMIC, expectedStrings = "((.*)?)*"
                    )
            })
    public void breakContinueExamples(int value) {
        StringBuilder sb1 = new StringBuilder("abc");
        for (int i = 0; i < value; i++) {
            if (i % 7 == 1) {
                break;
            } else if (i % 3 == 0) {
                continue;
            } else {
                sb1.append("d");
            }
        }
        analyzeString(sb1.toString());

        StringBuilder sb2 = new StringBuilder("");
        for (int i = 0; i < value; i++) {
            if (i % 2 == 0) {
                break;
            }
            sb2.append("some_value");
        }
        analyzeString(sb2.toString());

        StringBuilder sb3 = new StringBuilder();
        for (int i = 0; i < 10; i++) {
            if (sb3.toString().equals("")) {
                // The analysis currently does not detect, that this statement is executed at
                // most / exactly once as it fully relies on the three-address code and does not
                // infer any semantics of conditionals
                sb3.append(getRuntimeClassName());
            } else {
                continue;
            }
        }
        analyzeString(sb3.toString());
    }

    @StringDefinitionsCollection(
            value = "an example where in the condition of an 'if', a string is appended to a "
                    + "StringBuilder",
            stringDefinitions = {
                    @StringDefinitions(
                            expectedLevel = CONSTANT, expectedStrings = "java.lang.Runtime"
                    )
            })
    public void ifConditionAppendsToString(String className) {
        StringBuilder sb = new StringBuilder();
        if (sb.append("java.lang.Runtime").toString().equals(className)) {
            System.out.println("Yep, got the correct class!");
        }
        analyzeString(sb.toString());
    }

    @StringDefinitionsCollection(
            value = "checks if a string value with > 2 continuous appends and a second "
                    + "StringBuilder is determined correctly",
            stringDefinitions = {
                    @StringDefinitions(
                            expectedLevel = CONSTANT, expectedStrings = "B."
                    ),
                    @StringDefinitions(
                            expectedLevel = CONSTANT, expectedStrings = "java.langStringB."
                    )
            })
    public void directAppendConcatsWith2ndStringBuilder() {
        StringBuilder sb = new StringBuilder("java");
        StringBuilder sb2 = new StringBuilder("B");
        sb.append('.').append("lang");
        sb2.append('.');
        sb.append("String");
        sb.append(sb2.toString());
        analyzeString(sb2.toString());
        analyzeString(sb.toString());
    }

    @StringDefinitionsCollection(
            value = "checks if the case, where the value of a StringBuilder depends on the "
                    + "complex construction of a second StringBuilder is determined correctly.",
            stringDefinitions = {
                    @StringDefinitions(
                            expectedLevel = CONSTANT, expectedStrings = "java.lang.(Object|Runtime)"
                    )
            })
    public void secondStringBuilderRead(String className) {
        StringBuilder sbObj = new StringBuilder("Object");
        StringBuilder sbRun = new StringBuilder("Runtime");

        StringBuilder sb1 = new StringBuilder();
        if (sb1.length() == 0) {
            sb1.append(sbObj.toString());
        } else {
            sb1.append(sbRun.toString());
        }

        StringBuilder sb2 = new StringBuilder("java.lang.");
        sb2.append(sb1.toString());
        analyzeString(sb2.toString());
    }

    @StringDefinitionsCollection(
            value = "an example that uses a non final field",
            stringDefinitions = {
                    @StringDefinitions(
                            expectedLevel = PARTIALLY_CONSTANT, expectedStrings = "Field Value:.*"
                    )
            })
    public void nonFinalFieldRead() {
        StringBuilder sb = new StringBuilder("Field Value:");
        System.out.println(sb);
        sb.append(someStringField);
        analyzeString(sb.toString());
    }

    @StringDefinitionsCollection(
            value = "an example that reads a public final static field; for these, the string "
                    + "information are available (at lease on modern compilers)",
            stringDefinitions = {
                    @StringDefinitions(
                            expectedLevel = CONSTANT, expectedStrings = "Field Value:mine"
                    )
            })
    public void finalFieldRead() {
        StringBuilder sb = new StringBuilder("Field Value:");
        System.out.println(sb);
        sb.append(MY_CONSTANT);
        analyzeString(sb.toString());
    }

    @StringDefinitionsCollection(
            value = "A case with a criss-cross append on two StringBuilders",
            stringDefinitions = {
                    @StringDefinitions(
                            expectedLevel = CONSTANT, expectedStrings = "Object(Runtime)?"
                    ),
                    @StringDefinitions(
                            expectedLevel = CONSTANT, expectedStrings = "Runtime(Object)?"
                    )
            })
    public void crissCrossExample(String className) {
        StringBuilder sbObj = new StringBuilder("Object");
        StringBuilder sbRun = new StringBuilder("Runtime");

        if (className.length() == 0) {
            sbRun.append(sbObj.toString());
        } else {
            sbObj.append(sbRun.toString());
        }

        analyzeString(sbObj.toString());
        analyzeString(sbRun.toString());
    }

    @StringDefinitionsCollection(
            value = "examples that use a passed parameter to define strings that are analyzed",
            stringDefinitions = {
                    @StringDefinitions(
                            expectedLevel = DYNAMIC, expectedStrings = ".*"
                    ),
                    @StringDefinitions(
                            expectedLevel = DYNAMIC, expectedStrings = ".*"
                    ),
                    @StringDefinitions(
                            expectedLevel = PARTIALLY_CONSTANT, expectedStrings = "value=.*"
                    ),
                    @StringDefinitions(
                            expectedLevel = PARTIALLY_CONSTANT, expectedStrings = "value=.*.*"
                    )
            })
    public void parameterRead(String stringValue, StringBuilder sbValue) {
        analyzeString(stringValue);
        analyzeString(sbValue.toString());

        StringBuilder sb = new StringBuilder("value=");
        System.out.println(sb.toString());
        sb.append(stringValue);
        analyzeString(sb.toString());

        sb.append(sbValue.toString());
        analyzeString(sb.toString());
    }

    @StringDefinitionsCollection(
            value = "an example extracted from "
                    + "com.oracle.webservices.internal.api.message.BasePropertySet with two "
                    + "definition sites and one usage site",
            stringDefinitions = {
                    @StringDefinitions(
                            expectedLevel = PARTIALLY_CONSTANT,
                            expectedStrings = "(set.*|s.*)"
                    ),
            })
    public void twoDefinitionsOneUsage(String getName) throws ClassNotFoundException {
        String name = getName;
        String setName = name.startsWith("is") ?
                "set" + name.substring(2) :
                's' + name.substring(1);

        Class clazz = Class.forName("java.lang.MyClass");
        Method setter;
        try {
            setter = clazz.getMethod(setName);
            analyzeString(setName);
        } catch (NoSuchMethodException var15) {
            setter = null;
            System.out.println("Error occurred");
        }
    }

    @StringDefinitionsCollection(
            value = "Some comprehensive example for experimental purposes taken from the JDK and " +
                    "slightly modified",
            stringDefinitions = {
                    @StringDefinitions(
                            expectedLevel = PARTIALLY_CONSTANT,
                            expectedStrings = "Hello: (.*|.*|.*)?"
                    ),
            })
    protected void setDebugFlags(String[] var1) {
        for(int var2 = 0; var2 < var1.length; ++var2) {
            String var3 = var1[var2];

            int randomValue = new Random().nextInt();
            StringBuilder sb = new StringBuilder("Hello: ");
            if (randomValue % 2 == 0) {
                sb.append(getRuntimeClassName());
            } else if (randomValue % 3 == 0) {
                sb.append(getStringBuilderClassName());
            } else if (randomValue % 4 == 0) {
                sb.append(getSimpleStringBuilderClassName());
            }

            try {
                Field var4 = this.getClass().getField(var3 + "DebugFlag");
                int var5 = var4.getModifiers();
                if (Modifier.isPublic(var5) && !Modifier.isStatic(var5) &&
                        var4.getType() == Boolean.TYPE) {
                    var4.setBoolean(this, true);
                }
            } catch (IndexOutOfBoundsException var90) {
                System.out.println("Should never happen!");
            } catch (Exception var6) {
                int i = 10;
                i += new Random().nextInt();
                System.out.println("Some severe error occurred!" + i);
            } finally {
                int i = 10;
                i += new Random().nextInt();
                // TODO: Control structures in finally blocks are not handles correctly
                // if (i % 2 == 0) {
                //     System.out.println("Ready to analyze now in any case!" + i);
                // }
            }

            analyzeString(sb.toString());
        }
    }

    @StringDefinitionsCollection(
            value = "an example with an unknown character read",
            stringDefinitions = {
                    @StringDefinitions(expectedLevel = DYNAMIC, expectedStrings = ".*"),
                    @StringDefinitions(expectedLevel = DYNAMIC, expectedStrings = ".*"),
            })
    public void unknownCharValue() {
        int charCode = new Random().nextInt(200);
        char c = (char) charCode;
        String s = String.valueOf(c);
        analyzeString(s);

        StringBuilder sb = new StringBuilder();
        sb.append(c);
        analyzeString(sb.toString());
    }

    //    @StringDefinitions(
    //            value = "a case with a switch with missing breaks",
    //            expectedLevel = StringConstancyLevel.CONSTANT},
    //            expectedStrings ={ "a(bc|c)?" }
    //    )
    //    public void switchWithMissingBreak(int value) {
    //        StringBuilder sb = new StringBuilder("a");
    //        switch (value) {
    //        case 0:
    //            sb.append("b");
    //        case 1:
    //            sb.append("c");
    //            break;
    //        case 2:
    //            break;
    //        }
    //        analyzeString(sb.toString());
    //    }

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

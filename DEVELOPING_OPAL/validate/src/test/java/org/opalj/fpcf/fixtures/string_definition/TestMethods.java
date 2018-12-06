/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.string_definition;

import org.opalj.fpcf.properties.string_definition.StringDefinitions;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Random;

import static org.opalj.fpcf.properties.string_definition.StringConstancyLevel.*;

/**
 * This file contains various tests for the StringDefinitionAnalysis. The following things are to be
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
 * </ul>
 * <p>
 * Thus, you should avoid the following characters / strings to occur in "expectedStrings":
 * {*, ?, \w, |}. In the future, "expectedStrings" might be parsed back into a StringTree. Thus, to
 * be on the safe side, brackets should be avoided as well.
 *
 * @author Patrick Mell
 */
public class TestMethods {

    /**
     * This method represents the test method which is serves as the trigger point for the
     * {@link org.opalj.fpcf.LocalStringDefinitionTest} to know which string read operation to
     * analyze.
     * Note that the {@link StringDefinitions} annotation is designed in a way to be able to capture
     * only one read operation. For how to get around this limitation, see the annotation.
     *
     * @param s Some string which is to be analyzed.
     */
    public void analyzeString(String s) {
    }

    @StringDefinitions(
            value = "read-only string variable, trivial case",
            expectedLevels = { CONSTANT, CONSTANT },
            expectedStrings = { "java.lang.String", "java.lang.String" }
    )
    public void constantStringReads() {
        analyzeString("java.lang.String");

        String className = "java.lang.String";
        analyzeString(className);
    }

    @StringDefinitions(
            value = "checks if a string value with append(s) is determined correctly",
            expectedLevels = { CONSTANT, CONSTANT },
            expectedStrings = { "java.lang.String", "java.lang.Object" }
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

    @StringDefinitions(
            value = "checks if a string value with > 2 continuous appends is determined correctly",
            expectedLevels = { CONSTANT },
            expectedStrings = { "java.lang.String" }
    )
    public void directAppendConcats() {
        StringBuilder sb = new StringBuilder("java");
        sb.append(".").append("lang").append(".").append("String");
        analyzeString(sb.toString());
    }

    @StringDefinitions(
            value = "at this point, function call cannot be handled => DYNAMIC",
            expectedLevels = { DYNAMIC },
            expectedStrings = { "\\w" }
    )
    public void fromFunctionCall() {
        String className = getStringBuilderClassName();
        analyzeString(className);
    }

    @StringDefinitions(
            value = "constant string + string from function call => PARTIALLY_CONSTANT",
            expectedLevels = { PARTIALLY_CONSTANT },
            expectedStrings = { "java.lang.\\w" }
    )
    public void fromConstantAndFunctionCall() {
        String className = "java.lang.";
        System.out.println(className);
        className += getSimpleStringBuilderClassName();
        analyzeString(className);
    }

    @StringDefinitions(
            value = "array access with unknown index",
            expectedLevels = { CONSTANT },
            expectedStrings = { "(java.lang.String|java.lang.StringBuilder|"
                    + "java.lang.System|java.lang.Runnable)" }
    )
    public void fromStringArray(int index) {
        String[] classes = {
                "java.lang.String", "java.lang.StringBuilder",
                "java.lang.System", "java.lang.Runnable"
        };
        if (index >= 0 && index < classes.length) {
            analyzeString(classes[index]);
        }
    }

    @StringDefinitions(
            value = "a simple case where multiple definition sites have to be considered",
            expectedLevels = { CONSTANT },
            expectedStrings = { "(java.lang.System|java.lang.Runtime)" }
    )
    public void multipleConstantDefSites(boolean cond) {
        String s;
        if (cond) {
            s = "java.lang.System";
        } else {
            s = "java.lang.Runtime";
        }
        analyzeString(s);
    }

    @StringDefinitions(
            value = "a more comprehensive case where multiple definition sites have to be "
                    + "considered each with a different string generation mechanism",
            expectedLevels = { DYNAMIC },
            expectedStrings = { "(java.lang.Object|\\w|java.lang.System|java.lang.\\w|\\w)" }
    )
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

    @StringDefinitions(
            value = "a case where multiple optional definition sites have to be considered.",
            expectedLevels = { CONSTANT },
            expectedStrings = { "a(b|c)?" }
    )
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

    @StringDefinitions(
            value = "if-else control structure which append to a string builder with an int expr "
                    + "and an int",
            expectedLevels = { DYNAMIC, DYNAMIC },
            expectedStrings = { "(x|[AnIntegerValue])", "([AnIntegerValue]|x)" }
    )
    public void ifElseWithStringBuilderWithIntExpr() {
        StringBuilder sb1 = new StringBuilder();
        StringBuilder sb2 = new StringBuilder();
        int i = new Random().nextInt();
        if (i % 2 == 0) {
            sb1.append("x");
            sb2.append(i);
        } else {
            sb1.append(i + 1);
            sb2.append("x");
        }
        analyzeString(sb1.toString());
        analyzeString(sb2.toString());
    }

    @StringDefinitions(
            value = "if-else control structure which append to a string builder",
            expectedLevels = { CONSTANT, CONSTANT },
            expectedStrings = { "(a|b)", "a(b|c)" }
    )
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

    @StringDefinitions(
            value = "if-else control structure which append to a string builder multiple times",
            expectedLevels = { CONSTANT },
            expectedStrings = { "a(bcd|xyz)" }
    )
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

    @StringDefinitions(
            value = "simple for loops with known and unknown bounds",
            expectedLevels = { CONSTANT, CONSTANT },
            // Currently, the analysis does not support determining loop ranges => a(b)*
            expectedStrings = { "a(b)*", "a(b)*" }
    )
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

    @StringDefinitions(
            value = "if-else control structure within a for loop and with an append afterwards",
            expectedLevels = { PARTIALLY_CONSTANT },
            expectedStrings = { "((x|[AnIntegerValue]))*yz" }
    )
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

    @StringDefinitions(
            value = "if control structure without an else",
            expectedLevels = { CONSTANT },
            expectedStrings = { "a(b)?" }
    )
    public void ifWithoutElse() {
        StringBuilder sb = new StringBuilder("a");
        int i = new Random().nextInt();
        if (i % 2 == 0) {
            sb.append("b");
        }
        analyzeString(sb.toString());
    }

    @StringDefinitions(
            value = "case with a nested loop where in the outer loop a StringBuilder is created "
                    + "that is later read",
            expectedLevels = { CONSTANT },
            expectedStrings = { "a(b)*" }
    )
    public void nestedLoops(int range) {
        for (int i = 0; i < range; i++) {
            StringBuilder sb = new StringBuilder("a");
            for (int j = 0; j < range * range; j++) {
                sb.append("b");
            }
            analyzeString(sb.toString());
        }
    }

    @StringDefinitions(
            value = "some example that makes use of a StringBuffer instead of a StringBuilder",
            expectedLevels = { PARTIALLY_CONSTANT },
            expectedStrings = { "((x|[AnIntegerValue]))*yz" }
    )
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

    @StringDefinitions(
            value = "while-true example",
            expectedLevels = { CONSTANT },
            expectedStrings = { "a(b)*" }
    )
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

    @StringDefinitions(
            value = "an example with a non-while-true loop containing a break",
            expectedLevels = { CONSTANT },
            expectedStrings = { "a(b)*" }
    )
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

    @StringDefinitions(
            value = "an extensive example with many control structures",
            expectedLevels = { CONSTANT, PARTIALLY_CONSTANT },
            expectedStrings = { "(iv1|iv2): ", "(iv1|iv2): (great!)*(\\w)?" }
    )
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

    @StringDefinitions(
            value = "an example with a throw (and no try-catch-finally)",
            expectedLevels = { PARTIALLY_CONSTANT },
            expectedStrings = { "File Content:\\w" }
    )
    public void withThrow(String filename) throws IOException {
        StringBuilder sb = new StringBuilder("File Content:");
        String data = new String(Files.readAllBytes(Paths.get(filename)));
        sb.append(data);
        analyzeString(sb.toString());
    }

    @StringDefinitions(
            value = "case with a try-finally exception",
            // Currently, multiple expectedLevels and expectedStrings values are necessary because
            // the three-address code contains multiple calls to 'analyzeString' which are currently
            // not filtered out
            expectedLevels = {
                    PARTIALLY_CONSTANT, PARTIALLY_CONSTANT, PARTIALLY_CONSTANT
            },
            expectedStrings = {
                    "File Content:(\\w)?", "File Content:(\\w)?", "File Content:(\\w)?"
            }
    )
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

    @StringDefinitions(
            value = "case with a try-catch-finally exception",
            expectedLevels = {
                    PARTIALLY_CONSTANT, PARTIALLY_CONSTANT, PARTIALLY_CONSTANT
            },
            expectedStrings = {
                    "=====(\\w|=====)", "=====(\\w|=====)", "=====(\\w|=====)"
            }
    )
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

    @StringDefinitions(
            value = "simple examples to clear a StringBuilder",
            expectedLevels = { DYNAMIC, DYNAMIC },
            expectedStrings = { "\\w", "\\w" }
    )
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

    @StringDefinitions(
            value = "a more advanced example with a StringBuilder#setLength to clear it",
            expectedLevels = { CONSTANT },
            expectedStrings = { "(init_value:Hello, world!Goodbye|Goodbye)" }
    )
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

    @StringDefinitions(
            value = "a simple and a little more advanced example with a StringBuilder#replace call",
            expectedLevels = { DYNAMIC, PARTIALLY_CONSTANT },
            expectedStrings = { "\\w", "(init_value:Hello, world!Goodbye|\\wGoodbye)" }
    )
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

    //    @StringDefinitions(
    //            value = "a case with a switch with missing breaks",
    //            expectedLevels = {StringConstancyLevel.CONSTANT},
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
    //    //    @StringDefinitions(
    //    //            value = "checks if a string value with > 2 continuous appends and a second "
    //    //                    + "StringBuilder is determined correctly",
    //    //            expectedLevels = {StringConstancyLevel.CONSTANT},
    //    //            expectedStrings ={ "java.langStringB." }
    //    //    )
    //    //    public void directAppendConcats2() {
    //    //        StringBuilder sb = new StringBuilder("java");
    //    //        StringBuilder sb2 = new StringBuilder("B");
    //    //        sb.append(".").append("lang");
    //    //        sb2.append(".");
    //    //        sb.append("String");
    //    //        sb.append(sb2.toString());
    //    //        analyzeString(sb.toString());

    //    //    }

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

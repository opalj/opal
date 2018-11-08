/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.string_definition;

import org.opalj.fpcf.properties.string_definition.StringConstancyLevel;
import org.opalj.fpcf.properties.string_definition.StringDefinitions;

import java.util.Random;

/**
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

    // The following is a strange case (difficult / impossible? to follow back information flow)
    //    @StringDefinitions(
    //            value = "checks if a string value with > 1 continuous appends is determined correctly",
    //            expectedLevel = StringConstancyLevel.CONSTANT,
    //            expectedStrings = "java.lang.String"
    //    )
    //    public void directAppendConcat() {
    //        StringBuilder sb = new StringBuilder("java");
    //        sb.append(".").append("lang").append(".").append("String");
    //        analyzeString(sb.toString());
    //    }

    @StringDefinitions(
            value = "read-only string, trivial case",
            expectedLevel = StringConstancyLevel.CONSTANT,
            expectedStrings = "java.lang.String"
    )
    public void constantString() {
        analyzeString("java.lang.String");
    }

    @StringDefinitions(
            value = "read-only string variable, trivial case",
            expectedLevel = StringConstancyLevel.CONSTANT,
            expectedStrings = "java.lang.String"
    )
    public void constantStringVariable() {
        String className = "java.lang.String";
        analyzeString(className);
    }

    @StringDefinitions(
            value = "checks if a string value with one append is determined correctly",
            expectedLevel = StringConstancyLevel.CONSTANT,
            expectedStrings = "java.lang.string"
    )
    public void simpleStringConcat() {
        String className = "java.lang.";
        System.out.println(className);
        className += "string";
        analyzeString(className);
    }

    @StringDefinitions(
            value = "checks if a string value with > 1 appends is determined correctly",
            expectedLevel = StringConstancyLevel.CONSTANT,
            expectedStrings = "java.lang.string"
    )
    public void advStringConcat() {
        String className = "java.";
        System.out.println(className);
        className += "lang.";
        System.out.println(className);
        className += "string";
        analyzeString(className);
    }

    @StringDefinitions(
            value = "at this point, function call cannot be handled => DYNAMIC",
            expectedLevel = StringConstancyLevel.DYNAMIC,
            expectedStrings = "*"
    )
    public void fromFunctionCall() {
        String className = getStringBuilderClassName();
        analyzeString(className);
    }

    @StringDefinitions(
            value = "constant string + string from function call => PARTIALLY_CONSTANT",
            expectedLevel = StringConstancyLevel.PARTIALLY_CONSTANT,
            expectedStrings = "java.lang.*"
    )
    public void fromConstantAndFunctionCall() {
        String className = "java.lang.";
        System.out.println(className);
        className += getSimpleStringBuilderClassName();
        analyzeString(className);
    }

    @StringDefinitions(
            value = "array access with unknown index",
            expectedLevel = StringConstancyLevel.CONSTANT,
            expectedStrings = "(java.lang.String | java.lang.System | "
                    + "java.lang.Runnable | java.lang.StringBuilder)"
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
            expectedLevel = StringConstancyLevel.CONSTANT,
            expectedStrings = "(java.lang.System | java.lang.Runtime)"
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
            expectedLevel = StringConstancyLevel.DYNAMIC,
            expectedStrings = "(java.lang.System | java.lang.* | * | java.lang.Object)"
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
            value = "if-else control structure which append to a string builder with an int expr",
            expectedLevel = StringConstancyLevel.DYNAMIC,
            expectedStrings = "(x | [AnIntegerValue])"
    )
    public void ifElseWithStringBuilderWithIntExpr() {
        StringBuilder sb = new StringBuilder();
        int i = new Random().nextInt();
        if (i % 2 == 0) {
            sb.append("x");
        } else {
            sb.append(i + 1);
        }
        analyzeString(sb.toString());
    }

    @StringDefinitions(
            value = "if-else control structure which append to a string builder with an int",
            expectedLevel = StringConstancyLevel.DYNAMIC,
            expectedStrings = "(x | [AnIntegerValue])"
    )
    public void ifElseWithStringBuilderWithConstantInt() {
        StringBuilder sb = new StringBuilder();
        int i = new Random().nextInt();
        if (i % 2 == 0) {
            sb.append("x");
        } else {
            sb.append(i);
        }
        analyzeString(sb.toString());
    }

    @StringDefinitions(
            value = "if-else control structure which append to a string builder",
            expectedLevel = StringConstancyLevel.CONSTANT,
            expectedStrings = "(b | a)"
    )
    public void ifElseWithStringBuilder1() {
        StringBuilder sb;
        int i = new Random().nextInt();
        if (i % 2 == 0) {
            sb = new StringBuilder("a");
        } else {
            sb = new StringBuilder("b");
        }
        analyzeString(sb.toString());
    }

    @StringDefinitions(
            value = "if-else control structure which append to a string builder",
            expectedLevel = StringConstancyLevel.CONSTANT,
            expectedStrings = "a(b | c)"
    )
    public void ifElseWithStringBuilder2() {
        StringBuilder sb = new StringBuilder("a");
        int i = new Random().nextInt();
        if (i % 2 == 0) {
            sb.append("b");
        } else {
            sb.append("c");
        }
        analyzeString(sb.toString());
    }

    //    @StringDefinitions(
    //            value = "if-else control structure within a for loop with known loop bounds",
    //            expectedLevel = StringConstancyLevel.PARTIALLY_CONSTANT,
    //            expectedValues = { "(\"x\" | [Int Value])^20" }
    //    )
    //    public void ifElseInLoopWithKnownBounds() {
    //        StringBuilder sb = new StringBuilder();
    //        for (int i = 0; i < 20; i++) {
    //            if (i % 2 == 0) {
    //                sb.append("x");
    //            } else {
    //                sb.append(i + 1);
    //            }
    //        }
    //
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

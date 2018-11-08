/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.string_definition;

import org.opalj.fpcf.properties.string_definition.StringConstancyLevel;
import org.opalj.fpcf.properties.string_definition.StringDefinitions;

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

    @StringDefinitions(
            value = "read-only string, trivial case",
            expectedLevel = StringConstancyLevel.CONSTANT,
            expectedValues = { "java.lang.String" }
    )
    public void constantString() {
        String className = "java.lang.String";
        analyzeString(className);
    }

    @StringDefinitions(
            value = "checks if the string value for the *forName* call is correctly determined",
            expectedLevel = StringConstancyLevel.CONSTANT,
            expectedValues = { "java.lang.string" }
    )
    public void stringConcatenation() {
        String className = "java.lang.";
        System.out.println(className);
        className += "string";
        analyzeString(className);
    }

    @StringDefinitions(
            value = "at this point, function call cannot be handled => DYNAMIC",
            expectedLevel = StringConstancyLevel.DYNAMIC,
            expectedValues = { "*" }
    )
    public void fromFunctionCall() {
        String className = getStringBuilderClassName();
        analyzeString(className);
    }

    @StringDefinitions(
            value = "constant string + string from function call => PARTIALLY_CONSTANT",
            expectedLevel = StringConstancyLevel.PARTIALLY_CONSTANT,
            expectedValues = { "java.lang.*" }
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
            expectedValues = {
                    "java.lang.String", "java.lang.StringBuilder",
                    "java.lang.System", "java.lang.Runnable"
            }
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
            expectedValues = { "java.lang.System", "java.lang.Runtime" }
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
            expectedValues = { "java.lang.Object", "*", "java.lang.System", "java.lang.*" }
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

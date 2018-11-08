/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.string_definition;

import org.opalj.fpcf.properties.string_definition.StringConstancyLevel;
import org.opalj.fpcf.properties.string_definition.StringDefinitions;

/**
 * @author Patrick Mell
 */
public class TestMethods {

    @StringDefinitions(
            value = "read-only string, trivial case",
            expectedLevel = StringConstancyLevel.CONSTANT,
            expectedValues = { "java.lang.String" },
            pc = 4
    )
    public void constantString() {
        String className = "java.lang.String";
        try {
            Class.forName(className);
        } catch (ClassNotFoundException ignored) {
        }
    }

    @StringDefinitions(
            value = "checks if the string value for the *forName* call is correctly determined",
            expectedLevel = StringConstancyLevel.CONSTANT,
            expectedValues = { "java.lang.string" },
            pc = 31
    )
    public void stringConcatenation() {
        String className = "java.lang.";
        System.out.println(className);
        className += "string";
        try {
            Class.forName(className);
        } catch (ClassNotFoundException ignored) {
        }
    }

    @StringDefinitions(
            value = "at this point, function call cannot be handled => DYNAMIC",
            expectedLevel = StringConstancyLevel.DYNAMIC,
            expectedValues = { "*" },
            pc = 6
    )
    public void fromFunctionCall() {
        String className = getStringBuilderClassName();
        try {
            Class.forName(className);
        } catch (ClassNotFoundException ignored) {
        }
    }

    @StringDefinitions(
            value = "constant string + string from function call => PARTIALLY_CONSTANT",
            expectedLevel = StringConstancyLevel.PARTIALLY_CONSTANT,
            expectedValues = { "java.lang.*" },
            pc = 33
    )
    public void fromConstantAndFunctionCall() {
        String className = "java.lang.";
        System.out.println(className);
        className += getSimpleStringBuilderClassName();
        try {
            Class.forName(className);
        } catch (ClassNotFoundException ignored) {
        }
    }

    @StringDefinitions(
            value = "array access with unknown index",
            expectedLevel = StringConstancyLevel.CONSTANT,
            expectedValues = {
                    "java.lang.String", "java.lang.StringBuilder",
                    "java.lang.System", "java.lang.Runnable"
            }, pc = 38
    )
    public void fromStringArray(int index) {
        String[] classes = {
                "java.lang.String", "java.lang.StringBuilder",
                "java.lang.System", "java.lang.Runnable"
        };
        if (index >= 0 && index < classes.length) {
            try {
                Class.forName(classes[index]);
            } catch (ClassNotFoundException ignored) {
            }
        }
    }

    private String getStringBuilderClassName() {
        return "java.lang.StringBuilder";
    }

    private String getSimpleStringBuilderClassName() {
        return "StringBuilder";
    }

}

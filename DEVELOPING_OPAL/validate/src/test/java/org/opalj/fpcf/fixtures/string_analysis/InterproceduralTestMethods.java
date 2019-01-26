/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.string_analysis;

import org.opalj.fpcf.properties.string_analysis.StringDefinitions;
import org.opalj.fpcf.properties.string_analysis.StringDefinitionsCollection;

import static org.opalj.fpcf.properties.string_analysis.StringConstancyLevel.DYNAMIC;

/**
 * This file contains various tests for the InterproceduralStringAnalysis. For further information
 * on what to consider, please see {@link LocalTestMethods}
 *
 * @author Patrick Mell
 */
public class InterproceduralTestMethods {

    private String someStringField = "";
    public static final String MY_CONSTANT = "mine";

    /**
     * This method represents the test method which is serves as the trigger point for the
     * {@link org.opalj.fpcf.LocalStringAnalysisTest} to know which string read operation to
     * analyze.
     * Note that the {@link StringDefinitions} annotation is designed in a way to be able to capture
     * only one read operation. For how to get around this limitation, see the annotation.
     *
     * @param s Some string which is to be analyzed.
     */
    public void analyzeString(String s) {
    }

    @StringDefinitionsCollection(
            value = "at this point, function call cannot be handled => DYNAMIC",
            stringDefinitions = {
                    @StringDefinitions(
                            expectedLevel = DYNAMIC, expectedStrings = "\\w"
                    )
            })
    public void fromFunctionCall() {
        String className = getStringBuilderClassName();
        analyzeString(className);
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

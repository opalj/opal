/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.string;

import org.opalj.fpcf.properties.string_analysis.Constant;
import org.opalj.fpcf.properties.string_analysis.Failure;
import org.opalj.fpcf.properties.string_analysis.Level;

/**
 * Tests the integration with the system properties FPCF property.
 *
 * @see SimpleStringOps
 */
public class SystemProperties {

    /**
     * Serves as the sink for string variables to be analyzed.
     */
    public void analyzeString(String s) {}

    @Constant(n = 0, levels = Level.TRUTH, value = "some.test.value")
    @Failure(n = 0, levels = Level.L0)
    public void systemPropertiesIntegrationTest() {
        System.setProperty("some.test.property", "some.test.value");
        String s = System.getProperty("some.test.property");
        analyzeString(s);
    }
}

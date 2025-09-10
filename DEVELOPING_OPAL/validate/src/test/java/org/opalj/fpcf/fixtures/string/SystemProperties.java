/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.string;

import org.opalj.fpcf.properties.string.Constant;
import org.opalj.fpcf.properties.string.Failure;
import org.opalj.fpcf.properties.string.Level;

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

    @Constant(sinkIndex = 0, levels = Level.TRUTH, value = "some.test.value")
    @Failure(sinkIndex = 0, levels = Level.L0)
    public void systemPropertiesIntegrationTest() {
        System.setProperty("some.test.property", "some.test.value");
        String s = System.getProperty("some.test.property");
        analyzeString(s);
    }
}

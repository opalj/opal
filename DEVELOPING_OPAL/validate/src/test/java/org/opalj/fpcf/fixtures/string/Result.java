/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.string;

import org.opalj.fpcf.properties.string.*;

/**
 * Tests compatibility of the results of the string analysis with e.g. being compiled to a regex string.
 *
 * @see SimpleStringOps
 */
public class Result {

    /**
     * Serves as the sink for string variables to be analyzed.
     */
    public void analyzeString(String s) {}

    @Constant(sinkIndex = 0, levels = Level.TRUTH, value = "\\[B")
    @Constant(sinkIndex = 1, levels = Level.TRUTH, value = "\\[Ljava.lang.String;")
    @Constant(sinkIndex = 2, levels = Level.TRUTH, value = "\\[\\[Lsun.security.pkcs.SignerInfo;")
    @Constant(sinkIndex = 3, levels = Level.TRUTH, value = "US\\$")
    @Constant(sinkIndex = 4, levels = Level.TRUTH, value = "US\\\\")
    public void regexCompilableTest() {
        analyzeString("[B");
        analyzeString("[Ljava.lang.String;");
        analyzeString("[[Lsun.security.pkcs.SignerInfo;");
        analyzeString("US$");
        analyzeString("US\\");
    }
}

/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.string_analysis;

import org.opalj.fpcf.fixtures.string_analysis.tools.GreetingService;
import org.opalj.fpcf.fixtures.string_analysis.tools.HelloGreeting;
import org.opalj.fpcf.properties.string_analysis.*;

/**
 * @see SimpleStringOps
 */
public class Integration {

    /**
     * Serves as the sink for string variables to be analyzed.
     */
    public void analyzeString(String s) {}

    @Constant(n = 0, levels = Level.TRUTH, value = "java.lang.String")
    public void noCallersInformationRequiredTest(String s) {
        System.out.println(s);
        analyzeString("java.lang.String");
    }

    @Constant(n = 0, levels = Level.TRUTH, value = "some.test.value")
    @Failure(n = 0, levels = Level.L0)
    public void systemPropertiesIntegrationTest() {
        System.setProperty("some.test.property", "some.test.value");
        String s = System.getProperty("some.test.property");
        analyzeString(s);
    }

    @Constant(n = 0, levels = Level.TRUTH, value = "Hello World")
    @Failure(n = 0, levels = { Level.L0, Level.L1 })
    public void knownHierarchyInstanceTest() {
        GreetingService gs = new HelloGreeting();
        analyzeString(gs.getGreeting("World"));
    }

    @Constant(n = 0, levels = Level.TRUTH, value = "(Hello|Hello World)")
    @Failure(n = 0, levels = { Level.L0, Level.L1 })
    public void unknownHierarchyInstanceTest(GreetingService greetingService) {
        analyzeString(greetingService.getGreeting("World"));
    }

    @Constant(n = 0, levels = Level.TRUTH, value = "\\[B")
    @Constant(n = 1, levels = Level.TRUTH, value = "\\[Ljava.lang.String;")
    @Constant(n = 2, levels = Level.TRUTH, value = "\\[\\[Lsun.security.pkcs.SignerInfo;")
    @Constant(n = 3, levels = Level.TRUTH, value = "US\\$")
    @Constant(n = 4, levels = Level.TRUTH, value = "US\\\\")
    public void regexCompilableTest() {
        analyzeString("[B");
        analyzeString("[Ljava.lang.String;");
        analyzeString("[[Lsun.security.pkcs.SignerInfo;");
        analyzeString("US$");
        analyzeString("US\\");
    }
}

/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.string_analysis.hierarchies;

public class SimpleHelloGreeting implements GreetingService {

    @Override
    public String getGreeting(String name) {
        return "Hello";
    }

}

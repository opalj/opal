/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.string.tools;

public class ParameterDependentStringFactory implements StringFactory {

    @Override
    public String getString(String parameter) {
        return "Hello " + parameter;
    }

}

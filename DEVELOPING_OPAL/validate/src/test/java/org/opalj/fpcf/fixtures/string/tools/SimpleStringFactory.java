/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.string.tools;

public class SimpleStringFactory implements StringFactory {

    @Override
    public String getString(String parameter) {
        return "Hello";
    }

}

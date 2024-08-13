/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.xl.js.controlflow.intraprocedural.unidirectional;

import org.opalj.fpcf.properties.xl.JSEnvironment;
import org.opalj.fpcf.properties.xl.JSEnvironmentBinding;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

public class JavaScriptAllocationNoPutNoGet {
    @JSEnvironment(
            bindings = {
                    @JSEnvironmentBinding(identifier = "y",
                            value = "Undef|\"test\"")
            }
    )
    public static void main(String args[]) throws ScriptException, NoSuchMethodException {
        ScriptEngineManager sem = new ScriptEngineManager();
        ScriptEngine se = sem.getEngineByName("JavaScript");
        se.eval("var x = 'test'; var y = x;");
    }
}

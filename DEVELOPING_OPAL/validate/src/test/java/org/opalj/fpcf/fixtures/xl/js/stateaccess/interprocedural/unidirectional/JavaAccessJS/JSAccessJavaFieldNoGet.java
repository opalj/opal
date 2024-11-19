/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.xl.js.stateaccess.interprocedural.unidirectional.JavaAccessJS;

import org.opalj.fpcf.properties.xl.JSEnvironment;
import org.opalj.fpcf.properties.xl.JSEnvironmentBinding;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

/**
 * set modify JS "state" using ScriptEngine.put (without get)
 * annotation checks TAJS environment
 */
public class JSAccessJavaFieldNoGet {
    @JSEnvironment(
            bindings = {
                    @JSEnvironmentBinding(identifier = "instance",
                    value = "JavaObject[org.opalj.fpcf.fixtures.xl.js.stateaccess.interprocedural.unidirectional.JavaAccessJS.JSAccessJavaFieldNoGet]node: -1<no value>")
            }
    )
    public static void main(String args[]) throws ScriptException, NoSuchMethodException {
        ScriptEngineManager sem = new ScriptEngineManager();
        ScriptEngine se = sem.getEngineByName("JavaScript");
        putinstance(se);
        se.eval("var n = {'a':'b'}; var x = instance;");
    }

    private static void putinstance(ScriptEngine se) {
        JSAccessJavaFieldNoGet instance = new JSAccessJavaFieldNoGet();
        se.put("instance", instance);
    }


}

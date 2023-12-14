package org.opalj.fpcf.fixtures.xl.js.stateaccess.intraprocedural.unidirectional.JavaAccessJS;

import org.opalj.fpcf.properties.xl.TAJSEnvironment;
import org.opalj.fpcf.properties.xl.TAJSEnvironmentBinding;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

/**
 * set modify JS "state" using ScriptEngine.put (without get)
 * annotation checks TAJS environment
 */
public class PutInstanceNoGet {
    @TAJSEnvironment(
            bindings = {
                    @TAJSEnvironmentBinding(identifier = "instance",
                    value = "JavaObject[org.opalj.fpcf.fixtures.xl.js.stateaccess.intraprocedural.unidirectional.JavaAccessJS.PutInstanceNoGet]node: -1<no value>")
            }
    )
    public static void main(String args[]) throws ScriptException, NoSuchMethodException {
        PutInstanceNoGet instance = new PutInstanceNoGet();
        ScriptEngineManager sem = new ScriptEngineManager();
        ScriptEngine se = sem.getEngineByName("JavaScript");
        se.put("instance", instance);
        se.eval("var n = {'a':'b'}; var x = instance;");
    }

}

package org.opalj.fpcf.fixtures.xl.js.stateaccess.intraprocedural.unidirectional.JSAccessJava;

import org.opalj.fpcf.properties.xl.TAJSEnvironment;
import org.opalj.fpcf.properties.xl.TAJSEnvironmentBinding;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

/**
 * set modify JS "state" using ScriptEngine.put (without get)
 * annotation checks TAJS environment
 */
public class JSAccessJavaFieldNoGet {
    @TAJSEnvironment(
            bindings = {
                    @TAJSEnvironmentBinding(identifier = "instance",
                    value = "JavaObject[org.opalj.fpcf.fixtures.xl.js.stateaccess.intraprocedural.unidirectional.JavaAccessJS.JavaAcccessJSObject]node: -1<no value>")
            }
    )
    public static void main(String args[]) throws ScriptException, NoSuchMethodException {
        JSAccessJavaFieldNoGet instance = new JSAccessJavaFieldNoGet();
        ScriptEngineManager sem = new ScriptEngineManager();
        ScriptEngine se = sem.getEngineByName("JavaScript");
        se.put("instance", instance);
        se.eval("var n = {'a':'b'}; var x = instance;");
    }

}

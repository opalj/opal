package org.opalj.fpcf.fixtures.xl.js.controlflow.intraprocedural.unidirectional;

import org.opalj.fpcf.properties.pts.JavaScriptContextAllocSite;
import org.opalj.fpcf.properties.pts.PointsToSet;
import org.opalj.fpcf.properties.xl.TAJSEnvironment;
import org.opalj.fpcf.properties.xl.TAJSEnvironmentBinding;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

public class JavaScriptAllocationNoPutNoGet {
    @TAJSEnvironment(
            bindings = {
                    @TAJSEnvironmentBinding(identifier = "y",
                            value = "Undef|\"test\"")
            }
    )
    public static void main(String args[]) throws ScriptException, NoSuchMethodException {
        ScriptEngineManager sem = new ScriptEngineManager();
        ScriptEngine se = sem.getEngineByName("JavaScript");
        se.eval("var x = 'test'; var y = x;");
    }
}

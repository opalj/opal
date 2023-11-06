package org.opalj.fpcf.fixtures.xl.js.controlflow.interprocedural.unidirectional;

import org.opalj.fpcf.properties.pts.JavaMethodContextAllocSite;
import org.opalj.fpcf.properties.pts.PointsToSet;
import org.opalj.xl.javaanalyses.benchmark.bidirectional.execution.Wrapper;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

/**
 * java instance passed through JavaScript identity function. java.lang.Integer instance is preserved.
 */
public class JavaAllocationJSIdentity {
    @PointsToSet(variableDefinition = 35,
            expectedJavaAllocSites = {
                    @JavaMethodContextAllocSite(
                            cf = JavaAllocationJSIdentity.class,
                            methodName = "main",
                            methodDescriptor = "(java.lang.String[]): void",
                            allocSiteLinenumber = 34,
                            allocatedType = "java.lang.Integer")
            }
    )
    public static void main(String args[]) throws ScriptException, NoSuchMethodException {
        ScriptEngineManager sem = new ScriptEngineManager();
        ScriptEngine se = sem.getEngineByName("JavaScript");
        Wrapper wrapper = new Wrapper();
        String n = wrapper.s;
        se.put("jThis", wrapper);
        se.eval("function id(o){return o;}; var a = 3; var o = jThis;");
        Invocable inv = (Invocable) se;
        Integer in = 50;
        Integer result = (Integer) inv.invokeFunction("id", in);
        System.out.println("result: " + result);
    }
}

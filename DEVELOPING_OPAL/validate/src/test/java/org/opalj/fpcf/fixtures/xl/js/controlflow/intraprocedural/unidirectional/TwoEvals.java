package org.opalj.fpcf.fixtures.xl.js.controlflow.intraprocedural.unidirectional;

import org.opalj.fpcf.properties.pts.JavaMethodContextAllocSite;
import org.opalj.fpcf.properties.pts.PointsToSet;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

/**
 * two functions defined in separate eval calls. https://github.com/nateusse/java/blob/main/java_impatient/src/main/java/ch14/sec02/ScriptEngineDemo.java
 */
public class TwoEvals {
    @PointsToSet(variableDefinition = 32,
            expectedJavaAllocSites = {
                    @JavaMethodContextAllocSite(
                            cf = TwoEvals.class,
                            methodName = "main",
                            methodDescriptor = "(java.lang.String[]): void",
                            allocSiteLinenumber = 31,
                            allocatedType = "java.lang.Integer")
            }
    )
    public static void main(String args[]) throws ScriptException, NoSuchMethodException {
        ScriptEngineManager sem = new ScriptEngineManager();
        ScriptEngine se = sem.getEngineByName("JavaScript");
        se.eval("function f(n){return n;}");
        se.eval("function g(n){return f(n);}");
        Invocable inv = (Invocable) se;
        Integer in = 50;
        Object out = inv.invokeFunction("f", in);
        System.out.println(out);
    }
}

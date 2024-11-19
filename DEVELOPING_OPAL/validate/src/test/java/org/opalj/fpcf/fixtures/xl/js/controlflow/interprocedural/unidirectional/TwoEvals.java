/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.xl.js.controlflow.interprocedural.unidirectional;

import org.opalj.fpcf.fixtures.xl.js.testpts.SimpleContainerClass;
import org.opalj.fpcf.properties.pts.JavaMethodContextAllocSite;
import org.opalj.fpcf.properties.pts.PointsToSet;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

/**
 * two functions defined in separate eval calls. https://github.com/nateusse/java/blob/main/java_impatient/src/main/java/ch14/sec02/ScriptEngineDemo.java
 */
public class TwoEvals {
    @PointsToSet(variableDefinition = 33,
            expectedJavaAllocSites = {
                    @JavaMethodContextAllocSite(
                            cf = TwoEvals.class,
                            methodName = "main",
                            methodDescriptor = "(java.lang.String[]): void",
                            allocSiteLinenumber = 29,
                            allocatedType = "org.opalj.fpcf.fixtures.xl.js.testpts.SimpleContainerClass")
            }
    )
    public static void main(String args[]) throws ScriptException, NoSuchMethodException {
        ScriptEngineManager sem = new ScriptEngineManager();
        ScriptEngine se = sem.getEngineByName("JavaScript");
        SimpleContainerClass in = new SimpleContainerClass();
        se.put("i", in);
        defineF(se);
        defineG(se);
        Object out = se.get("r");
        System.out.println(out);
    }

    public static void defineF(ScriptEngine se) throws ScriptException {
        se.eval("function f(n){return n;}");
    }
    public static void defineG(ScriptEngine se) throws ScriptException {
        se.eval("function g(n){return f(n);} var r = g(i);");
    }
}

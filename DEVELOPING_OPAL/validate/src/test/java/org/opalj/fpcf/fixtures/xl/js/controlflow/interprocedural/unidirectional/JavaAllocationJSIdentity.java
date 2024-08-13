/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.xl.js.controlflow.interprocedural.unidirectional;

import org.opalj.fpcf.fixtures.xl.js.testpts.SimpleContainerClass;
import org.opalj.fpcf.properties.pts.JavaMethodContextAllocSite;
import org.opalj.fpcf.properties.pts.PointsToSet;

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
                            allocSiteLinenumber = 32,
                            allocatedType = "org.opalj.fpcf.fixtures.xl.js.testpts.SimpleContainerClass")
            }
    )
    public static void main(String args[]) throws ScriptException, NoSuchMethodException {
        ScriptEngineManager sem = new ScriptEngineManager();
        ScriptEngine se = sem.getEngineByName("JavaScript");
        SimpleContainerClass wrapper = new SimpleContainerClass();
        String n = wrapper.s;
        se.put("jThis", wrapper);
        SimpleContainerClass in = new SimpleContainerClass();
        se.put("i", in);
        se.eval("function id(o){return o;} var a = 3; var o = jThis; var r = id(i);");
        SimpleContainerClass result = (SimpleContainerClass) se.get("r");
        System.out.println("result: " + result);
    }
}
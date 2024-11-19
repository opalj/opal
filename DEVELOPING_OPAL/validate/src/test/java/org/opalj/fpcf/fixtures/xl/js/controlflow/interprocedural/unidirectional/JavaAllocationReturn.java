/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.xl.js.controlflow.interprocedural.unidirectional;

import org.opalj.fpcf.fixtures.xl.js.testpts.SimpleContainerClass;
import org.opalj.fpcf.properties.pts.JavaMethodContextAllocSite;
import org.opalj.fpcf.properties.pts.PointsToSet;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
/**
 * A function evaluateScript() puts an instance of SimpleContainerClass created in another function,
 * evaluates a script returns instance.
 */
public class JavaAllocationReturn {
    @PointsToSet(variableDefinition = 28,
            expectedJavaAllocSites = {
            @JavaMethodContextAllocSite(
                cf = JavaAllocationReturn.class,
                    methodName = "getObject",
                    methodDescriptor = "(): java.lang.Object",
                    allocSiteLinenumber = 43,
                    allocatedType = "org.opalj.fpcf.fixtures.xl.js.testpts.SimpleContainerClass")

            }
    )
    public static void main(String args[]) throws ScriptException, NoSuchMethodException {
        Object p = evaluateScript();
        System.out.println(p.getClass());
    }

    public static Object evaluateScript() throws ScriptException {
        ScriptEngineManager sem = new ScriptEngineManager();
        ScriptEngine se = sem.getEngineByName("JavaScript");
        Object object = getObject();
        se.put("w", object);
        se.eval("var n = w;");
        Object p = se.get("n");
        return p;
    }

    public static Object getObject() {
        SimpleContainerClass sc = new SimpleContainerClass();
        sc.s = "flag";
        return sc;
    }
}

/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.xl.js.stateaccess.interprocedural.bidirectional;

import org.opalj.fpcf.properties.pts.JavaMethodContextAllocSite;
import org.opalj.fpcf.properties.pts.PointsToSet;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
/**
 * object stored in java field (from javascript).
 * reference in java field stored in javascript field (from java, using eval).
*/
public class JSJavaJavaJS3evals {
    @PointsToSet(variableDefinition = 38,
            expectedJavaAllocSites = {
                    @JavaMethodContextAllocSite(
                            cf = JSJavaJavaJS3evals.class,
                            methodName = "main",
                            methodDescriptor = "(java.lang.String[]): void",
                            allocSiteLinenumber = 30,
                            allocatedType = "java.lang.Object")
            }
    )
    public static void main(String args[]) throws ScriptException, NoSuchMethodException {
        JSJavaJavaJS3evals instance = new JSJavaJavaJS3evals();
        ScriptEngineManager sem = new ScriptEngineManager();
        ScriptEngine se = sem.getEngineByName("JavaScript");
        se.put("instance", instance);
        Object o = new Object();
        System.out.println(o);
        se.put("o", o);
        // JS -> Java state
        se.eval("var n = {'a':'b'}; instance.myfield = o;");
        // Java -> JS state
        setJSField(se, instance.myfield);
        se.eval("var n_field = n.field");
        Object n_field = se.get("n_field");
        System.out.println(n_field);
    }

    private static void setJSField(ScriptEngine se, Object fieldValue) throws ScriptException {
        se.put("o", fieldValue);
        se.eval("n.field = o;");
    }
    public Object myfield;

}
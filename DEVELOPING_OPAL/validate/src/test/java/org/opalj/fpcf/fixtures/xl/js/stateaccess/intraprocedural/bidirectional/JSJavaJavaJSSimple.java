/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.xl.js.stateaccess.intraprocedural.bidirectional;

import org.opalj.fpcf.properties.pts.JavaMethodContextAllocSite;
import org.opalj.fpcf.properties.pts.JavaScriptContextAllocSite;
import org.opalj.fpcf.properties.pts.PointsToSet;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
/**
 * object stored in java field (from javascript).
 * reference in java field stored in javascript field (from java, using eval).
*/
public class JSJavaJavaJSSimple {
    @PointsToSet(variableDefinition = 41,
            expectedJavaAllocSites = {
                    @JavaMethodContextAllocSite(
                            cf = JSJavaJavaJSSimple.class,
                            methodName = "main",
                            methodDescriptor = "(java.lang.String[]): void",
                            allocSiteLinenumber = 31,
                            allocatedType = "java.lang.Object")
            }
    )
    public static void main(String args[]) throws ScriptException, NoSuchMethodException {
        JSJavaJavaJSSimple instance = new JSJavaJavaJSSimple();
        ScriptEngineManager sem = new ScriptEngineManager();
        ScriptEngine se = sem.getEngineByName("JavaScript");
        se.put("instance", instance);
        Object o = new Object();
        System.out.println(o);
        se.put("o", o);
        // JS -> Java state
        se.eval("var n = {'a':'b'}; instance.myfield = o;");
        // Java -> JS state
        se.put("myfield", instance.myfield);
        se.eval("n.field = myfield");
        se.eval("var n_field = n.field");
        // verify
        Object n_field = se.get("n_field");
        System.out.println(n_field);
    }


    public Object myfield;

}

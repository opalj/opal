package org.opalj.fpcf.fixtures.xl.js.stateaccess.bidirectional;

import jdk.nashorn.api.scripting.ScriptObjectMirror;
import org.opalj.fpcf.properties.pts.JavaMethodContextAllocSite;
import org.opalj.fpcf.properties.pts.PointsToSet;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

/**
 * object stored in java field (from javascript).
 * reference in java field stored in javascript field (from java, using setMember).
 */
public class JSJavaJavaJS {
    @PointsToSet(variableDefinition = 38,
            expectedJavaAllocSites = {
                    @JavaMethodContextAllocSite(
                            cf = JSJavaJavaJS.class,
                            methodName = "main",
                            methodDescriptor = "(java.lang.String[]): void",
                            allocSiteLinenumber = 32,
                            allocatedType = "java.lang.Object")
            }
    )
    public static void main(String args[]) throws ScriptException, NoSuchMethodException {
        JSJavaJavaJS instance = new JSJavaJavaJS();
        ScriptEngineManager sem = new ScriptEngineManager();
        ScriptEngine se = sem.getEngineByName("JavaScript");
        se.put("instance", instance);
        Object o = new Object();
        System.out.println(o);
        se.put("o", o);
        se.eval("var n = {'a':'b'}; instance.myfield = o;");
        ScriptObjectMirror n = (ScriptObjectMirror) se.get("n");
        n.setMember("field", instance.myfield);
        se.eval("var n_field = n.field");
        Object n_field = se.get("n_field");
        System.out.println(n_field);
    }


    public Object myfield;

}

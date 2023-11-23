package org.opalj.fpcf.fixtures.xl.js.stateaccess.interprocedural.bidirectional;

import org.opalj.fpcf.properties.pts.JavaMethodContextAllocSite;
import org.opalj.fpcf.properties.pts.PointsToSet;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

/**
 * java instance stored in javascript field ( using eval).
 * then, javascript field stored in java field (from javascript).
 */
public class JavaJSJSJava {
    @PointsToSet(variableDefinition = 41,
            expectedJavaAllocSites = {
                    @JavaMethodContextAllocSite(
                            cf = JavaJSJSJava.class,
                            methodName = "main",
                            methodDescriptor = "(java.lang.String[]): void",
                            allocSiteLinenumber = 35,
                            allocatedType = "java.lang.Object")
            }
    )
    public static void main(String args[]) throws ScriptException, NoSuchMethodException {
        JavaJSJSJava instance = new JavaJSJSJava();
        ScriptEngineManager sem = new ScriptEngineManager();
        ScriptEngine se = sem.getEngineByName("JavaScript");
        se.put("instance", instance);
        se.eval("var n = {'a':'b'};");
        Object n = se.get("n");
        Object o = new Object();
        System.out.println(o);
        setJSField(se, o, n);
        se.put("n2", n);
        se.eval("instance.myfield = n2.field;");
        Object getField = instance.myfield;
        System.out.println(getField);
    }

    private static void setJSField(ScriptEngine se, Object fieldValue, Object jsObject) throws ScriptException {
        se.put("o", fieldValue);
        se.put("n2", jsObject);
        se.eval("n2.field = o;");
    }


    public Object myfield;

}

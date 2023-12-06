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
public class JavaJSJSJavaSimple {
    @PointsToSet(variableDefinition = 37,
            expectedJavaAllocSites = {
                    @JavaMethodContextAllocSite(
                            cf = JavaJSJSJavaSimple.class,
                            methodName = "main",
                            methodDescriptor = "(java.lang.String[]): void",
                            allocSiteLinenumber = 31,
                            allocatedType = "java.lang.Object")
            }
    )
    public static void main(String args[]) throws ScriptException, NoSuchMethodException {
        JavaJSJSJavaSimple instance = new JavaJSJSJavaSimple();
        ScriptEngineManager sem = new ScriptEngineManager();
        ScriptEngine se = sem.getEngineByName("JavaScript");
        se.put("instance", instance);
        se.eval("var n = {'a':'b'};");
        Object o = new Object();
        System.out.println(o);
        // Java -> JS state
        setJSField(se, o);
        // JS -> Java state
        se.eval("instance.myfield = n.field;");
        Object getField = instance.myfield;
        System.out.println(getField);
    }

    private static void setJSField(ScriptEngine se, Object fieldValue) throws ScriptException {
        se.put("o", fieldValue);
        se.eval("n.field = o;");
    }


    public Object myfield;

}

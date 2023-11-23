package org.opalj.fpcf.fixtures.xl.js.stateaccess.interprocedural.unidirectional.JSAccessJava;

import org.opalj.fpcf.properties.pts.JavaMethodContextAllocSite;
import org.opalj.fpcf.properties.pts.PointsToSet;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

/**
 * javascript code modifies a Java instance field (instantiated from Javascript).
 *
 *
 */
public class JSAllocationWriteFieldFromJS {
    /*
    @PointsToSet(variableDefinition = 34,
            expectedJavaAllocSites = {
            @JavaMethodContextAllocSite(
                cf = JSAllocationWriteFieldFromJS.class,
                    methodName = "main",
                    methodDescriptor = "(java.lang.String[]): void",
                    allocSiteLinenumber = 31,
                    allocatedType = "java.lang.Object")

            }
    )
    public static void main(String args[]) throws ScriptException, NoSuchMethodException {
        ScriptEngineManager sem = new ScriptEngineManager();
        ScriptEngine se = sem.getEngineByName("JavaScript");
        JSAllocationWriteFieldFromJS instance = (JSAllocationWriteFieldFromJS) instantiate(se);
        Object myobject = new Object();
        System.out.println(myobject);
       setJavaField(se, myobject, instance);
        Object instancefield = instance.myfield;
        System.out.println(instancefield);

    }
    private static Object instantiate(ScriptEngine se) throws ScriptException {

         se.eval("var javaTestClass = Java.type(\"org.opalj.fpcf.fixtures.xl.js.stateaccess.interprocedural.unidirectional.JSAccessJava.JSAllocationWriteFieldFromJS\"); ");
        se.eval("var instance = new javaTestClass()");
        return se.get("instance");
    }
    private static void setJavaField(ScriptEngine se, Object fieldValue, Object javaObject) throws ScriptException {
        se.put("o", fieldValue);
        se.put("n2", javaObject);
        se.eval("n2.myfield = o;");
    }

    public Object myfield;
    */
}

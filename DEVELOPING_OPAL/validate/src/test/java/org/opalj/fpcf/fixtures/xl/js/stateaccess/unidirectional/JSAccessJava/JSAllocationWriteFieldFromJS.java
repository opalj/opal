package org.opalj.fpcf.fixtures.xl.js.stateaccess.unidirectional.JSAccessJava;

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
    @PointsToSet(variableDefinition = 35,
            expectedJavaAllocSites = {
            @JavaMethodContextAllocSite(
                cf = JSAllocationWriteFieldFromJS.class,
                    methodName = "main",
                    methodDescriptor = "(java.lang.String[]): void",
                    allocSiteLinenumber = 32,
                    allocatedType = "java.lang.Object")

            }
    )
    public static void main(String args[]) throws ScriptException, NoSuchMethodException {
        JSAllocationWriteFieldFromJS instance = new JSAllocationWriteFieldFromJS();
        ScriptEngineManager sem = new ScriptEngineManager();
        ScriptEngine se = sem.getEngineByName("JavaScript");
        se.put("instance", instance);
        Object myobject = new Object();
        se.put("myobject", myobject);
        se.eval("var javaTestClass = Java.type(\"org.opalj.fpcf.fixtures.xl.js.dataflow.unidirectional.JSAccessJava.JSAllocationWriteFieldFromJS\"); var instance = new javaTestClass();  instance.myfield = mystring");
        Object instancefield = instance.myfield;
        System.out.println(instancefield);

    }
    public Object myfield;
}

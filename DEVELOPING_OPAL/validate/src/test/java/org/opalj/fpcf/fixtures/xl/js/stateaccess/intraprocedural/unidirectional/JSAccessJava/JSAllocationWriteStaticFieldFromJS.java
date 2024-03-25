package org.opalj.fpcf.fixtures.xl.js.stateaccess.intraprocedural.unidirectional.JSAccessJava;

import org.opalj.fpcf.fixtures.xl.js.controlflow.intraprocedural.unidirectional.JavaScriptAllocationNoPut;
import org.opalj.fpcf.properties.pts.JavaMethodContextAllocSite;
import org.opalj.fpcf.properties.pts.JavaScriptContextAllocSite;
import org.opalj.fpcf.properties.pts.PointsToSet;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

/**
 * javascript code modifies a Java instance field (instantiated from Javascript).
 *
 *
 */
public class JSAllocationWriteStaticFieldFromJS {
    @PointsToSet(variableDefinition = 30,
            expectedJavaScriptAllocSites = @JavaScriptContextAllocSite(
                    cf = JSAllocationWriteStaticFieldFromJS.class,
                    nodeIdTAJS = 16,
                    allocatedType = "java.lang.Object"
            )
    )
    public static void main(String args[]) throws ScriptException, NoSuchMethodException {
        ScriptEngineManager sem = new ScriptEngineManager();
        ScriptEngine se = sem.getEngineByName("JavaScript");
        se.eval("var javaTestClass = Java.type(\"org.opalj.fpcf.fixtures.xl.js.stateaccess.intraprocedural.unidirectional.JSAccessJava.JSAllocationWriteStaticFieldFromJS\"); var myobject = {'a' : 3}; javaTestClass.myfield = myobject");

        Object instancefield = JSAllocationWriteStaticFieldFromJS.myfield;
        System.out.println(instancefield);

    }
    public static Object myfield;
}
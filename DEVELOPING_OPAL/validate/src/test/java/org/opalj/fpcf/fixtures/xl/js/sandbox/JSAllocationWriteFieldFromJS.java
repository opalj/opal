package org.opalj.fpcf.fixtures.xl.js.sandbox;
/*
import org.opalj.fpcf.properties.pts.JavaMethodContextAllocSite;
import org.opalj.fpcf.properties.pts.PointsToSet;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
*/
/**
 * javascript code modifies a Java instance field (instantiated from Javascript).
 *
 *
 */
/*public class JSAllocationWriteFieldFromJS {

    public static void main(String args[]) throws ScriptException, NoSuchMethodException {
        ScriptEngineManager sem = new ScriptEngineManager();
        ScriptEngine se = sem.getEngineByName("JavaScript");
        Object myobject = new Object();
        System.out.println(myobject);
        se.put("myobject", myobject);
        se.eval("var javaTestClass = Java.type(\"org.opalj.fpcf.fixtures.xl.js.stateaccess.intraprocedural.unidirectional.JSAccessJava.JSAllocationWriteFieldFromJS\"); var instance = new javaTestClass();  instance.myfield = myobject");
        JSAllocationWriteFieldFromJS instance = (JSAllocationWriteFieldFromJS)se.get("instance");
        Object instancefield = instance.myfield;
        System.out.println(instancefield);

    }
    public Object myfield;
}
*/
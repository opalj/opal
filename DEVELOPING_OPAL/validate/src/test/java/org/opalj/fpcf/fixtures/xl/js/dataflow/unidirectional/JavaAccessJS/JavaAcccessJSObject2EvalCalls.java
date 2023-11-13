package org.opalj.fpcf.fixtures.xl.js.dataflow.unidirectional.JavaAccessJS;

import jdk.nashorn.api.scripting.ScriptObjectMirror;
import org.opalj.fpcf.fixtures.xl.js.dataflow.unidirectional.JSAccessJava.JSAllocationWriteFieldFromJS;
import org.opalj.fpcf.properties.pts.JavaMethodContextAllocSite;
import org.opalj.fpcf.properties.pts.PointsToSet;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

/**
 * java modifies js state through setMember()
 *
 * https://github.com/Sdk0815/logbook-kai/blob/6b6c62882de761c114c51c32425a885c55992137/src/main/java/logbook/internal/gui/BattleLogScriptController.java#L15
 *
 */
public class JavaAcccessJSObject2EvalCalls {
    @PointsToSet(variableDefinition = 43,
            expectedJavaAllocSites = {
                    @JavaMethodContextAllocSite(
                            cf = JSAllocationWriteFieldFromJS.class,
                            methodName = "main",
                            methodDescriptor = "(java.lang.String[]): void",
                            allocSiteLinenumber = 39,
                            allocatedType = "java.lang.Object")

            }
    )
    public static void main(String args[]) throws ScriptException, NoSuchMethodException {
        JavaAcccessJSObject2EvalCalls instance = new JavaAcccessJSObject2EvalCalls();
        ScriptEngineManager sem = new ScriptEngineManager();
        ScriptEngine se = sem.getEngineByName("JavaScript");
        se.put("instance", instance);
        se.eval("var n = {'a':'b'}");
        ScriptObjectMirror n = (ScriptObjectMirror) se.get("n");
        System.out.println(n);
        Object myobject = new Object();
        n.setMember("field", myobject);
        se.eval("var ob = n");
        ScriptObjectMirror ob = (ScriptObjectMirror) se.get("ob");
        Object getField = ob.get("field"); // ob and n are not the same instance in java
        System.out.println(getField); // getfield and myobject are the same instance
    }

}

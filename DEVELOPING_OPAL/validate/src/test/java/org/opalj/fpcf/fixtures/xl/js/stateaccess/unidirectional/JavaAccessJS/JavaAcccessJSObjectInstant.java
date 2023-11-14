package org.opalj.fpcf.fixtures.xl.js.stateaccess.unidirectional.JavaAccessJS;

import jdk.nashorn.api.scripting.ScriptObjectMirror;
import org.opalj.fpcf.fixtures.xl.js.stateaccess.unidirectional.JSAccessJava.JSAllocationWriteFieldFromJS;
import org.opalj.fpcf.properties.pts.JavaMethodContextAllocSite;
import org.opalj.fpcf.properties.pts.PointsToSet;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

/**
 * java modifies js state through setMember()
 * field is immediately read through getSatte ( no JS analysis necessary )
 */
public class JavaAcccessJSObjectInstant {
    @PointsToSet(variableDefinition = 39,
            expectedJavaAllocSites = {
                    @JavaMethodContextAllocSite(
                            cf = JSAllocationWriteFieldFromJS.class,
                            methodName = "main",
                            methodDescriptor = "(java.lang.String[]): void",
                            allocSiteLinenumber = 37,
                            allocatedType = "java.lang.Object")

            }
    )
    public static void main(String args[]) throws ScriptException, NoSuchMethodException {
        JavaAcccessJSObjectInstant instance = new JavaAcccessJSObjectInstant();
        ScriptEngineManager sem = new ScriptEngineManager();
        ScriptEngine se = sem.getEngineByName("JavaScript");
        se.put("instance", instance);
        se.eval("var n = {'a':'b'}");
        ScriptObjectMirror n = (ScriptObjectMirror) se.get("n");
        System.out.println(n);
        Object myobject = new Object();
        n.setMember("field", myobject);
        Object getField = n.get("field"); // ob and n are not the same instance in java
        System.out.println(getField); // getfield and myobject are the same instance
    }

}

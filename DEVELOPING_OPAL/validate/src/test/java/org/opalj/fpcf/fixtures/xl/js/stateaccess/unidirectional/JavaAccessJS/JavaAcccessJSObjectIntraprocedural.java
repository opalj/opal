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
public class JavaAcccessJSObjectIntraprocedural {
    @PointsToSet(variableDefinition = 35,
            expectedJavaAllocSites = {
                    @JavaMethodContextAllocSite(
                            cf = JSAllocationWriteFieldFromJS.class,
                            methodName = "setField",
                            methodDescriptor = "(jdk.nashorn.api.scripting.ScriptObjectMirror): void",
                            allocSiteLinenumber = 40,
                            allocatedType = "java.lang.Object")
            }
    )
    public static void main(String args[]) throws ScriptException, NoSuchMethodException {
        JavaAcccessJSObjectIntraprocedural instance = new JavaAcccessJSObjectIntraprocedural();
        ScriptEngineManager sem = new ScriptEngineManager();
        ScriptEngine se = sem.getEngineByName("JavaScript");
        se.put("instance", instance);
        se.eval("var n = {'a':'b'}; instance.setField(n);");
        ScriptObjectMirror n = (ScriptObjectMirror) se.get("n");
        System.out.println(n);
        Object getField = n.get("field");
        System.out.println(getField);
    }

    public void setField(ScriptObjectMirror obj) {
        Object myobject = new Object();
        obj.setMember("field", myobject);
    }

}

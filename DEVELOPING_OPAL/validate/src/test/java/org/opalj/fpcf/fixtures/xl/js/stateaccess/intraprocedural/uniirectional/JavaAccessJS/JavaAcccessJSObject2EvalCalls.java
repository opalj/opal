package org.opalj.fpcf.fixtures.xl.js.stateaccess.intraprocedural.uniirectional.JavaAccessJS;

import org.opalj.fpcf.fixtures.xl.js.stateaccess.intraprocedural.uniirectional.JSAccessJava.JSAllocationWriteFieldFromJS;
import org.opalj.fpcf.properties.pts.JavaMethodContextAllocSite;
import org.opalj.fpcf.properties.pts.PointsToSet;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

/**
 * java modifies js state (setMember not yet supported)
 * field of JS object is set using eval. in separate eval, field is read from same JS object
 * (setMember: https://github.com/Sdk0815/logbook-kai/blob/6b6c62882de761c114c51c32425a885c55992137/src/main/java/logbook/internal/gui/BattleLogScriptController.java#L15)
 *
 */
public class JavaAcccessJSObject2EvalCalls {
    @PointsToSet(variableDefinition = 41,
            expectedJavaAllocSites = {
                    @JavaMethodContextAllocSite(
                            cf = JSAllocationWriteFieldFromJS.class,
                            methodName = "main",
                            methodDescriptor = "(java.lang.String[]): void",
                            allocSiteLinenumber = 36,
                            allocatedType = "java.lang.Object")

            }
    )
    public static void main(String args[]) throws ScriptException, NoSuchMethodException {
        JavaAcccessJSObject2EvalCalls instance = new JavaAcccessJSObject2EvalCalls();
        ScriptEngineManager sem = new ScriptEngineManager();
        ScriptEngine se = sem.getEngineByName("JavaScript");
        se.put("instance", instance);
        se.eval("var n = {'a':'b'};");
        Object n = se.get("n");
        Object myobject = new Object();
        System.out.println(myobject);
        se.put("fieldVal", myobject);
        se.eval("n.field = fieldVal;");
        se.put("o2", n);
        Object getField = se.eval("o2.field");
        System.out.println(getField);
    }

}

package org.opalj.fpcf.fixtures.xl.js.sandbox;
/*
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
 *//*
public class JavaAcccessJSObject2EvalCalls {
    @PointsToSet(variableDefinition = 38,
            expectedJavaAllocSites = {
                    @JavaMethodContextAllocSite(
                            cf = JavaAcccessJSObject2EvalCalls.class,
                            methodName = "main",
                            methodDescriptor = "(java.lang.String[]): void",
                            allocSiteLinenumber = 35,
                            allocatedType = "org.opalj.fpcf.fixtures.xl.js.sandbox.O")

            }
    )
    public static void main(String args[]) throws ScriptException, NoSuchMethodException {
        JavaAcccessJSObject2EvalCalls instance = new JavaAcccessJSObject2EvalCalls();
        ScriptEngineManager sem = new ScriptEngineManager();
        ScriptEngine se = sem.getEngineByName("JavaScript");
        se.put("instance", instance);
        se.eval("var n = {'a':'b'};");
        O n = new O(); // se.get("n");
        O myobject = new O();
        System.out.println(myobject);
        setJSField(se, myobject, n);
        Object getField = getJSField(se, n);
        System.out.println(getField);
    }

    private static Object getJSField(ScriptEngine se, Object n) throws ScriptException {
        se.put("o2", n);
        se.eval("var result = o2.field");
        Object getField = se.get("result");
        return getField;
    }

    private static void setJSField(ScriptEngine se, Object fieldValue, Object jsObject) throws ScriptException {
        se.put("o", fieldValue);
        se.put("n2", jsObject);
        se.eval("n2.field = o;");
    }


}
class O{
    Object field = new Object();
}*/
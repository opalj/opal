package org.opalj.fpcf.fixtures.xl.js.stateaccess.intraprocedural.unidirectional.JSAccessJava.sandyboxy;

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
public class ReadInstanceFieldFromJS {
    @PointsToSet(variableDefinition = 36,
            expectedJavaAllocSites = {
            @JavaMethodContextAllocSite(
                cf = ReadInstanceFieldFromJS.class,
                    methodName = "main",
                    methodDescriptor = "(java.lang.String[]): void",
                    allocSiteLinenumber = 30,
                    allocatedType = "java.lang.Object")

            }
    )
    public static void main(String args[]) throws ScriptException, NoSuchMethodException {
        ScriptEngineManager sem = new ScriptEngineManager();
        ScriptEngine se = sem.getEngineByName("JavaScript");
        Object myobject = new Object();
        System.out.println(myobject);
        ReadInstanceFieldFromJS container = new ReadInstanceFieldFromJS();
        container.myfield = myobject;
        se.put("container", container);
        se.eval("var x = container.myfield;");
        Object instancefield = se.get("x");
        System.out.println(instancefield);

    }
    public Object myfield;
}

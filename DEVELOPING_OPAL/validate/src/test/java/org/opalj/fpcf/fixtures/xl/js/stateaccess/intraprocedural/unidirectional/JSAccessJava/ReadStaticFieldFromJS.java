/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.xl.js.stateaccess.intraprocedural.unidirectional.JSAccessJava;

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
public class ReadStaticFieldFromJS {
    @PointsToSet(variableDefinition = 35,
            expectedJavaAllocSites = {
            @JavaMethodContextAllocSite(
                cf = ReadStaticFieldFromJS.class,
                    methodName = "main",
                    methodDescriptor = "(java.lang.String[]): void",
                    allocSiteLinenumber = 31,
                    allocatedType = "java.lang.Object")

            }
    )
    public static void main(String args[]) throws ScriptException, NoSuchMethodException {
        ScriptEngineManager sem = new ScriptEngineManager();
        ScriptEngine se = sem.getEngineByName("JavaScript");
        Object myobject = new Object();
        System.out.println(myobject);
        ReadStaticFieldFromJS.myfield = myobject;
        se.eval("var javaTestClass = Java.type(\"org.opalj.fpcf.fixtures.xl.js.stateaccess.intraprocedural.unidirectional.JSAccessJava.ReadStaticFieldFromJS\"); var x = javaTestClass.myfield;");
        Object instancefield = se.get("x");
        System.out.println(instancefield);

    }
    public static Object myfield;
}

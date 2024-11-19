/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.xl.js.controlflow.interprocedural.unidirectional;

import org.opalj.fpcf.fixtures.xl.js.testpts.SimpleContainerClass;
import org.opalj.fpcf.properties.pts.JavaMethodContextAllocSite;
import org.opalj.fpcf.properties.pts.PointsToSet;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

/**
 * A function evaluateScript() puts an instance of SimpleContainerClass created in another function,
 * evaluates a script returns instance.
 */
public class CallStatic {

    public static void main(String args[]) throws ScriptException, NoSuchMethodException {
        ScriptEngineManager sem = new ScriptEngineManager();
        ScriptEngine se = sem.getEngineByName("JavaScript");
        Object object = new Object();
        System.out.println(object);
        se.put("w", object);
        se.eval("var javaTestClass = Java.type(\"org.opalj.fpcf.fixtures.xl.js.controlflow.interprocedural.unidirectional.CallStatic\"); javaTestClass.myStaticFunction(w);");
    }
    @PointsToSet(parameterIndex = 0,
            expectedJavaAllocSites = {
                    @JavaMethodContextAllocSite(
                            cf = CallStatic.class,
                            methodName = "main",
                            methodDescriptor = "(java.lang.String[]): void",
                            allocSiteLinenumber = 21,
                            allocatedType = "java.lang.Object")

            }
    )
    public static void myStaticFunction(Object o) {
        System.out.println(o);
    }
}

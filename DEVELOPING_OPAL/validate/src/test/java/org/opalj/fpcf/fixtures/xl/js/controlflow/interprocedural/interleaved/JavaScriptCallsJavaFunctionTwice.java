/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.xl.js.controlflow.interprocedural.interleaved;

import org.opalj.fpcf.fixtures.xl.js.testpts.SimpleContainerClass;
import org.opalj.fpcf.properties.pts.JavaMethodContextAllocSite;
import org.opalj.fpcf.properties.pts.PointsToSet;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

/**
 * two java functions are called in sequence.
 */
public class JavaScriptCallsJavaFunctionTwice {
    @PointsToSet(variableDefinition = 36,
            expectedJavaAllocSites = {
                    @JavaMethodContextAllocSite(
                            cf = JavaScriptCallsJavaFunctionTwice.class,
                            methodName = "main",
                            methodDescriptor = "(java.lang.String[]): void",
                            allocSiteLinenumber = 30,
                            allocatedType = "java.lang.Object")
            }
    )
    public static void main(String args[]) throws ScriptException, NoSuchMethodException {
        ScriptEngineManager sem = new ScriptEngineManager();
        ScriptEngine se = sem.getEngineByName("JavaScript");
        SimpleContainerClass w = new SimpleContainerClass();
        Object s = new Object();
        System.out.println(s);
        se.put("w", s);
        JavaScriptCallsJavaFunctionTwice inst = new JavaScriptCallsJavaFunctionTwice();
        setScriptEngineInstance(se, inst);
        se.eval("var n = w; var javainstance = inst; var x1 = javainstance.identity1(n); var x2 = javainstance.identity2(x1);");
        Object o = se.get("x1");
        System.out.println(o);
    }


    public Object identity1(Object x) {
        System.out.println(x);
        return x;
    }
    public Object identity2(Object x) {
        System.out.println(x);
        return x;
    }


    public static void setScriptEngineInstance(ScriptEngine se, JavaScriptCallsJavaFunctionTwice inst) {
        se.put("inst", inst);
    }
}

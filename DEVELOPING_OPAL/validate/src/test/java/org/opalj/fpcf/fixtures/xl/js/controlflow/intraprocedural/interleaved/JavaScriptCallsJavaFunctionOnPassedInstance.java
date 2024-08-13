/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.xl.js.controlflow.intraprocedural.interleaved;

import org.opalj.fpcf.fixtures.xl.js.testpts.SimpleContainerClass;
import org.opalj.fpcf.properties.pts.JavaMethodContextAllocSite;
import org.opalj.fpcf.properties.pts.PointsToSet;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

/**
 * An instance of JavaScriptCallsJavaFunction and SimpleContainerClass are passed to ScriptEngine.
 * in eval, JavaScriptCallsJavaFunction.javaFunctionCalledFromJS() is called, SimpleContainerClass instance is passed.
 * PTS of arg in javaFunctionCalledFromJS shoudl include alloc site before eval call.
 */
public class JavaScriptCallsJavaFunctionOnPassedInstance {

    public static void main(String args[]) throws ScriptException, NoSuchMethodException {
        ScriptEngineManager sem = new ScriptEngineManager();
        ScriptEngine se = sem.getEngineByName("JavaScript");
        SimpleContainerClass w = new SimpleContainerClass();
        String s = "flag";
        w.s = s;
        se.put("w", w);
        JavaScriptCallsJavaFunctionOnPassedInstance inst = new JavaScriptCallsJavaFunctionOnPassedInstance();
        se.put("inst", inst);
        se.eval("var n = w; var javainstance = inst; javainstance.javaFunctionCalledFromJS(n);");
    }

    @PointsToSet(variableDefinition = 42,
            expectedJavaAllocSites = {
                    @JavaMethodContextAllocSite(
                            cf = JavaScriptCallsJavaFunctionOnPassedInstance.class,
                            methodName = "main",
                            methodDescriptor = "(java.lang.String[]): void",
                            allocSiteLinenumber = 23,
                            allocatedType = "java.lang.String")
            }
    )
    public void javaFunctionCalledFromJS(SimpleContainerClass argument) {
        String s = argument.s;
        System.out.println(s);
    }
}

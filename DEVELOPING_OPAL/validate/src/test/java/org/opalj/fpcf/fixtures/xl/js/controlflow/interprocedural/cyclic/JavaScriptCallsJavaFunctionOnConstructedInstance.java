package org.opalj.fpcf.fixtures.xl.js.controlflow.interprocedural.cyclic;

import org.opalj.fpcf.fixtures.xl.js.testpts.SimpleContainerClass;
import org.opalj.fpcf.properties.pts.JavaMethodContextAllocSite;
import org.opalj.fpcf.properties.pts.PointsToSet;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

/**
 * An instance  SimpleContainerClass are passed to ScriptEngine.
 * in eval, JavaScriptCallsJavaFunctionOnConstructedInstance is instantiated dynamically, javaFunctionCalledFromJS() is called, SimpleContainerClass instance is passed.
 * PTS of arg in javaFunctionCalledFromJS shoudl include alloc site before eval call.
 *
 * Real world case: https://github.com/exzhawk/OmniOcular/blob/946579584b4b19bda135207f8ceae57e5bf9802f/src/main/java/me/exz/omniocular/handler/JSHandler.java#L17
 */
public class JavaScriptCallsJavaFunctionOnConstructedInstance {

    public static void main(String args[]) throws ScriptException, NoSuchMethodException {
        ScriptEngineManager sem = new ScriptEngineManager();
        ScriptEngine se = sem.getEngineByName("JavaScript");
        SimpleContainerClass w = new SimpleContainerClass();
        String s = "flag";
        w.s = s;
        se.put("w", w);
        JavaScriptCallsJavaFunctionOnConstructedInstance inst = new JavaScriptCallsJavaFunctionOnConstructedInstance();
        se.put("inst", inst);
        se.eval("var n = w; var javaTestClass = Java.type(\"org.opalj.fpcf.fixtures.xl.js.controlflow.intraprocedural.interleaved.JavaScriptCallsJavaFunctionOnConstructedInstance\"); var javainstance = new javaTestClass(); javainstance.javaFunctionCalledFromJS(n);");
    }

    @PointsToSet(variableDefinition = 43,
            expectedJavaAllocSites = {
                    @JavaMethodContextAllocSite(
                            cf = JavaScriptCallsJavaFunctionOnConstructedInstance.class,
                            methodName = "main",
                            methodDescriptor = "(java.lang.String[]): void",
                            allocSiteLinenumber = 24,
                            allocatedType = "java.lang.String")
            }
    )
    public void javaFunctionCalledFromJS(SimpleContainerClass argument) {
        String s = argument.s;
        System.out.println(s);
    }
}

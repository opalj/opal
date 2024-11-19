/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.xl.js.controlflow.interprocedural.interleaved;

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
 * Real world case: https://github.com/exzhawk/OmniOcular/blob/946579584b4b19bda135207f8ceae57e5bf9802f/src/main/java/me/exz/omniocular/handler/JSHandler.java#L17
 * https://github.com/ilmila/J2EEScan/blob/1936af81732b8abfa9e4447c80335986d487460f/src/main/java/burp/j2ee/issues/impl/SpringDataCommonRCE.java#L83
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
        setScriptEngineInstance(se, inst);
        se.eval("var n = w; var javaTestClass = Java.type(\"org.opalj.fpcf.fixtures.xl.js.controlflow.interprocedural.interleaved.JavaScriptCallsJavaFunctionOnConstructedInstance\"); var javainstance = new javaTestClass(); javainstance.javaFunctionCalledFromJS(n);");
    }

    @PointsToSet(variableDefinition = 44,
            expectedJavaAllocSites = {
                    @JavaMethodContextAllocSite(
                            cf = JavaScriptCallsJavaFunctionOnConstructedInstance.class,
                            methodName = "main",
                            methodDescriptor = "(java.lang.String[]): void",
                            allocSiteLinenumber = 25,
                            allocatedType = "java.lang.String")
            }
    )
    public void javaFunctionCalledFromJS(SimpleContainerClass argument) {
        String s = argument.s;
        System.out.println(s);
    }

    public static void setScriptEngineInstance(ScriptEngine se, JavaScriptCallsJavaFunctionOnConstructedInstance inst) {
        se.put("inst", inst);
    }
}

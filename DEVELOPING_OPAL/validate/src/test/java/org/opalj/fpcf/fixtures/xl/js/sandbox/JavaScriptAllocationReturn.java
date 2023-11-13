package org.opalj.fpcf.fixtures.xl.js.sandbox;

import org.opalj.fpcf.properties.pts.JavaScriptContextAllocSite;
import org.opalj.fpcf.properties.pts.PointsToSet;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

/**
 * A function evaluateScript() evaluates a script that instantiates a JS object, which is returned.
 */
public class JavaScriptAllocationReturn {

    public static void main(String args[]) throws ScriptException, NoSuchMethodException {
        Object p = evaluateScript();
        System.out.println(p.getClass());
    }

    @PointsToSet(variableDefinition = 34,
            expectedJavaScriptAllocSites = @JavaScriptContextAllocSite(
                    evalCallSource = JavaScriptAllocationReturn.class,
                    evalCallLineNumber = 30,
                    allocatedType = "java.lang.Object"
            )
    )
    public static Object evaluateScript() throws ScriptException {
        ScriptEngineManager sem = new ScriptEngineManager();
        ScriptEngine se = sem.getEngineByName("JavaScript");
        SimpleContainerClass simpleContainerClass = new SimpleContainerClass();
        simpleContainerClass.o = "abc";
        se.put("scc", simpleContainerClass);
        se.eval("var p = scc.o;");//={};"); //function O(a) {this.a=a;}; const o = new O(\"name\");");
        Object p = se.get("p");
        Object a = p;
        return a;
    }
}

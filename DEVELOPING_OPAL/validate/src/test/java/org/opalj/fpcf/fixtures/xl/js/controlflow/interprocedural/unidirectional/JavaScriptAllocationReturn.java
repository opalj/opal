/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.xl.js.controlflow.interprocedural.unidirectional;

import org.opalj.fpcf.properties.pts.JavaScriptContextAllocSite;
import org.opalj.fpcf.properties.pts.PointsToSet;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

/**
 * A function evaluateScript() evaluates a script that instantiates a JS object, which is returned.
 */
public class JavaScriptAllocationReturn {
    @PointsToSet(variableDefinition = 23,
            expectedJavaScriptAllocSites = @JavaScriptContextAllocSite(
                    cf = JavaScriptAllocationReturn.class,
                    nodeIdTAJS = 11,
                    allocatedType = "java.lang.Object"
            )
    )
    public static void main(String args[]) throws ScriptException, NoSuchMethodException {
        Object p = evaluateScript();
        System.out.println(p.getClass());
    }

    public static Object evaluateScript() throws ScriptException {
        ScriptEngineManager sem = new ScriptEngineManager();
        ScriptEngine se = sem.getEngineByName("JavaScript");
        se.eval("var x = {'a':10}; var y = x;");
        Object p = se.get("y");
        return p;
    }
}

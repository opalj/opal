/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.xl.js.stateaccess.interprocedural.bidirectional;

import org.opalj.fpcf.properties.pts.JavaMethodContextAllocSite;
import org.opalj.fpcf.properties.pts.PointsToSet;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

/**
 * java instance stored in javascript field ( using eval).
 * then, javascript field stored in java field (from javascript).
*/
public class JavaJSJSJavaSimple2evals {
    @PointsToSet(variableDefinition = 38,
            expectedJavaAllocSites = {
                    @JavaMethodContextAllocSite(
                            cf = JavaJSJSJavaSimple2evals.class,
                            methodName = "main",
                            methodDescriptor = "(java.lang.String[]): void",
                            allocSiteLinenumber = 32,
                            allocatedType = "java.lang.Object")
            }
    )
    public static void main(String args[]) throws ScriptException, NoSuchMethodException {
        JavaJSJSJavaSimple2evals instance = new JavaJSJSJavaSimple2evals();
        ScriptEngineManager sem = new ScriptEngineManager();
        ScriptEngine se = sem.getEngineByName("JavaScript");
        se.put("instance", instance);
        se.eval("var n = {'a':'b'};");
        Object o = new Object();
        System.out.println(o);
        // Java -> JS state
        setJSField(se, o);
        // JS -> Java state
        se.eval("instance.myfield = n.field;");
        Object getField = instance.myfield;
        System.out.println(getField);
    }

    private static void setJSField(ScriptEngine se, Object fieldValue) throws ScriptException {
        se.put("o", fieldValue);
        se.eval("n.field = o;");
    }


    public Object myfield;

}

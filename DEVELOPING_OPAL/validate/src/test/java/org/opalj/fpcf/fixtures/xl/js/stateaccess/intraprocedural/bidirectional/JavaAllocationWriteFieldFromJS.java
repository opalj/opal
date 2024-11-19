/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.xl.js.stateaccess.intraprocedural.bidirectional;

import org.opalj.fpcf.properties.pts.JavaMethodContextAllocSite;
import org.opalj.fpcf.properties.pts.PointsToSet;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

/**
 * javascript code modifies a Java instance field.
 * <p>
 * https://github.com/jindw/lite/blob/d4b0c7ef54c2469bc10bf63dc892e11e4fd1f7a1/src/main/java/org/xidea/lite/LiteCompiler.java#L45
 */
public class JavaAllocationWriteFieldFromJS {
    @PointsToSet(variableDefinition = 36,
            expectedJavaAllocSites = {
                    @JavaMethodContextAllocSite(
                            cf = JavaAllocationWriteFieldFromJS.class,
                            methodName = "main",
                            methodDescriptor = "(java.lang.String[]): void",
                            allocSiteLinenumber = 33,
                            allocatedType = "java.lang.Object")

            }
    )
    public static void main(String args[]) throws ScriptException, NoSuchMethodException {
        JavaAllocationWriteFieldFromJS instance = new JavaAllocationWriteFieldFromJS();
        ScriptEngineManager sem = new ScriptEngineManager();
        ScriptEngine se = sem.getEngineByName("JavaScript");
        se.put("instance", instance);
        Object myobject = new Object();
        se.put("myobject", myobject);
        se.eval("instance.myfield = myobject");
        Object instancefield = instance.myfield;
        System.out.println(instancefield);

    }

    public Object myfield;
}

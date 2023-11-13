package org.opalj.fpcf.fixtures.xl.js.dataflow.bidirectional;

import org.opalj.fpcf.properties.pts.JavaMethodContextAllocSite;
import org.opalj.fpcf.properties.pts.PointsToSet;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

/**
 * javascript code modifies a Java instance field.
 *
 * https://github.com/jindw/lite/blob/d4b0c7ef54c2469bc10bf63dc892e11e4fd1f7a1/src/main/java/org/xidea/lite/LiteCompiler.java#L45
 *
 */
public class JavaAllocationReadFieldReturn {
    @PointsToSet(variableDefinition = 37,
            expectedJavaAllocSites = {
            @JavaMethodContextAllocSite(
                cf = JavaAllocationReadFieldReturn.class,
                    methodName = "getObject",
                    methodDescriptor = "(java.lang.String[]): java.lang.Object",
                    allocSiteLinenumber = 34,
                    allocatedType = "java.lang.String")

            }
    )
    public static void main(String args[]) throws ScriptException, NoSuchMethodException {
        JavaAllocationReadFieldReturn instance = new JavaAllocationReadFieldReturn();
        ScriptEngineManager sem = new ScriptEngineManager();
        ScriptEngine se = sem.getEngineByName("JavaScript");
        se.put("instance", instance);
        String mystring = "flag";
        se.put("mystring", mystring);
        se.eval("instance.myfield = mystring");
        String instancefield = instance.myfield;
        System.out.println(instancefield);

    }
    public String myfield;
}

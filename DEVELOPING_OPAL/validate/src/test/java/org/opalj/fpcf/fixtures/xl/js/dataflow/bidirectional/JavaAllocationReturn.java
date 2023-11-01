package org.opalj.fpcf.fixtures.xl.js.dataflow.bidirectional;

import org.opalj.fpcf.fixtures.xl.js.testpts.SimpleContainerClass;
import org.opalj.fpcf.properties.pts.JavaMethodContextAllocSite;
import org.opalj.fpcf.properties.pts.PointsToSet;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

/**
 * javascript code modifies a Java instance field.
 *
 *
 *
 */
public class JavaAllocationReturn {
    @PointsToSet(variableDefinition = 37,
            expectedJavaAllocSites = {
            @JavaMethodContextAllocSite(
                cf = JavaAllocationReturn.class,
                    methodName = "getObject",
                    methodDescriptor = "(java.lang.String[]): java.lang.Object",
                    allocSiteLinenumber = 34,
                    allocatedType = "java.lang.String")

            }
    )
    public static void main(String args[]) throws ScriptException, NoSuchMethodException {
        JavaAllocationReturn instance = new JavaAllocationReturn();
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

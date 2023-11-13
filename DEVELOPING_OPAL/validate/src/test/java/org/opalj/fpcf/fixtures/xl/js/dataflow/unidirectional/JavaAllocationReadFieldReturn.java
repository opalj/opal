package org.opalj.fpcf.fixtures.xl.js.dataflow.unidirectional;

import org.opalj.fpcf.fixtures.xl.js.controlflow.interprocedural.unidirectional.JavaScriptAllocationReturn;
import org.opalj.fpcf.properties.pts.JavaMethodContextAllocSite;
import org.opalj.fpcf.properties.pts.JavaScriptContextAllocSite;
import org.opalj.fpcf.properties.pts.PointsToSet;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

/**
 * javascript returns value generated in js
 *
 * https://github.com/ngthanhtrung23/ACM_Notebook_new/blob/43d56ded57cc681f965fcb737d02db9f23ac22e7/Java/JS.java#L12
 *
 */
public class JavaAllocationReadFieldReturn {
    @PointsToSet(variableDefinition = 35,
            expectedJavaScriptAllocSites = {
                    @JavaScriptContextAllocSite(
                            cf = JavaAllocationReadFieldReturn.class,
                            nodeIdTAJS = 1,
                            allocatedType = ""
                    )
            }
    )
    public static void main(String args[]) throws ScriptException, NoSuchMethodException {
        JavaAllocationReadFieldReturn instance = new JavaAllocationReadFieldReturn();
        ScriptEngineManager sem = new ScriptEngineManager();
        ScriptEngine se = sem.getEngineByName("JavaScript");
        se.put("instance", instance);
        String mystring = "flag";
        se.eval("var n = {'a':'b'}");
        Object g = se.get("n");
        System.out.println(g);

    }

}

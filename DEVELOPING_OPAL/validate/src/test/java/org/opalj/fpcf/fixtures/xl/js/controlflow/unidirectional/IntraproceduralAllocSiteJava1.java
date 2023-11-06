package org.opalj.fpcf.fixtures.xl.js.controlflow.unidirectional;

import org.opalj.fpcf.fixtures.xl.js.objects.javatype.SimpleContainerClass;
import org.opalj.fpcf.properties.pts.JavaMethodContextAllocSite;
import org.opalj.fpcf.properties.pts.PointsToSet;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.util.Map;

public class IntraproceduralAllocSiteJava1 {
    @PointsToSet(variableDefinition = 29,
            expectedJavaAllocSites = {
                    @JavaMethodContextAllocSite(
                            cf = IntraproceduralAllocSiteJava1.class,
                            methodName = "main",
                            methodDescriptor = "(java.lang.String[]): void",
                            allocSiteLinenumber = 26,
                            allocatedType = "org.opalj.fpcf.fixtures.xl.js.objects.javatype.SimpleContainerClass")
            }
    )
    public static void main(String args[]) throws ScriptException, NoSuchMethodException {
        ScriptEngineManager sem = new ScriptEngineManager();
        ScriptEngine se = sem.getEngineByName("JavaScript");
        SimpleContainerClass w = new SimpleContainerClass();
        se.put("w", w);
        se.eval("var n = w;");
        Object p = se.get("n");
        System.out.println(p);
    }
}

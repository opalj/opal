package org.opalj.fpcf.fixtures.xl.js.testpts;

import org.opalj.fpcf.properties.pts.JavaMethodContextAllocSite;
import org.opalj.fpcf.properties.pts.PointsToSet;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.util.Map;

public class JavaType {
    @PointsToSet(variableDefinition = 25,
            expectedJavaAllocSites = {
                    @JavaMethodContextAllocSite(
                            cf = JavaType.class,
                            methodName = "main",
                            methodDescriptor = "(java.lang.String[]): void",
                            allocSiteLinenumber = 24,
                            allocatedType = "java.lang.String")
            }
    )
    public static void main(String args[]) throws ScriptException, NoSuchMethodException {
        SimpleContainerClass w = new SimpleContainerClass();
        w.s = "teststring";
        String ws = w.s;
        System.out.println(ws);
    }
}

package org.opalj.fpcf.fixtures.xl.js.objects.javatype;

import org.opalj.fpcf.properties.pts.JavaMethodContextAllocSite;
import org.opalj.fpcf.properties.pts.PointsToSet;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.util.Map;

public class JavaType {
    @PointsToSet(variableDefinition = 36,
            expectedJavaAllocSites = {
                    @JavaMethodContextAllocSite(
                            cf = JavaType.class,
                            methodName = "main",
                            methodDescriptor = "(java.lang.String[]): void",
                            allocSiteLinenumber = 27,
                            allocatedType = "java.lang.String")
            }
    )
    public static void main(String args[]) throws ScriptException, NoSuchMethodException {
        ScriptEngineManager sem = new ScriptEngineManager();
        ScriptEngine se = sem.getEngineByName("JavaScript");
        se.put("p", new Object());
        SimpleContainerClass w = new SimpleContainerClass();
        w.s = "teststring";
        se.eval("var n = 3; var HashMap = Java.type(\"java.util.HashMap\"); var mapDef = new HashMap(); var a = p; var o = {n:5}; var b = 7;mapDef[4]=9;");
        Object p = se.get("p");
        System.out.println(p);
        Object a = se.get("mapDef");
        System.out.println(a.getClass());
        Map hm = (Map)a;
        System.out.println(hm.size());
        hm.entrySet().forEach(o -> System.out.println(o));
        String ws = w.s;
        System.out.println(ws);
    }
}

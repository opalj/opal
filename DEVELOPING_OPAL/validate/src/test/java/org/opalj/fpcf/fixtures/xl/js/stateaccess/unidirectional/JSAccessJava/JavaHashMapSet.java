package org.opalj.fpcf.fixtures.xl.js.stateaccess.unidirectional.JSAccessJava;

import org.opalj.fpcf.properties.pts.JavaMethodContextAllocSite;
import org.opalj.fpcf.properties.pts.PointsToSet;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.util.HashMap;

/**
 * javascript sets value on hashmap
 *
 * https://github.com/cliff363825/TwentyFour/blob/a1822ad9682a6b363b3d1612052dfc716c077484/01_Language/03_Java/j2se/src/main/java/com/onevgo/j2se/script/ScriptMain.java#L5
 */
public class JavaHashMapSet {
    @PointsToSet(variableDefinition = 36,
            expectedJavaAllocSites = {
            @JavaMethodContextAllocSite(
                cf = JavaHashMapSet.class,
                    methodName = "main",
                    methodDescriptor = "(java.lang.String[]): void",
                    allocSiteLinenumber = 32,
                    allocatedType = "java.lang.Object")

            }
    )
    public static void main(String args[]) throws ScriptException, NoSuchMethodException {
        ScriptEngineManager sem = new ScriptEngineManager();
        ScriptEngine se = sem.getEngineByName("JavaScript");
        HashMap<String, Object> map = new HashMap<>();
        Object obj = new Object();
        se.put("map", map);
        se.put("obj", obj);
        se.eval("map.a = obj");
        Object obj2 = map.get("a");
        System.out.println(obj2);
    }
}

package org.opalj.fpcf.fixtures.xl.js.controlflow.unidirectional;

import org.opalj.fpcf.properties.pts.JavaScriptContextAllocSite;
import org.opalj.fpcf.properties.pts.PointsToSet;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

public class InterproceduralAllocSiteJS1 {
    @PointsToSet(variableDefinition = 19,
            expectedJavaScriptAllocSites = @JavaScriptContextAllocSite(
                    evalCallSource = InterproceduralAllocSiteJS1.class,
                    evalCallLineNumber = 25,
                    allocatedType = ""
            )
    )
    public static void main(String args[]) throws ScriptException, NoSuchMethodException {
        Object p = evaluateScript();
    }

    public static Object evaluateScript() throws ScriptException {
        ScriptEngineManager sem = new ScriptEngineManager();
        ScriptEngine se = sem.getEngineByName("JavaScript");
        se.eval("var x = {'a':10}; var y = x;");
        Object p = se.get("y");
    }
}

package org.opalj.fpcf.fixtures.xl.js.controlflow.unidirectional;

import org.opalj.fpcf.properties.pts.JavaScriptContextAllocSite;
import org.opalj.fpcf.properties.pts.PointsToSet;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

public class IntraproceduralAllocSiteJS1 {
    @PointsToSet(variableDefinition = 22,
            expectedJavaScriptAllocSites = @JavaScriptContextAllocSite(
                    evalCallSource = IntraproceduralAllocSiteJS1.class,
                    evalCallLineNumber = 21,
                    allocatedType = ""
            )
    )
    public static void main(String args[]) throws ScriptException, NoSuchMethodException {
        ScriptEngineManager sem = new ScriptEngineManager();
        ScriptEngine se = sem.getEngineByName("JavaScript");
        se.eval("var x = {'a':10}; var y = x;");
        Object p = se.get("y");
    }
}

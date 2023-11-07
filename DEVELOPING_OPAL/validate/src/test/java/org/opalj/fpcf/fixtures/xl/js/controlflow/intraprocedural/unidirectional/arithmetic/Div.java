package org.opalj.fpcf.fixtures.xl.js.controlflow.intraprocedural.unidirectional.arithmetic;

import org.opalj.fpcf.properties.pts.JavaScriptContextAllocSite;
import org.opalj.fpcf.properties.pts.PointsToSet;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

public class Div {
    @PointsToSet(variableDefinition = 25,
            expectedJavaScriptAllocSites = @JavaScriptContextAllocSite(
                    evalCallSource = Div.class,
                    evalCallLineNumber = 23,
                    allocatedType = ""
            )
    )

    public static void main(String args[]) throws ScriptException, NoSuchMethodException {
        ScriptEngineManager sem = new ScriptEngineManager();
        ScriptEngine se = sem.getEngineByName("JavaScript");
        se.eval("function div(a,b){return a / b;}");
        Invocable inv = (Invocable) se;
        Double result = (Double) inv.invokeFunction("div", 1, 3);
        System.out.println("result: " + result);
    }
}


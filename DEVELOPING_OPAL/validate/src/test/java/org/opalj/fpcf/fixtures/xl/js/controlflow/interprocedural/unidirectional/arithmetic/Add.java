package org.opalj.fpcf.fixtures.xl.js.controlflow.interprocedural.unidirectional.arithmetic;

import org.opalj.fpcf.properties.pts.JavaScriptContextAllocSite;
import org.opalj.fpcf.properties.pts.PointsToSet;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

public class Add {
    @PointsToSet(variableDefinition = 24,
            expectedJavaScriptAllocSites = @JavaScriptContextAllocSite(
                    evalCallSource = Add.class,
                    evalCallLineNumber = 22,
                    allocatedType = ""
            )
    )
    public static void main(String args[]) throws ScriptException, NoSuchMethodException {
        ScriptEngineManager sem = new ScriptEngineManager();
        ScriptEngine se = sem.getEngineByName("JavaScript");
        Integer x = 1;
        Integer y = 3 ;
        se.put("x",x);
        se.put("y",y);
        se.eval("function add(a,b){return a + b;} var z = add(x,y);");
        Double result = (Double) se.get("z");
        System.out.println("result: " + result);
    }
}


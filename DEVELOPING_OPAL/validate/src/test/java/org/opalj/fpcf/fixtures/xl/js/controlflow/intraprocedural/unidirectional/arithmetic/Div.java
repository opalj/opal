package org.opalj.fpcf.fixtures.xl.js.controlflow.intraprocedural.unidirectional.arithmetic;

import org.opalj.fpcf.properties.pts.JavaScriptContextAllocSite;
import org.opalj.fpcf.properties.pts.PointsToSet;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

public class Div {
    @PointsToSet(variableDefinition = 28,
            expectedJavaScriptAllocSites = {@JavaScriptContextAllocSite(
                    cf = Div.class,
                    nodeIdTAJS = -50,
                    allocatedType = "java.lang.Double"
            )}
    )

    public static void main(String args[]) throws ScriptException, NoSuchMethodException {
        ScriptEngineManager sem = new ScriptEngineManager();
        ScriptEngine se = sem.getEngineByName("JavaScript");
        Integer a = new Integer(1);
        Integer b = new Integer(3);
        se.put("a", a);
        se.put("b", b);
        se.eval("function div(a,b){return a / b;} var z = div(a,b);");
        Double result = (Double) se.get("z");
        System.out.println("result: " + result);
    }
}


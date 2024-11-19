/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.xl.js.controlflow.intraprocedural.unidirectional.arithmetic;

import org.opalj.fpcf.properties.pts.JavaScriptContextAllocSite;
import org.opalj.fpcf.properties.pts.PointsToSet;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

public class Add {

    @PointsToSet(variableDefinition = 26, expectedJavaScriptAllocSites = {@JavaScriptContextAllocSite(
            cf = Add.class,
            nodeIdTAJS = -50,
            allocatedType = "java.lang.Double"
    )})
    public static void main(String args[]) throws ScriptException, NoSuchMethodException {
        ScriptEngineManager sem = new ScriptEngineManager();
        ScriptEngine se = sem.getEngineByName("JavaScript");
        Integer x = new Integer(1);
        Integer y = new Integer(3);
        se.put("x",x);
        se.put("y",y);
        se.eval("function add(a,b){return a + b;} var z = add(x,y);");
        Double result = (Double) se.get("z");
        System.out.println("result: " + result);
    }
}


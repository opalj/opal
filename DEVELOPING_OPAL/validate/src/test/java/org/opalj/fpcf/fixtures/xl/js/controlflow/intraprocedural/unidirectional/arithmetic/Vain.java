/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.xl.js.controlflow.intraprocedural.unidirectional.arithmetic;

import org.opalj.fpcf.properties.pts.JavaScriptContextAllocSite;
import org.opalj.fpcf.properties.pts.PointsToSet;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

/**
 * broken test case?
 */
public class Vain {

    public static void main(String args[]) throws ScriptException, NoSuchMethodException {
        ScriptEngineManager sem = new ScriptEngineManager();
        ScriptEngine se = sem.getEngineByName("JavaScript");
        Integer a = new Integer(1);
        Integer b = new Integer(3);
        se.put("a", a);
        se.put("b", b);
        se.eval("var z = f(a,b);");
        Double result = (Double) se.get("z");
        System.out.println("result: " + result);
    }
}


package org.opalj.fpcf.fixtures.xl.js.controlflow.intraprocedural.cyclic;

import org.opalj.fpcf.properties.pts.JavaMethodContextAllocSite;
import org.opalj.fpcf.properties.pts.PointsToSet;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

/**
 * Cyclic execution: javascript calls java function that it was invoked in.
 * original example changed to use String (to allow pts tracking).
 * if pts were calculated correctly, pts for parameter should include defsite of value calculated below.
 */
public class Cyclic {

    public static void main(String[] args) throws ScriptException, NoSuchMethodException {
        Cyclic c = new Cyclic();
        String res = c.shorten("AAAAA");
        System.out.println();
    }
    @PointsToSet(variableDefinition = 35,
            expectedJavaAllocSites = {
                    @JavaMethodContextAllocSite(
                            cf = Cyclic.class,
                            methodName = "shorten",
                            methodDescriptor = "(java.lang.String): java.lang.String",
                            allocSiteLinenumber = 37,
                            allocatedType = "java.lang.String")
            }
    )
    public String shorten(String p) throws ScriptException, NoSuchMethodException {
        String param = p;
        System.out.println("parameter: "+param + " id: " + System.identityHashCode(param));
        String shortened = param.substring(1);
        System.out.println("shortened: "+ shortened + " id: " + System.identityHashCode(shortened));
        ScriptEngineManager sem = new ScriptEngineManager();
        ScriptEngine se = sem.getEngineByName("JavaScript");
        se.put("jThis", this);
        se.eval("function f(n){return jThis.shorten(n);}");
        if(!shortened.isEmpty()){
            Invocable inv = (Invocable) se;
             String result = (String)(inv.invokeFunction("f", shortened));
            return result;
        }
        else {
            System.out.println("n: " + shortened);
            return shortened;
        }
    }
}


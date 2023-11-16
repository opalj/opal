package org.opalj.fpcf.fixtures.xl.js.controlflow.interprocedural.cyclic;

import org.opalj.No;
import org.opalj.fpcf.fixtures.xl.js.controlflow.interprocedural.interleaved.JavaScriptCallsJavaFunctionOnPassedInstance;
import org.opalj.fpcf.properties.pts.JavaMethodContextAllocSite;
import org.opalj.fpcf.properties.pts.PointsToSet;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

/**
 * Cyclic execution: javascript calls java function that it was invoked in (interprocedural variation).
 * original example changed to use String (to allow pts tracking).
 * if pts were calculated correctly, pts for parameter should include defsite of value calculated below.
 */
public class Cyclic {
    ScriptEngine se;
    public static void main(String[] args) throws ScriptException, NoSuchMethodException {
        Cyclic c = new Cyclic();
        ScriptEngineManager sem = new ScriptEngineManager();
        c.se = sem.getEngineByName("JavaScript");
        c.se.put("jThis", c);
        c.se.eval("function f(n){return jThis.shorten(n);}");

        String res = c.shorten("AAAAA");
        System.out.println(res);
    }
    @PointsToSet(variableDefinition = 41,
            expectedJavaAllocSites = {
                    @JavaMethodContextAllocSite(
                            cf = Cyclic.class,
                            methodName = "shorten",
                            methodDescriptor = "(java.lang.String): java.lang.String",
                            allocSiteLinenumber = 43,
                            allocatedType = "java.lang.String")
            }
    )
    public String shorten(String p) throws ScriptException, NoSuchMethodException {
        String param = p;
        System.out.println("parameter: "+param + " id: " + System.identityHashCode(param));
        String shortened = param.substring(1);
        System.out.println("shortened: "+ shortened + " id: " + System.identityHashCode(shortened));

        if(!shortened.isEmpty()){
            String result =  callShortenThroughScriptEngine(shortened);
            return result;
        }
        else {
            System.out.println("n: " + shortened);
            return shortened;
        }
    }

    public String callShortenThroughScriptEngine(String s) throws ScriptException, NoSuchMethodException {
        se.put("arg", s);
        se.eval("var res = f(arg)");
        String result = (String) se.get("res");
        return result;
        // Invocable inv = (Invocable) se;
        // String result = (String)(inv.invokeFunction("f", s));
        // return result;
    }
}


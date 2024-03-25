package org.opalj.fpcf.fixtures.xl.js.controlflow.intraprocedural.cyclic;

import org.opalj.fpcf.fixtures.xl.js.testpts.SimpleContainerClass;
import org.opalj.fpcf.properties.pts.JavaMethodContextAllocSite;
import org.opalj.fpcf.properties.pts.PointsToSet;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

/**
 * Cyclic execution: javascript calls java function that it was invoked in.
 * original example changed to use String (to allow pts tracking).
 * if pts were calculated correctly, pts for parameter should include defsite of value calculated below.
 */
public class CyclicWithJSFunction {

    public static void main(String[] args) throws ScriptException, NoSuchMethodException {
        CyclicWithJSFunction c = new CyclicWithJSFunction();
        SimpleContainerClass initial = new SimpleContainerClass();
        SimpleContainerClass res = c.decrement(initial);
        System.out.println();
    }
    @PointsToSet(parameterIndex = 0,
            expectedJavaAllocSites = {
                    @JavaMethodContextAllocSite(
                            cf = CyclicWithJSFunction.class,
                            methodName = "decrement",
                            methodDescriptor = "(org.opalj.fpcf.fixtures.xl.js.testpts.SimpleContainerClass): org.opalj.fpcf.fixtures.xl.js.testpts.SimpleContainerClass",
                            allocSiteLinenumber = 37,
                            allocatedType = "org.opalj.fpcf.fixtures.xl.js.testpts.SimpleContainerClass")
            }
    )
    public SimpleContainerClass decrement(SimpleContainerClass p) throws ScriptException, NoSuchMethodException {
        SimpleContainerClass param = p;
        System.out.println("parameter: "+param + " id: " + System.identityHashCode(param));
        SimpleContainerClass decremented = new SimpleContainerClass();
        decremented.n = param.n - 1;
        System.out.println("decremented: "+ decremented + " id: " + System.identityHashCode(decremented));
        ScriptEngineManager sem = new ScriptEngineManager();
        ScriptEngine se = sem.getEngineByName("JavaScript");
        se.put("jThis", this);
        if(decremented.n > 0){
            se.put("arg", decremented);
            se.eval("function f(n){return jThis.decrement(n);}; var res = f(arg)");
            SimpleContainerClass result = (SimpleContainerClass) se.get("res");
            return result;
            //Invocable inv = (Invocable) se;
            //SimpleContainerClass result = (SimpleContainerClass)(inv.invokeFunction("f", decremented));
            //return result;
        }
        else {
            System.out.println("n: " + decremented);
            return decremented;
        }
    }
}

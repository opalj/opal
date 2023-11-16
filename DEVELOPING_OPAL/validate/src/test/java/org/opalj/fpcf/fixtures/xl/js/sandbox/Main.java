package org.opalj.fpcf.fixtures.xl.js.sandbox;

import org.opalj.fpcf.fixtures.xl.js.controlflow.interprocedural.unidirectional.JavaAllocationJSIdentity;
import org.opalj.fpcf.properties.pts.JavaMethodContextAllocSite;
import org.opalj.fpcf.properties.pts.PointsToSet;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

public class Main {
    @PointsToSet(variableDefinition = 30,
            expectedJavaAllocSites = {
                    @JavaMethodContextAllocSite(
                            cf = Main.class,
                            methodName = "main",
                            methodDescriptor = "(java.lang.String[]): void",
                            allocSiteLinenumber = 25,
                            allocatedType = "org.opalj.fpcf.fixtures.xl.js.sandbox.Person")
            }
    )
    public static void main(String args[]) throws ScriptException, NoSuchMethodException {
        ScriptEngineManager sem = new ScriptEngineManager();
        ScriptEngine se = sem.getEngineByName("JavaScript");
        Person p = new Person();
        Class c = new Class();
        se.put("p",p);
        se.put("c",c);
        se.eval("var z = c.identity(p);");
        Double result = (Double) se.get("z");
        System.out.println("result: " + result);
    }
}

package org.opalj.fpcf.fixtures.xl.js.controlflow.unidirectional;

import org.opalj.fpcf.fixtures.xl.js.objects.javatype.SimpleContainerClass;
import org.opalj.fpcf.properties.pts.JavaMethodContextAllocSite;
import org.opalj.fpcf.properties.pts.PointsToSet;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

public class InterproceduralAllocSiteJava1 {
    private ScriptEngine engine;
    public InterproceduralAllocSiteJava1(ScriptEngineManager sem) {
        engine = sem.getEngineByName("JavaScript");
    }
    public void setObject(Object o) {
        engine.put("w", o);
    }
    public Object evaluate() throws ScriptException {
        engine.eval("var n = w;");
        return engine.get("n");
    }
    @PointsToSet(variableDefinition = 38,
            expectedJavaAllocSites = {
                    @JavaMethodContextAllocSite(
                            cf = InterproceduralAllocSiteJava1.class,
                            methodName = "main",
                            methodDescriptor = "(java.lang.String[]): void",
                            allocSiteLinenumber = 36,
                            allocatedType = "org.opalj.fpcf.fixtures.xl.js.SimpleContainerClass")
            }
    )
    public static void main(String args[]) throws ScriptException, NoSuchMethodException {
        ScriptEngineManager sem = new ScriptEngineManager();
        InterproceduralAllocSiteJava1 engineContainer = new InterproceduralAllocSiteJava1(sem);
        SimpleContainerClass s = new SimpleContainerClass();
        engineContainer.setObject(s);
        Object out = engineContainer.evaluate();
    }


}

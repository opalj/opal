package org.opalj.fpcf.fixtures.xl.js.controlflow.interprocedural.unidirectional;

import org.opalj.fpcf.fixtures.xl.js.testpts.SimpleContainerClass;
import org.opalj.fpcf.properties.pts.JavaMethodContextAllocSite;
import org.opalj.fpcf.properties.pts.PointsToSet;
import org.opalj.xl.javaanalyses.benchmark.bidirectional.execution.Wrapper;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

/**
 * EngineWrapper contains an instance of ScriptEngine. calls to eval and put are wrapped.
 * Real world test cases:
 * https://github.com/MyRobotLab/myrobotlab/blob/3b12214657191d80fac696bf6ff0ac70317042d3/src/main/java/org/myrobotlab/service/JavaScript.java#L58
 * https://github.com/Free2Free/hutool/blob/7dc9078a99f2eb5bfff4e02ae0d67ef362449484/hutool-script/src/main/java/cn/hutool/script/JavaScriptEngine.java#L96
 */
public class EngineWrapper {
    private ScriptEngine engine;
    public EngineWrapper(ScriptEngineManager sem) {
        engine = sem.getEngineByName("JavaScript");
    }
    public void setObject(Object o) {
        engine.put("w", o);
    }
    public Object evaluate() throws ScriptException {
        engine.eval("var n = w;");
        return engine.get("n");
    }
    @PointsToSet(variableDefinition = 46,
            expectedJavaAllocSites = {
                    @JavaMethodContextAllocSite(
                            cf = EngineWrapper.class,
                            methodName = "main",
                            methodDescriptor = "(java.lang.String[]): void",
                            allocSiteLinenumber = 44,
                            allocatedType = "org.opalj.fpcf.fixtures.xl.js.testpts.SimpleContainerClass")
            }
    )
    public static void main(String args[]) throws ScriptException, NoSuchMethodException {
        ScriptEngineManager sem = new ScriptEngineManager();
        EngineWrapper engineContainer = new EngineWrapper(sem);
        SimpleContainerClass s = new SimpleContainerClass();
        engineContainer.setObject(s);
        Object out = engineContainer.evaluate();
        System.out.println(out.getClass());
    }


    public static class Mult2 {
        public static void main(String args[]) throws ScriptException, NoSuchMethodException {
            ScriptEngineManager sem = new ScriptEngineManager();
            ScriptEngine se = sem.getEngineByName("JavaScript");
            Wrapper wrapper = new Wrapper();
            String n = wrapper.s;
            System.out.println(n);
            se.put("jThis", wrapper);
            se.eval("function id(o){return o;}; var a = 3;");
            Invocable inv = (Invocable) se;
            Integer result = (Integer) inv.invokeFunction("id", new Object());

            System.out.println("result: " + result);
        }
    }
}

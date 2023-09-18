package org.opalj.xl.javaanalyses.benchmark.bidirectional.execution;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

public class Mult {
    public static void main(String args[]) throws ScriptException, NoSuchMethodException {
        ScriptEngineManager sem = new ScriptEngineManager();
        ScriptEngine se = sem.getEngineByName("JavaScript");
        Wrapper wrapper = new Wrapper();
        se.put("jThis", wrapper);
        se.eval("function mult(a,b){return jThis.add(1,3);}");
        Invocable inv = (Invocable) se;
        Integer result = (Integer) inv.invokeFunction("mult", 1, 3);
        System.out.println("result: " + result);
    }
}

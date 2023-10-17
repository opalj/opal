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
        String n = wrapper.s;
        se.put("jThis", wrapper);
        se.eval("function id(o){return o;}; var a = 3; var o = jThis;");
        Invocable inv = (Invocable) se;
        Integer result = (Integer) inv.invokeFunction("id", new Object());

        System.out.println("result: " + result);
    }
}

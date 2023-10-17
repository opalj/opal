package org.opalj.xl.javaanalyses.benchmark.objects;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

public class Test {
    public static void main(String args[]) throws ScriptException, NoSuchMethodException {
        ScriptEngineManager sem = new ScriptEngineManager();
        ScriptEngine se = sem.getEngineByName("JavaScript");
        se.eval("function mult(a,b){return a*b;}");
        Invocable inv = (Invocable) se;
        Integer result = (Integer) inv.invokeFunction("mult", 1, 3);
        System.out.println("result: " + result);
    }
}


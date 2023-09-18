package org.opalj.xl.javaanalyses.benchmark.unidirectional.execution;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

public class Identity {
    public static void main(String args[]) throws ScriptException, NoSuchMethodException {
        ScriptEngineManager sem = new ScriptEngineManager();
        ScriptEngine se = sem.getEngineByName("JavaScript");
        se.eval("function identity(n){return n;}");
        Invocable inv = (Invocable) se;
        int result = (int) inv.invokeFunction("identity", 5);
        System.out.println("result: " + result);
    }
}


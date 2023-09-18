package org.opalj.xl.javaanalyses.benchmark.unidirectional.execution.arithmetic;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

public class Vain {
    public static void main(String args[]) throws ScriptException, NoSuchMethodException {
        ScriptEngineManager sem = new ScriptEngineManager();
        ScriptEngine se = sem.getEngineByName("JavaScript");
        Invocable inv = (Invocable) se;
        Double result = (Double) inv.invokeFunction("f", 1, 3);
        System.out.println("result: " + result);
    }
}


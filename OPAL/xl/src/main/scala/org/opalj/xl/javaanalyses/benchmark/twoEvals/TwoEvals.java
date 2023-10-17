package org.opalj.xl.javaanalyses.benchmark.twoEvals;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

public class TwoEvals {
    public static void main(String args[]) throws ScriptException, NoSuchMethodException {
        /*ScriptEngineManager sem = new ScriptEngineManager();
        ScriptEngine se = sem.getEngineByName("JavaScript");
        se.put("u", 11);
        se.eval("function f(n){return n;}");
        se.eval("function f(n){return n*2;}");
        se.eval("u = 8;");
        Invocable inv = (Invocable) se;
        System.out.println(inv.invokeFunction("f",5));
        System.out.println(se.get("u"));*/
        ScriptEngineManager sem = new ScriptEngineManager();
        ScriptEngine se = sem.getEngineByName("JavaScript");
        se.eval("function f(n){return n+1;}");
        se.eval("function g(n){return f(n)*2;}");
        Invocable inv = (Invocable) se;
        System.out.println(inv.invokeFunction("f",5));
    }
}

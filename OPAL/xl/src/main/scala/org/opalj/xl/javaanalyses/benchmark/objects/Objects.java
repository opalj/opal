package org.opalj.xl.javaanalyses.benchmark.objects;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

public class Objects {
    public static void main(String args[]) throws ScriptException, NoSuchMethodException {
        ScriptEngineManager sem = new ScriptEngineManager();
        ScriptEngine se = sem.getEngineByName("JavaScript");
        se.put("p", new Object());
        se.eval("var o1 = {n:3}; var o2 = {n:4}; var o = {}; if(Math.random()==3) o = o1; else o = o2; var a = {a:3, b:5}; "); //var HashMap = Java.type(\"java.util.HashMap\");var mapDef = true");
        se.eval("function f(n){return n;}");
        Invocable inv = (Invocable) se;
        inv.invokeFunction("f",5);
        Object a = se.get("p");
        // System.out.println(f);
        System.out.println(a.getClass());
    }
}




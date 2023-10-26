package org.opalj.xl.javaanalyses.benchmark.objects;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

public class JavaType {
    public static void main(String args[]) throws ScriptException, NoSuchMethodException {
        ScriptEngineManager sem = new ScriptEngineManager();
        ScriptEngine se = sem.getEngineByName("JavaScript");
        Object p = new Object();
        se.put("p", p);
        se.eval("var n = 3; var HashMap = Java.type('java.util.HashMap'); var p = new HashMap(); var o = {n:5}; var b = 7;");
        Object a = se.get("p");
        System.out.println(a.getClass());
    }
}

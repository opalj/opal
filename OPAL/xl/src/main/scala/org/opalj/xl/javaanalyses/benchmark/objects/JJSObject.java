package org.opalj.xl.javaanalyses.benchmark.objects;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

public class JJSObject {
    public static void main(String args[]) throws ScriptException, NoSuchMethodException {
        ScriptEngineManager sem = new ScriptEngineManager();
        ScriptEngine se = sem.getEngineByName("JavaScript");
        se.put("p", new Object());
        se.eval("var HashMap = Java.type(\"java.util.HashMap\");var mapDef = new HashMap(); var mapDef = new HashMap(); if(Math.random()==3) mapDef = {n:5};");
        Object a = se.get("mapDef");
        System.out.println(a.getClass());
    }
}

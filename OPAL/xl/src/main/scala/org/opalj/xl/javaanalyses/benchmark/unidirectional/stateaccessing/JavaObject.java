package org.opalj.xl.javaanalyses.benchmark.unidirectional.stateaccessing;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

public class JavaObject {

    public static void main(String args[]) throws ScriptException, NoSuchMethodException {
        ScriptEngineManager sem = new ScriptEngineManager();
        ScriptEngine se = sem.getEngineByName("JavaScript");
        se.eval("var variable = 3; var HashMap = Java.type('java.util.HashMap'); var map = new HashMap(); map.put('hello', 'world');map;");
        Object javaObject = (Object) se.get("variable");
        System.out.println("Java Object: " + javaObject);
    }
}

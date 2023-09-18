package org.opalj.xl.javaanalyses.benchmark.unidirectional.stateaccessing;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.util.LinkedList;

public class PutGetObject {

    public static void main(String args[]) throws ScriptException, NoSuchMethodException {
        ScriptEngineManager sem = new ScriptEngineManager();
        ScriptEngine se = sem.getEngineByName("JavaScript");
        se.put("javaObject", new Object());
        se.put("linkedList", new LinkedList<>());
        se.put("linkedList2", new LinkedList<>());
        se.eval("var a = 3;");
        Object javaObject = (Object) se.get("javaObject");
        System.out.println("Java Object: " + javaObject);
    }
}

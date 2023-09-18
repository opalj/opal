package org.opalj.xl.javaanalyses.benchmark.bidirectional.stateaccessing;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

public class PutEvalGet {
    public static void main(String args[]) throws ScriptException, NoSuchMethodException {
        ScriptEngineManager sem = new ScriptEngineManager();
        ScriptEngine se = sem.getEngineByName("JavaScript");
        se.put("p", new Person());
        se.eval("p.number = 3; let name = p.name; function add(a, b) {return a + b;}");
        Person p = (Person) se.get("p");
        String name = (String) se.get("name");
        System.out.println("p.number: " + p.number);
        System.out.println("name: " + name);
    }
}


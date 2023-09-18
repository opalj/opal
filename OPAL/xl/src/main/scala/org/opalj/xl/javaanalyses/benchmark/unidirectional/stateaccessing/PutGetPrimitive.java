package org.opalj.xl.javaanalyses.benchmark.unidirectional.stateaccessing;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

public class PutGetPrimitive {

    public static void main(String args[]) throws ScriptException, NoSuchMethodException {
        ScriptEngineManager sem = new ScriptEngineManager();
        ScriptEngine se = sem.getEngineByName("JavaScript");
        se.put("variable", 7);
        int variable = (int) se.get("variable");
        System.out.println("variable: " + variable);
    }
}

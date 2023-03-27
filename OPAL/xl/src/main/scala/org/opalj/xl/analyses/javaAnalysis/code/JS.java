package org.opalj.xl.analyses.javaAnalysis.code;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

public class JS {

    public static void main(String args[]) throws ScriptException {
        ScriptEngineManager sem = new ScriptEngineManager();
        ScriptEngine se = sem.getEngineByName("JavaScript");
        Password pw = new Password();

        se.put("secret", pw);
        se.eval("var x = 42;");
        String fromJS = (String) se.get("secret");
        System.out.println("fromJS: " + fromJS);
    }
}

class Password{}

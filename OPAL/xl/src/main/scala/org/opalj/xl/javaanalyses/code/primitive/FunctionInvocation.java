package org.opalj.xl.javaanalyses.code.primitive;

import javax.script.*;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

public class FunctionInvocation {

    public static void main(String args[]) throws ScriptException, NoSuchMethodException {
        ScriptEngineManager sem = new ScriptEngineManager();
        ScriptEngine se = sem.getEngineByName("nashorn");
        se.eval("function add(a,b) {return a +b;}\n var n = 3;");
       System.out.println(se.eval("3"));
        /* Invocable invocable = (Invocable) se;
        Integer i1 = Integer.valueOf(3);
        Integer i2 = Integer.valueOf(4);
        Double result = (Double) invocable.invokeFunction("add", i1, i2);
        int result2 = (int) se.get("n");
        System.out.println("result: " + result);
        System.out.println("result2: " + result2); */
    }
}


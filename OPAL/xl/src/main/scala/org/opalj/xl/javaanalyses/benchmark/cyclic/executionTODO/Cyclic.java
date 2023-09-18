package org.opalj.xl.javaanalyses.benchmark.cyclic.executionTODO;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

public class Cyclic {

    public int f(int n) throws ScriptException, NoSuchMethodException {
        System.out.println("n: "+n);
        n = n -1;
        ScriptEngineManager sem = new ScriptEngineManager();
        ScriptEngine se = sem.getEngineByName("JavaScript");
        se.put("jThis", this);
        se.eval("function f(n){return jThis.f(n);}");
        if(n>0){
            Invocable inv = (Invocable) se;
             int result = (int)(inv.invokeFunction("f", n));
            return result;
        }
        else {
            System.out.println("n: " + n);
            return n;
        }
    }
}


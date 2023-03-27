package org.opalj.xl.analyses.javaAnalysis.code;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

public class JSDeclarationExample {

    public static void main(String args[]) throws ScriptException {
        ScriptEngineManager sem = new ScriptEngineManager();
        ScriptEngine se = sem.getEngineByName("JavaScript");
        se.eval("var n = 8; var i = 5; var a = n+i; n = a; const q = 5; console.log(q); const myCar = {\n" +
                "  make: \"Ford\",\n" +
                "  model: \"Mustang\",\n" +
                "  year: 1969,\n" +
                "};\n" +
                "myCar.age = 5;");
        String fromJS = (String) se.get("secret");
        System.out.println("fromJS: " + fromJS);
    }
}


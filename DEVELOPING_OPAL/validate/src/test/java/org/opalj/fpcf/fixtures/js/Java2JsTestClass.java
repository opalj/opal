package org.opalj.fpcf.fixtures.js;

import org.opalj.fpcf.properties.taint.ForwardFlowPath;

import javax.script.*;

public class Java2JsTestClass {
    /* Test flows through Javascript. */

    @ForwardFlowPath({"flowThroughJS"})
    public static void flowThroughJS() throws ScriptException
    {
        ScriptEngineManager sem = new ScriptEngineManager();
        ScriptEngine se = sem.getEngineByName("JavaScript");
        String pw = source();

        se.put("secret", pw);
        se.eval("var x = 42;");
        String fromJS = (String) se.get("secret");
        sink(fromJS);
    }

    @ForwardFlowPath({})
    public static void jsOverwritesBinding() throws ScriptException
    {
        ScriptEngineManager sem = new ScriptEngineManager();
        ScriptEngine se = sem.getEngineByName("JavaScript");
        String pw = source();

        se.put("secret", pw);
        se.eval("secret = \"42\";");
        String fromJS = (String) se.get("secret");
        sink(fromJS);
    }

    @ForwardFlowPath({"flowInsideJS"})
    public static void flowInsideJS() throws ScriptException
    {
        ScriptEngineManager sem = new ScriptEngineManager();
        ScriptEngine se = sem.getEngineByName("JavaScript");
        String pw = source();

        se.put("secret", pw);
        se.eval("var xxx = secret;");
        String fromJS = (String) se.get("xxx");
        sink(fromJS);
    }

    @ForwardFlowPath({"flowInsideJS2"})
    public static void flowInsideJS2() throws ScriptException
    {
        ScriptEngineManager sem = new ScriptEngineManager();
        ScriptEngine se = sem.getEngineByName("JavaScript");
        String pw = source();

        se.put("secret", pw);
        se.eval("var xxx = secret;" +
                "var yyy = xxx;");
        String fromJS = (String) se.get("yyy");
        sink(fromJS);
    }

    @ForwardFlowPath({})
    public static void flowInsideJSLateOverwrite() throws ScriptException
    {
        ScriptEngineManager sem = new ScriptEngineManager();
        ScriptEngine se = sem.getEngineByName("JavaScript");
        String pw = source();

        se.put("secret", pw);
        se.eval("var xxx = secret;" +
                "var xxx = 42;");
        String fromJS = (String) se.get("yyy");
        sink(fromJS);
    }

    @ForwardFlowPath({"jsInvokeIdentity"})
    public static void jsInvokeIdentity() throws ScriptException, NoSuchMethodException
    {
        ScriptEngineManager sem = new ScriptEngineManager();
        ScriptEngine se = sem.getEngineByName("JavaScript");
        String pw = source();

        se.eval("function id(x) { return x; }");
        String fromJS = (String) ((Invocable) se).invokeFunction("id", pw);
        sink(fromJS);
    }

    @ForwardFlowPath({})
    public static void jsInvokeOverwrite() throws ScriptException, NoSuchMethodException
    {
        ScriptEngineManager sem = new ScriptEngineManager();
        ScriptEngine se = sem.getEngineByName("JavaScript");
        String pw = source();

        se.eval("function overwrite(x) { return \"42\"; }");
        String fromJS = (String) ((Invocable) se).invokeFunction("id", pw);
        sink(fromJS);
    }

    @ForwardFlowPath({"jsInvokeWithComputation"})
    public static void jsInvokeWithComputation() throws ScriptException, NoSuchMethodException
    {
        ScriptEngineManager sem = new ScriptEngineManager();
        ScriptEngine se = sem.getEngineByName("JavaScript");
        se.eval("function check(str) {\n" +
                "    return str === \"1337\";\n" +
                "}");

        String pw = source();

        Invocable inv = (Invocable) se;
        Boolean state = (Boolean) inv.invokeFunction("check", pw);
        sink(state);
    }

    @ForwardFlowPath({})
    public static void jsUnusedTaintedParameter() throws ScriptException, NoSuchMethodException
    {
        ScriptEngineManager sem = new ScriptEngineManager();
        ScriptEngine se = sem.getEngineByName("JavaScript");
        se.eval("function check(str, unused) {\n" +
                "    return str === \"1337\";\n" +
                "}");

        String pw = source();
        Invocable inv = (Invocable) se;
        Boolean state = (Boolean) inv.invokeFunction("check", "1337", pw);
        sink(state);
    }

    /* More advanced flows. */

    @ForwardFlowPath({"jsInterproceduralFlow"})
    public static void jsInterproceduralFlow() throws ScriptException, NoSuchMethodException
    {
        ScriptEngineManager sem = new ScriptEngineManager();
        ScriptEngine se = sem.getEngineByName("JavaScript");

        se.eval("function add42(x) {" +
                "    return x + 42;" +
                "}" +
                "function id(x) {" +
                "    return x;" +
                "}" +
                "function check(str) {\n" +
                "    return id(add42(str)) === id(\"1337\");\n" +
                "}");
        String pw = source();
        Invocable inv = (Invocable) se;
        Boolean state = (Boolean) inv.invokeFunction("check", pw);
        sink(state);
    }

    @ForwardFlowPath({"jsFunctionFlow"})
    public static void jsFunctionFlow() throws ScriptException, NoSuchMethodException
    {
        ScriptEngineManager sem = new ScriptEngineManager();
        ScriptEngine se = sem.getEngineByName("JavaScript");

        se.eval("function myfun(x) {\n" +
                "    var xxx = x;\n" +
                "    return xxx;\n" +
                "}\n");
        String pw = source();
        Invocable inv = (Invocable) se;
        String value = (String) inv.invokeFunction("myfun", pw);
        sink(value);
    }

    /* Test flows through ScriptEngine objects. */

    @ForwardFlowPath({"simplePutGet"})
    public static void simplePutGet() throws ScriptException
    {
        ScriptEngineManager sem = new ScriptEngineManager();
        ScriptEngine se = sem.getEngineByName("JavaScript");

        String pw = source();
        se.put("secret", pw);
        Object out = se.get("secret");
        sink(out);
    }

    @ForwardFlowPath({"overapproxPutGet"})
    public static void overapproxPutGet() throws ScriptException
    {
        ScriptEngineManager sem = new ScriptEngineManager();
        ScriptEngine se = sem.getEngineByName("JavaScript");

        String pw = source();
        // String is no constant
        se.put(Integer.toString(1337), pw);
        // Because the .put had no constant string, we do not know the key here
        // and taint the return as an over-approximation.
        Object out = se.get("secret");
        sink(out);
    }

    @ForwardFlowPath({})
    public static void overwritePutGet() throws ScriptException
    {
        ScriptEngineManager sem = new ScriptEngineManager();
        ScriptEngine se = sem.getEngineByName("JavaScript");

        String pw = source();
        // String is no constant
        se.put("secret", pw);
        se.put("secret", "Const");
        Object out = se.get("secret");
        sink(out);
    }

    @ForwardFlowPath({"bindingsSimplePutGet"})
    public static void bindingsSimplePutGet() throws ScriptException
    {
        ScriptEngineManager sem = new ScriptEngineManager();
        ScriptEngine se = sem.getEngineByName("JavaScript");
        Bindings b = se.createBindings();

        String pw = source();
        se.put("secret", pw);
        Object out = se.get("secret");
        sink(out);
    }

    @ForwardFlowPath({"bindingsOverapproxPutGet"})
    public static void bindingsOverapproxPutGet() throws ScriptException
    {
        ScriptEngineManager sem = new ScriptEngineManager();
        ScriptEngine se = sem.getEngineByName("JavaScript");
        Bindings b = se.createBindings();

        String pw = source();
        // String is no constant
        se.put(Integer.toString(1337), pw);
        // Because the .put had no constant string, we do not know the key here
        // and taint the return as an over-approximation.
        Object out = se.get("secret");
        sink(out);
    }

    @ForwardFlowPath({})
    public static void BindingsOverwritePutGet() throws ScriptException
    {
        ScriptEngineManager sem = new ScriptEngineManager();
        ScriptEngine se = sem.getEngineByName("JavaScript");
        Bindings b = se.createBindings();

        String pw = source();
        se.put("secret", pw);
        se.put("secret", "Const");
        Object out = se.get("secret");
        sink(out);
    }

    @ForwardFlowPath({})
    public static void bindingsPutRemoveGet() throws ScriptException
    {
        ScriptEngineManager sem = new ScriptEngineManager();
        ScriptEngine se = sem.getEngineByName("JavaScript");
        Bindings b = se.createBindings();

        String pw = source();
        b.put("secret", pw);
        b.remove("secret");
        Object out = b.get("secret");
        sink(out);
    }

    @ForwardFlowPath({})
    public static void overwritePutRemoveGet() throws ScriptException
    {
        ScriptEngineManager sem = new ScriptEngineManager();
        ScriptEngine se = sem.getEngineByName("JavaScript");
        Bindings b = se.createBindings();

        String pw = source();
        b.put("secret", pw);
        b.remove("secret");
        Object out = b.get("secret");
        sink(out);
    }

    @ForwardFlowPath({"bindingsPutAll"})
    public static void bindingsPutAll() throws ScriptException
    {
        ScriptEngineManager sem = new ScriptEngineManager();
        ScriptEngine se = sem.getEngineByName("JavaScript");
        Bindings b = se.createBindings();

        String pw = source();
        b.put("secret", pw);
        Bindings newb = se.createBindings();
        newb.putAll(b);
        Object out = newb.get("secret");
        sink(out);
    }

    @ForwardFlowPath({"interproceduralPutGet"})
    public static void interproceduralPutGet() throws ScriptException
    {
        ScriptEngineManager sem = new ScriptEngineManager();
        ScriptEngine se = sem.getEngineByName("JavaScript");

        String pw = source();
        se.put("secret", pw);
        id (se);
        Object out = se.get("secret");
        sink(out);
    }

    @ForwardFlowPath({})
    public static void interproceduralOverwrite() throws ScriptException
    {
        ScriptEngineManager sem = new ScriptEngineManager();
        ScriptEngine se = sem.getEngineByName("JavaScript");

        String pw = source();
        se.put("secret", pw);
        removeSecret (se);
        Object out = se.get("secret");
        sink(out);
    }

    public static Object id(Object obj) {
        return obj;
    }

    public static void removeSecret(ScriptEngine se) {
        se.put("secret", 42);
    }

    public static String source() {
        return "1337";
    }

    private static void sink(String i) {
        System.out.println(i);
    }

    private static void sink(Object i) {
        System.out.println(i);
    }
}

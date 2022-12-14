package org.opalj.fpcf.fixtures.python;

import org.opalj.fpcf.properties.taint.ForwardFlowPath;

import javax.script.*;

public class Java2PythonTestClass {
    /* Test flows through Python. */

//    @ForwardFlowPath({"flowThroughPython"})
//    public static void flowThroughPython() throws ScriptException
//    {
//        ScriptEngineManager sem = new ScriptEngineManager();
//        ScriptEngine se = sem.getEngineByName("python");
//        String pw = source();
//
//        se.put("secret", pw);
//        se.eval("x = 42");
//        String fromPy = (String) se.get("secret");
//        sink(fromPy);
//    }

    @ForwardFlowPath({})
    public static void pythonOverwritesBinding() throws ScriptException
    {
        ScriptEngineManager sem = new ScriptEngineManager();
        ScriptEngine se = sem.getEngineByName("python");
        String pw = source();

        se.put("secret", pw);
        se.eval("secret = \"42\"");
        String fromPy = (String) se.get("secret");
        sink(fromPy);
    }

//    @ForwardFlowPath({"flowInsidePY"})
//    public static void flowInsidePY() throws ScriptException
//    {
//        ScriptEngineManager sem = new ScriptEngineManager();
//        ScriptEngine se = sem.getEngineByName("python");
//        String pw = source();
//
//        se.put("secret", pw);
//        se.eval("xxx = secret");
//        String fromPy = (String) se.get("xxx");
//        sink(fromPy);
//    }

//    @ForwardFlowPath({"flowInsidePY2"})
//    public static void flowInsidePY2() throws ScriptException
//    {
//        ScriptEngineManager sem = new ScriptEngineManager();
//        ScriptEngine se = sem.getEngineByName("python");
//        String pw = source();
//
//        se.put("secret", pw);
//        se.eval("xxx = secret\n" +
//                "yyy = xxx");
//        String fromPy = (String) se.get("yyy");
//        sink(fromPy);
//    }

    @ForwardFlowPath({})
    public static void flowInsidePyLateOverwrite() throws ScriptException
    {
        ScriptEngineManager sem = new ScriptEngineManager();
        ScriptEngine se = sem.getEngineByName("python");
        String pw = source();

        se.put("secret", pw);
        se.eval("xxx = secret" +
                "xxx = 42");
        String fromPy = (String) se.get("yyy");
        sink(fromPy);
    }

//    @ForwardFlowPath({"pyInvokeIdentity"})
//    public static void pyInvokeIdentity() throws ScriptException, NoSuchMethodException
//    {
//        ScriptEngineManager sem = new ScriptEngineManager();
//        ScriptEngine se = sem.getEngineByName("python");
//        String pw = source();
//
//        se.eval("def id(x): return x");
//        String fromPy = (String) ((Invocable) se).invokeFunction("id", pw);
//        sink(fromPy);
//    }

    @ForwardFlowPath({})
    public static void pyInvokeOverwrite() throws ScriptException, NoSuchMethodException
    {
        ScriptEngineManager sem = new ScriptEngineManager();
        ScriptEngine se = sem.getEngineByName("python");
        String pw = source();

        se.eval("def overwrite(x): return \"42\"");
        String fromPy = (String) ((Invocable) se).invokeFunction("id", pw);
        sink(fromPy);
    }

//    @ForwardFlowPath({"pyInvokeWithComputation"})
//    public static void pyInvokeWithComputation() throws ScriptException, NoSuchMethodException
//    {
//        ScriptEngineManager sem = new ScriptEngineManager();
//        ScriptEngine se = sem.getEngineByName("python");
//        se.eval("def check(str): return str == \"1337\"");
//
//        String pw = source();
//
//        Invocable inv = (Invocable) se;
//        Boolean state = (Boolean) inv.invokeFunction("check", pw);
//        sink(state);
//    }

    @ForwardFlowPath({})
    public static void pyUnusedTaintedParameter() throws ScriptException, NoSuchMethodException
    {
        ScriptEngineManager sem = new ScriptEngineManager();
        ScriptEngine se = sem.getEngineByName("python");
        se.eval("def check(str, unused): return str == \"1337\"");

        String pw = source();
        Invocable inv = (Invocable) se;
        Boolean state = (Boolean) inv.invokeFunction("check", "1337", pw);
        sink(state);
    }

    /* More advanced flows. */

//    @ForwardFlowPath({"pyInterproceduralFlow"})
//    public static void pyInterproceduralFlow() throws ScriptException, NoSuchMethodException
//    {
//        ScriptEngineManager sem = new ScriptEngineManager();
//        ScriptEngine se = sem.getEngineByName("python");
//
//        se.eval("def add42(x): return int(x) + 42 \n" +
//                "def id(x): return x \n" +
//                "def check(str): return id(add42(str)) == id(\"1337\")");
//        String pw = source();
//        Invocable inv = (Invocable) se;
//        Boolean state = (Boolean) inv.invokeFunction("check", pw);
//        sink(state);
//    }

//    @ForwardFlowPath({"pyFunctionFlow"})
//    public static void pyFunctionFlow() throws ScriptException, NoSuchMethodException
//    {
//        ScriptEngineManager sem = new ScriptEngineManager();
//        ScriptEngine se = sem.getEngineByName("python");
//
//        se.eval("def myfun(x):\n" +
//                " xxx = x\n" +
//                " return xxx");
//        String pw = source();
//        Invocable inv = (Invocable) se;
//        String value = (String) inv.invokeFunction("myfun", pw);
//        sink(value);
//    }

    /* Test flows through ScriptEngine objects. */

    @ForwardFlowPath({"simplePutGet"})
    public static void simplePutGet() throws ScriptException
    {
        ScriptEngineManager sem = new ScriptEngineManager();
        ScriptEngine se = sem.getEngineByName("python");

        String pw = source();
        se.put("secret", pw);
        Object out = se.get("secret");
        sink(out);
    }

    @ForwardFlowPath({"overapproxPutGet"})
    public static void overapproxPutGet() throws ScriptException
    {
        ScriptEngineManager sem = new ScriptEngineManager();
        ScriptEngine se = sem.getEngineByName("python");

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
        ScriptEngine se = sem.getEngineByName("python");

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
        ScriptEngine se = sem.getEngineByName("python");
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
        ScriptEngine se = sem.getEngineByName("python");
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
        ScriptEngine se = sem.getEngineByName("python");
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
        ScriptEngine se = sem.getEngineByName("python");
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
        ScriptEngine se = sem.getEngineByName("python");
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
        ScriptEngine se = sem.getEngineByName("python");
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
        ScriptEngine se = sem.getEngineByName("python");

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
        ScriptEngine se = sem.getEngineByName("python");

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

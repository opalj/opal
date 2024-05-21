package org.opalj.fpcf.fixtures.xl.llvm.controlflow.interprocedural.interleaved;

import javax.script.ScriptException;

/**
 * Java Function Called from native.
 */
public class CallJavaFunctionFromNative {

    public static void main(String args[]) throws ScriptException, NoSuchMethodException {
        Object o = new Object();
        CallJavaFunctionFromNative tb = new CallJavaFunctionFromNative();
        tb.callMyJavaFunctionFromNative(o);
    }
    public void f(Object x) {
        callMyJavaFunctionFromNative(x);
    }
    public native void callMyJavaFunctionFromNative(Object x);


    public void myJavaFunction(Object x) {
        System.out.println(x);
    }
}

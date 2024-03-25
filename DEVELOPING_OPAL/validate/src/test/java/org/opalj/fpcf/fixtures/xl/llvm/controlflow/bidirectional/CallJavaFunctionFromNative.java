package org.opalj.fpcf.fixtures.xl.llvm.controlflow.bidirectional;

import org.opalj.fpcf.properties.pts.JavaMethodContextAllocSite;
import org.opalj.fpcf.properties.pts.PointsToSet;

import javax.script.ScriptException;

/**
 * Native identity function called, String passed, return value should be string.
 */
public class CallJavaFunctionFromNative {

    public static void main(String args[]) throws ScriptException, NoSuchMethodException {
        Object o = new Object();
        CallJavaFunctionFromNative tb = new CallJavaFunctionFromNative();
        tb.callMyFunctionFromNative(o);
    }

    public native void callMyFunctionFromNative(Object x);

    @PointsToSet(parameterIndex = 0,
            expectedJavaAllocSites = {
                    @JavaMethodContextAllocSite(
                            cf = CallJavaFunctionFromNative.class,
                            methodName = "main",
                            methodDescriptor = "(java.lang.String[]): void",
                            allocSiteLinenumber = 14,
                            allocatedType = "java.lang.String")
            }
    )
    public void myJavaFunction(Object x) {
        System.out.println(x);
    }
}
/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.xl.llvm.controlflow.interprocedural.interleaved;

import org.opalj.fpcf.properties.pts.JavaMethodContextAllocSite;
import org.opalj.fpcf.properties.pts.PointsToSet;

import javax.script.ScriptException;

/**
 * Java function called from native, String passed, return value should be string.
 */
public class CallJavaFunctionFromNativeAndReturn2 {
    @PointsToSet(variableDefinition = 26,
            expectedJavaAllocSites = {
                    @JavaMethodContextAllocSite(
                            cf = CallJavaFunctionFromNativeAndReturn2.class,
                            methodName = "main",
                            methodDescriptor = "(java.lang.String[]): void",
                            allocSiteLinenumber = 24,
                            allocatedType = "java.lang.Object")
            }
    )
    public static void main(String args[]) throws ScriptException, NoSuchMethodException {
        Object o = new Object();
        CallJavaFunctionFromNativeAndReturn2 tb = new CallJavaFunctionFromNativeAndReturn2();
        Object o2 = tb.f(o);
        System.out.println(o2);
    }
    public Object f(Object x) {
        return callMyJavaFunctionFromNative(x);
    }
    public native Object callMyJavaFunctionFromNative(Object x);

    public Object myReturningJavaFunction1(Object x) {
        System.out.println(x);
        return x;
    }

    public Object myReturningJavaFunction2(Object x) {
        System.out.println(x);
        return x;
    }
}

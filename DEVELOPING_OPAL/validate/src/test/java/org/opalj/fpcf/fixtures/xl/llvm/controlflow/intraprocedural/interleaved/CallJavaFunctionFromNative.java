/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.xl.llvm.controlflow.intraprocedural.interleaved;

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
        tb.callMyJavaFunctionFromNative(o);
    }

    public native void callMyJavaFunctionFromNative(Object x);

    @PointsToSet(parameterIndex = 0,
            expectedJavaAllocSites = {
                    @JavaMethodContextAllocSite(
                            cf = CallJavaFunctionFromNative.class,
                            methodName = "main",
                            methodDescriptor = "(java.lang.String[]): void",
                            allocSiteLinenumber = 15,
                            allocatedType = "java.lang.Object")
            }
    )
    public void myJavaFunction(Object x) {
        System.out.println(x);
    }
}

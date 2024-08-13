/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.xl.llvm.controlflow.intraprocedural.interleaved;

import org.opalj.fpcf.fixtures.xl.llvm.stateaccess.interprocedural.unidirectional.CAccessJava.ReadJavaFieldFromNative;
import org.opalj.fpcf.properties.pts.JavaMethodContextAllocSite;
import org.opalj.fpcf.properties.pts.PointsToSet;

import javax.script.ScriptException;

/**
 * Java function called from native, String passed, return value should be string.
 */
public class CallJavaFunctionFromNativeAndReturn {
    @PointsToSet(variableDefinition = 27,
            expectedJavaAllocSites = {
                    @JavaMethodContextAllocSite(
                            cf = CallJavaFunctionFromNativeAndReturn.class,
                            methodName = "main",
                            methodDescriptor = "(java.lang.String[]): void",
                            allocSiteLinenumber = 25,
                            allocatedType = "java.lang.Object")
            }
    )
    public static void main(String args[]) throws ScriptException, NoSuchMethodException {
        Object o = new Object();
        CallJavaFunctionFromNativeAndReturn tb = new CallJavaFunctionFromNativeAndReturn();
        Object o2 = tb.callMyJavaFunctionFromNative(o);
        System.out.println(o2);
    }
    public native Object callMyJavaFunctionFromNative(Object x);

    public Object myReturningJavaFunction(Object x) {
        System.out.println(x);
        return x;
    }
}

/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.xl.llvm.controlflow.interprocedural.interleaved;

import org.opalj.fpcf.properties.pts.JavaMethodContextAllocSite;
import org.opalj.fpcf.properties.pts.PointsToSet;

import javax.script.ScriptException;

/**
 * Class is instantiated from native, instance method is called on object. Interprocedural indirection in both Java and C
 */
public class CreateJavaInstanceFromNative {

    public static void main(String[] args) throws ScriptException, NoSuchMethodException {
        Object o = new Object();
        f(o);
    }
    public static void f(Object x) {
        CreateJavaInstanceFromNative.createInstanceAndCallMyFunctionFromNative(x);

    }
    public static native void createInstanceAndCallMyFunctionFromNative(Object x);

    @PointsToSet(parameterIndex = 0,
            expectedJavaAllocSites = {
                    @JavaMethodContextAllocSite(
                            cf = CreateJavaInstanceFromNative.class,
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

package org.opalj.fpcf.fixtures.xl.llvm.controlflow.unidirectional;

import org.opalj.fpcf.properties.pts.JavaMethodContextAllocSite;
import org.opalj.fpcf.properties.pts.PointsToSet;

import javax.script.ScriptException;

/**
 * Native identity function called, String passed, return value should be string.
 */
public class NativeIdentityFunction {
    @PointsToSet(variableDefinition = 29,
            expectedJavaAllocSites = {
            @JavaMethodContextAllocSite(
                cf = NativeIdentityFunction.class,
                    methodName = "main",
                    methodDescriptor = "(java.lang.String[]): void",
                    allocSiteLinenumber = 28,
                    allocatedType = "java.lang.String")

            }
    )
    public static void main(String args[]) throws ScriptException, NoSuchMethodException {
        String o = "Hello";
        Object x = identity(o);
        System.out.println(x);
    }

    public static native Object identity(Object x);
}

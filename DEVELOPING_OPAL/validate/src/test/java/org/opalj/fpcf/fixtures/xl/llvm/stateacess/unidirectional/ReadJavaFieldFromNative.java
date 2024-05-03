package org.opalj.fpcf.fixtures.xl.llvm.stateacess.unidirectional;

import org.opalj.fpcf.properties.pts.JavaMethodContextAllocSite;
import org.opalj.fpcf.properties.pts.PointsToSet;

import javax.script.ScriptException;

/**
 * Native function reads field, returns value
 */
public class ReadJavaFieldFromNative {
    Object myfield;
    @PointsToSet(parameterIndex = 0,
            expectedJavaAllocSites = {
                    @JavaMethodContextAllocSite(
                            cf = ReadJavaFieldFromNative.class,
                            methodName = "main",
                            methodDescriptor = "(java.lang.String[]): void",
                            allocSiteLinenumber = 24,
                            allocatedType = "java.lang.Object")
            }
    )
    public static void main(String[] args) throws ScriptException, NoSuchMethodException {
        Object o = new Object();
        ReadJavaFieldFromNative tb = new ReadJavaFieldFromNative();
        tb.myfield = o;
        Object x = tb.getMyfield(o);
        System.out.println(x);
    }

    public native Object getMyfield(Object x);
}

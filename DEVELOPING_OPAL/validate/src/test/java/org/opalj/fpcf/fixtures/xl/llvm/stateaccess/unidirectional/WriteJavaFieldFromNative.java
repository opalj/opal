package org.opalj.fpcf.fixtures.xl.llvm.stateaccess.unidirectional;

import org.opalj.fpcf.properties.pts.JavaMethodContextAllocSite;
import org.opalj.fpcf.properties.pts.PointsToSet;

import javax.script.ScriptException;

/**
 * Native function takes parameter, sets instance field.
 */
public class WriteJavaFieldFromNative {
    Object myfield;
    @PointsToSet(parameterIndex = 0,
            expectedJavaAllocSites = {
                    @JavaMethodContextAllocSite(
                            cf = WriteJavaFieldFromNative.class,
                            methodName = "main",
                            methodDescriptor = "(java.lang.String[]): void",
                            allocSiteLinenumber = 24,
                            allocatedType = "java.lang.Object")
            }
    )
    public static void main(String[] args) throws ScriptException, NoSuchMethodException {
        Object o = new Object();
        WriteJavaFieldFromNative tb = new WriteJavaFieldFromNative();
        tb.setMyfield(o);
        Object x = tb.myfield;
        System.out.println(x);
    }

    public native void setMyfield(Object x);
}

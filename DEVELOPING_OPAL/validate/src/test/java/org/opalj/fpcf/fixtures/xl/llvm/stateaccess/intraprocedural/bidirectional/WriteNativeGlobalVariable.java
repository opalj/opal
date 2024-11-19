/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.xl.llvm.stateaccess.intraprocedural.bidirectional;

import org.opalj.fpcf.properties.pts.JavaMethodContextAllocSite;
import org.opalj.fpcf.properties.pts.PointsToSet;

import javax.script.ScriptException;

/**
 * Native function takes parameter, sets instance field. Interprocedural indirection in both Java and C
 */
public class WriteNativeGlobalVariable {
    Object myfield;
    @PointsToSet(variableDefinition = 28,
            expectedJavaAllocSites = {
                    @JavaMethodContextAllocSite(
                            cf = WriteNativeGlobalVariable.class,
                            methodName = "main",
                            methodDescriptor = "(java.lang.String[]): void",
                            allocSiteLinenumber = 25,
                            allocatedType = "java.lang.Object")
            }
    )
    public static void main(String[] args) throws ScriptException, NoSuchMethodException {
        Object o = new Object();
        WriteNativeGlobalVariable tb = new WriteNativeGlobalVariable();
        tb.setNativeGlobal(o);
        Object x = tb.getNativeGlobal();
        System.out.println(x);
    }
    public native void setNativeGlobal(Object x);
    public native Object getNativeGlobal();
}

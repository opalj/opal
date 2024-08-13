/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.xl.llvm.stateaccess.interprocedural.unidirectional.CAccessJava;

import org.opalj.fpcf.properties.pts.JavaMethodContextAllocSite;
import org.opalj.fpcf.properties.pts.PointsToSet;

import javax.script.ScriptException;

/**
 * Native function takes parameter, sets instance field. Interprocedural indirection in both Java and C
 */
public class WriteJavaFieldFromNative {
    Object myfield;
    @PointsToSet(variableDefinition = 28,
            expectedJavaAllocSites = {
                    @JavaMethodContextAllocSite(
                            cf = WriteJavaFieldFromNative.class,
                            methodName = "main",
                            methodDescriptor = "(java.lang.String[]): void",
                            allocSiteLinenumber = 25,
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
    public void setSetMyField(Object x) {
        setMyfield(x);
    }
    public native void setMyfield(Object x);
}

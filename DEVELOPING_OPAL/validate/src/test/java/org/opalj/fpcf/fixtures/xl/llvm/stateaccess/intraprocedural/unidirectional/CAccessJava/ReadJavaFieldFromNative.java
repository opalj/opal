/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.xl.llvm.stateaccess.intraprocedural.unidirectional.CAccessJava;

import org.opalj.fpcf.properties.pts.JavaMethodContextAllocSite;
import org.opalj.fpcf.properties.pts.PointsToSet;

import javax.script.ScriptException;

/**
 * Native function reads field, returns value
 */
public class ReadJavaFieldFromNative {
    Object myfield;
    @PointsToSet(variableDefinition = 28,
            expectedJavaAllocSites = {
                    @JavaMethodContextAllocSite(
                            cf = ReadJavaFieldFromNative.class,
                            methodName = "main",
                            methodDescriptor = "(java.lang.String[]): void",
                            allocSiteLinenumber = 25,
                            allocatedType = "java.lang.Object")
            }
    )
    public static void main(String[] args) throws ScriptException, NoSuchMethodException {
        Object o = new Object();
        ReadJavaFieldFromNative tb = new ReadJavaFieldFromNative();
        tb.myfield = o;
        Object x = tb.getMyfield();
        System.out.println(x);
    }

    public native Object getMyfield();
}

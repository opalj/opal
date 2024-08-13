/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.xl.llvm.controlflow.interprocedural.cyclic;

import org.opalj.fpcf.fixtures.xl.js.testpts.SimpleContainerClass;
import org.opalj.fpcf.properties.pts.JavaMethodContextAllocSite;
import org.opalj.fpcf.properties.pts.PointsToSet;

import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

public class CyclicRecursion {
    public static void main(String[] args) throws ScriptException, NoSuchMethodException {
        CyclicRecursion c = new CyclicRecursion();
        SimpleContainerClass initial = new SimpleContainerClass();
        SimpleContainerClass res = c.decrement(initial);
        System.out.println(res);
    }
    @PointsToSet(parameterIndex = 0,
            expectedJavaAllocSites = {
                    @JavaMethodContextAllocSite(
                            cf = CyclicRecursion.class,
                            methodName = "decrement",
                            methodDescriptor = "(org.opalj.fpcf.fixtures.xl.js.testpts.SimpleContainerClass): org.opalj.fpcf.fixtures.xl.js.testpts.SimpleContainerClass",
                            allocSiteLinenumber = 31,
                            allocatedType = "org.opalj.fpcf.fixtures.xl.js.testpts.SimpleContainerClass")
            }
    )
    public SimpleContainerClass decrement(SimpleContainerClass p) throws ScriptException, NoSuchMethodException {
        SimpleContainerClass param = p;
        System.out.println("parameter: "+param + " id: " + System.identityHashCode(param));
        SimpleContainerClass decremented = new SimpleContainerClass(); // alloc site should be contained in arg0 pts.
        decremented.n = param.n - 1;
        System.out.println("decremented: "+ decremented + " id: " + System.identityHashCode(decremented));

        if(decremented.n > 0){
            SimpleContainerClass result =  callDecrementFromNative(decremented);
            return result;
        }
        else {
            System.out.println("n: " + decremented);
            return decremented;
        }
    }

    public native SimpleContainerClass callDecrementFromNative(SimpleContainerClass c);
}

package org.opalj.fpcf.fixtures.return_freshness;

import org.opalj.fpcf.properties.return_freshness.FreshReturnValue;
import org.opalj.fpcf.properties.return_freshness.NoFreshReturnValue;
import org.opalj.fpcf.properties.return_freshness.PrimitiveReturnValue;

public class BasicReturns {

    public static Object global;

    @PrimitiveReturnValue("the return value is primitive")
    public static int primitiveReturnValue(int i) {
        return 4 + i;
    }

    @FreshReturnValue("the object is created in this method")
    public static Object objectFactory() {
        return new Object();
    }

    @FreshReturnValue("the analysis should work with transitivity")
    public static Object transitiveObjectFactory() {
        return objectFactory();
    }

    @NoFreshReturnValue("a parameter is returned")
    public static Object returnParameter(Object param) {
        return param;
    }

    @NoFreshReturnValue("the fresh object escapes globally")
    public static Object escapingObject() {
        Object o = new Object();
        global = o;
        return o;
    }
}

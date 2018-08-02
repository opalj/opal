/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.return_freshness;

import org.opalj.fpcf.properties.return_freshness.FreshReturnValue;
import org.opalj.fpcf.properties.return_freshness.Getter;
import org.opalj.fpcf.properties.return_freshness.NoFreshReturnValue;
import org.opalj.fpcf.properties.return_freshness.PrimitiveReturnValue;

/**
 * Some basic test cases for the fresh return value analysis.
 *
 * @author Florian Kuebler
 */
public final class BasicReturns {

    public static Object global;

    private Object field = new Object();

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

    @NoFreshReturnValue("return value is fresh but gets assigned to static field")
    public static Object transitiveEscapingObject() {
        Object o = objectFactory();
        global = o;
        return o;
    }

    @FreshReturnValue("return value does not escape")
    public static Object transitiveNoEscape() {
        Object o = objectFactory();

        if (o instanceof BasicReturns) {
            System.out.println("OK");
        }

        return o;
    }

    @Getter("It is just a getter")
    public Object getField() {
        return field;
    }

}

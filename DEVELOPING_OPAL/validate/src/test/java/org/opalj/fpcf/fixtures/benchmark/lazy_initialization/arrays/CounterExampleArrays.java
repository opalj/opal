package org.opalj.fpcf.fixtures.benchmark.lazy_initialization.arrays;

import org.opalj.fpcf.properties.immutability.field_assignability.AssignableField;
import org.opalj.fpcf.properties.immutability.fields.MutableField;

/**
 * This class encompasses different counter examples of lazy initialized arrays.
 */
public class CounterExampleArrays {

    //@Immutable
    @MutableField("Field is assignable")
    @AssignableField("Multiple assignments possible")
    private Object[] array;

    public Object[] getArray(int n) {
        if (array == null || array.length < n) {
            this.array = new Object[n];
        }
        return array;
    }

    //@Immutable
    @MutableField("Field is assignable")
    @AssignableField("Field can be read with multiple values.")
    private Object[] b;

    public Object[] getB(boolean flag) throws Exception {
        if(b!=null)
            return b;
        else if(flag)
            return b; //throw new Exception("");
        else {
            this.b = new Object[5];
            return b;
        }
    }
}

/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.cifi_benchmark.lazy_initialization.arrays;

import org.opalj.fpcf.properties.immutability.field_assignability.AssignableField;
import org.opalj.fpcf.properties.immutability.fields.MutableField;

/**
 * This class encompasses different counter examples of lazy initialized arrays.
 */
public class CounterExampleArrays {

    @MutableField("Field is assignable")
    @AssignableField("Multiple assignments possible")
    private Object[] array;

    public Object[] getArray(int n) {
        if (array == null || array.length < n) {
            this.array = new Object[n];
        }
        return array;
    }

    @MutableField("Field is assignable")
    @AssignableField("Field can be read with multiple values.")
    private Object[] b;

    public Object[] getB(boolean flag) throws Exception {
        if(b!=null)
            return b;
        else if(flag)
            return b;
        else {
            this.b = new Object[5];
            return b;
        }
    }
}

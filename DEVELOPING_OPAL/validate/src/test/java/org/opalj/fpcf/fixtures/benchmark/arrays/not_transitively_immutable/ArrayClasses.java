/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.benchmark.arrays.not_transitively_immutable;

//import edu.cmu.cs.glacier.qual.Immutable;
import org.opalj.fpcf.properties.immutability.classes.MutableClass;
import org.opalj.fpcf.properties.immutability.fields.MutableField;
import org.opalj.fpcf.properties.immutability.fields.NonTransitivelyImmutableField;
import org.opalj.fpcf.properties.immutability.field_assignability.EffectivelyNonAssignableField;
import org.opalj.fpcf.properties.immutability.field_assignability.AssignableField;
import org.opalj.fpcf.properties.immutability.types.MutableType;

//@Immutable
@MutableType("")
@MutableClass("")
public class ArrayClasses<T> {

    //@Immutable
    @NonTransitivelyImmutableField("The elements of the array are manipulated after initialization and can escape.")
    @EffectivelyNonAssignableField("The array is eager initialized.")
    private Object[] b = new Object[]{1, 2, 3, 4, 5};

    public void setB() {
        b[2] = 2;
    }

    //@Immutable
    @MutableField("Array has a mutable reference.")
    @AssignableField("The array is initalized always when the InitC function is called")
    private Object[] c;

    public void InitC() {
        c = new Object[]{1, 2, 3};
    }

    //@Immutable
    @NonTransitivelyImmutableField("The elements of the array can escape.")
    @EffectivelyNonAssignableField("The array is eager initialized.")
    private Object[] d = new Object[]{1, 2, 3, 4, 5,};

    public Object[] getD() {
        return d;
    }



    @Override
    protected void finalize() throws Throwable {
        super.finalize();
    }

    //@Immutable
    @NonTransitivelyImmutableField("escaping")
    private String[] stringArray;

    //@Immutable
    @NonTransitivelyImmutableField("escaping")
    private int[] intArray;

    //@Immutable
    @NonTransitivelyImmutableField("escaping")
    private Object[] oArr;

    //@Immutable
    @NonTransitivelyImmutableField("escaping")
    private T[] tArray;

    ArrayClasses(String[] stringArray, int[] intArray, Object[] oArr, T[] tArray) {
        this.stringArray = stringArray;
        this.intArray = intArray;
        this.oArr = oArr;
        this.tArray = tArray;
    }
}

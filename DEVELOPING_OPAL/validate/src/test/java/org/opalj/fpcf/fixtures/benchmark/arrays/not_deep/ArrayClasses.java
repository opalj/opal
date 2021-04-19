/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.benchmark.arrays.not_deep;

//import edu.cmu.cs.glacier.qual.Immutable;
import org.opalj.fpcf.properties.immutability.classes.MutableClass;
import org.opalj.fpcf.properties.immutability.fields.MutableField;
import org.opalj.fpcf.properties.immutability.fields.ShallowImmutableField;
import org.opalj.fpcf.properties.immutability.references.ImmutableFieldReference;
import org.opalj.fpcf.properties.immutability.references.LazyInitializedNotThreadSafeFieldReference;
import org.opalj.fpcf.properties.immutability.references.LazyInitializedThreadSafeFieldReference;
import org.opalj.fpcf.properties.immutability.references.MutableFieldReference;
import org.opalj.fpcf.properties.immutability.types.MutableType;

//@Immutable
@MutableType("")
@MutableClass("")
public class ArrayClasses<T> {

    //@Immutable
    @ShallowImmutableField("The elements of the array are manipulated after initialization and can escape.")
    @ImmutableFieldReference("The array is eager initialized.")
    private Object[] b = new Object[]{1, 2, 3, 4, 5};

    public void setB() {
        b[2] = 2;
    }

    //@Immutable
    @MutableField("Array has a mutable reference.")
    @MutableFieldReference("The array is initalized always when the InitC function is called")
    private Object[] c;

    public void InitC() {
        c = new Object[]{1, 2, 3};
    }

    //@Immutable
    @ShallowImmutableField("The elements of the array can escape.")
    @ImmutableFieldReference("The array is eager initialized.")
    private Object[] d = new Object[]{1, 2, 3, 4, 5,};

    public Object[] getD() {
        return d;
    }



    @Override
    protected void finalize() throws Throwable {
        super.finalize();
    }

    //@Immutable
    @ShallowImmutableField("escaping")
    private String[] stringArray;

    //@Immutable
    @ShallowImmutableField("escaping")
    private int[] intArray;

    //@Immutable
    @ShallowImmutableField("escaping")
    private Object[] oArr;

    //@Immutable
    @ShallowImmutableField("escaping")
    private T[] tArray;

    ArrayClasses(String[] stringArray, int[] intArray, Object[] oArr, T[] tArray) {
        this.stringArray = stringArray;
        this.intArray = intArray;
        this.oArr = oArr;
        this.tArray = tArray;
    }
}

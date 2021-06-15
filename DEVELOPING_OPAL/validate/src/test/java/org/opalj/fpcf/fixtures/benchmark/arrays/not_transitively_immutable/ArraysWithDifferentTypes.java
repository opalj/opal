/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.benchmark.arrays.not_transitively_immutable;

//import edu.cmu.cs.glacier.qual.Immutable;
import org.opalj.fpcf.properties.immutability.classes.MutableClass;
import org.opalj.fpcf.properties.immutability.fields.MutableField;
import org.opalj.fpcf.properties.immutability.fields.NonTransitivelyImmutableField;
import org.opalj.fpcf.properties.immutability.field_assignability.EffectivelyNonAssignableField;
import org.opalj.fpcf.properties.immutability.field_assignability.AssignableField;
import org.opalj.fpcf.properties.immutability.types.MutableType;

/**
 * This class encompasses fields with different types of arrays.
 */
//@Immutable
@MutableType("")
@MutableClass("")
public class ArraysWithDifferentTypes<T> {

    //@Immutable
    @NonTransitivelyImmutableField("The elements of the array are manipulated after initialization.")
    @EffectivelyNonAssignableField("The array is eager initialized.")
    private final Object[] finalArrayWithSetterForOneElement = new Object[]{1, 2, 3, 4, 5};

    public void setB() {
        finalArrayWithSetterForOneElement[2] = 2;
    }

    //@Immutable
    @MutableField("Array has a mutable reference.")
    @AssignableField("The array is initalized always when the InitC function is called")
    private Object[] arrayWithAssignableRefeference;

    public void InitC() {
        arrayWithAssignableRefeference = new Object[]{1, 2, 3};
    }

    //@Immutable
    @NonTransitivelyImmutableField("The elements of the array can escape.")
    @EffectivelyNonAssignableField("The array is eager initialized.")
    private Object[] arrayThatCanEscapeViaGetter = new Object[]{1, 2, 3, 4, 5,};

    public Object[] getArrayThatCanEscapeViaGetter() {
        return arrayThatCanEscapeViaGetter;
    }



    @Override
    protected void finalize() throws Throwable {
        super.finalize();
    }

    //@Immutable
    @NonTransitivelyImmutableField("The array escapes via the constructor")
    @EffectivelyNonAssignableField("The field is initialized in the constructor")
    private String[] privateStringArrayEscapingViaConstructor;

    //@Immutable
    @NonTransitivelyImmutableField("The array escapes via the constructor")
    @EffectivelyNonAssignableField("The field is initialized in the constructor")
    private int[] privateIntArrayEscapingViaConstructor;

    //@Immutable
    @NonTransitivelyImmutableField("The array escapes via the constructor")
    @EffectivelyNonAssignableField("The field is initialized in the constructor")
    private Object[] privateObjectArrayEscapingViaConstructor;

    //@Immutable
    @NonTransitivelyImmutableField("The array escapes via the constructor")
    @EffectivelyNonAssignableField("The field is initialized in the constructor")
    private T[] privateTArrayEscapingViaConstructor;

    ArraysWithDifferentTypes(String[] stringArray, int[] intArray, Object[] objectArr, T[] tArray) {
        this.privateStringArrayEscapingViaConstructor = stringArray;
        this.privateIntArrayEscapingViaConstructor = intArray;
        this.privateObjectArrayEscapingViaConstructor = objectArr;
        this.privateTArrayEscapingViaConstructor = tArray;
    }
}

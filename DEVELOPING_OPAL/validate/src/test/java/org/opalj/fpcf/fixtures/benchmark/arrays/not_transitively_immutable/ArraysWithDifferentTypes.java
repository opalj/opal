/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.benchmark.arrays.not_transitively_immutable;

//import edu.cmu.cs.glacier.qual.Immutable;
import org.opalj.fpcf.fixtures.benchmark.commons.CustomObject;
import org.opalj.fpcf.properties.immutability.classes.MutableClass;
import org.opalj.fpcf.properties.immutability.field_assignability.NonAssignableField;
import org.opalj.fpcf.properties.immutability.fields.MutableField;
import org.opalj.fpcf.properties.immutability.fields.NonTransitivelyImmutableField;
import org.opalj.fpcf.properties.immutability.field_assignability.EffectivelyNonAssignableField;
import org.opalj.fpcf.properties.immutability.field_assignability.AssignableField;
import org.opalj.fpcf.properties.immutability.types.MutableType;

/**
 * This class encompasses fields with different types of arrays.
 */
//@Immutable
@MutableType("The class is mutable")
@MutableClass("The class has mutable fields")
public class ArraysWithDifferentTypes<T> {

    //@Immutable
    @NonTransitivelyImmutableField("The elements of the array are manipulated after initialization.")
    @NonAssignableField("The field is final")
    private final CustomObject[] finalArrayWithSetterForOneElement =
            new CustomObject[]{new CustomObject(), new CustomObject()};

    public void setB() {
        finalArrayWithSetterForOneElement[2] = new CustomObject();
    }

    //@Immutable
    @MutableField("Array is assignable.")
    @AssignableField("The array is initialized always when the InitC function is called")
    private CustomObject[] assignableArray;

    public void InitC() {
        assignableArray = new CustomObject[]{new CustomObject(), new CustomObject()};
    }

    //@Immutable
    @NonTransitivelyImmutableField("The elements of the array can escape.")
    @EffectivelyNonAssignableField("The array is eager initialized.")
    private CustomObject[] arrayThatCanEscapeViaGetter = new CustomObject[]{new CustomObject(), new CustomObject()};

    public CustomObject[] getArrayThatCanEscapeViaGetter() {
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
    private CustomObject[] privateObjectArrayEscapingViaConstructor;

    //@Immutable
    @NonTransitivelyImmutableField("The array escapes via the constructor")
    @EffectivelyNonAssignableField("The field is initialized in the constructor")
    private T[] privateTArrayEscapingViaConstructor;

    ArraysWithDifferentTypes(String[] stringArray, int[] intArray, CustomObject[] objectArr, T[] tArray) {
        this.privateStringArrayEscapingViaConstructor = stringArray;
        this.privateIntArrayEscapingViaConstructor = intArray;
        this.privateObjectArrayEscapingViaConstructor = objectArr;
        this.privateTArrayEscapingViaConstructor = tArray;
    }
}

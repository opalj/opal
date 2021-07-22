/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.cifi_benchmark.arrays.not_transitively_immutable;

import org.opalj.fpcf.fixtures.cifi_benchmark.common.CustomObject;
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
@MutableType("The class is mutable")
@MutableClass("The class has mutable fields")
public class ArraysWithDifferentTypes<T> {

    @NonTransitivelyImmutableField("The elements of the array are manipulated after initialization.")
    @NonAssignableField("The field is final")
    private final CustomObject[] finalArrayWithSetterForOneElement =
            new CustomObject[]{new CustomObject(), new CustomObject()};

    public void setB() {
        finalArrayWithSetterForOneElement[2] = new CustomObject();
    }

    
    @MutableField("Array is assignable.")
    @AssignableField("The array is initialized always when the InitC function is called")
    private CustomObject[] assignableArray;

    public void setC() {
        assignableArray = new CustomObject[]{new CustomObject(), new CustomObject()};
    }

    @NonTransitivelyImmutableField("The elements of the array can escape.")
    @EffectivelyNonAssignableField("The array is eager initialized.")
    private CustomObject[] arrayThatCanEscapeViaGetter = new CustomObject[]{new CustomObject(), new CustomObject()};

    public CustomObject[] getArrayThatCanEscapeViaGetter() {
        return arrayThatCanEscapeViaGetter;
    }

    @NonTransitivelyImmutableField("The array escapes via the constructor")
    @EffectivelyNonAssignableField("The field is initialized in the constructor")
    private String[] privateStringArrayEscapingViaConstructor;

    @NonTransitivelyImmutableField("The array escapes via the constructor")
    @EffectivelyNonAssignableField("The field is initialized in the constructor")
    private int[] privateIntArrayEscapingViaConstructor;

    
    @NonTransitivelyImmutableField("The array escapes via the constructor")
    @EffectivelyNonAssignableField("The field is initialized in the constructor")
    private CustomObject[] privateObjectArrayEscapingViaConstructor;

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

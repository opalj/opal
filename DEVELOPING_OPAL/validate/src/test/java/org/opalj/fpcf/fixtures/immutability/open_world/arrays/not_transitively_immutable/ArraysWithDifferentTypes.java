/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.immutability.open_world.arrays.not_transitively_immutable;

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
public class ArraysWithDifferentTypes {

    @NonTransitivelyImmutableField("The elements of the array are manipulated after initialization.")
    @NonAssignableField("The field is final")
    private final Object[] finalArrayWithSetterForOneElement =
            new Object[]{new Object(), new Object()};

    public void setB() {
        finalArrayWithSetterForOneElement[2] = new Object();
    }

    @MutableField("The array is assignable.")
    @AssignableField("The array is initialized always when the setC function is called")
    private Object[] assignableArray;

    public void setC() {
        assignableArray = new Object[]{new Object(), new Object()};
    }

    @NonTransitivelyImmutableField("The elements of the array can escape.")
    @EffectivelyNonAssignableField("The array is eager initialized.")
    private Object[] arrayThatCanEscapeViaGetter = new Object[]{new Object(), new Object()};

    public Object[] getArrayThatCanEscapeViaGetter() {
        return arrayThatCanEscapeViaGetter;
    }

    @NonTransitivelyImmutableField("The array escapes via the constructor")
    @EffectivelyNonAssignableField("The field is initialized in the constructor")
    private String[] privateStringArrayEscapingViaConstructor;

    @MutableField("Field is assignable")
    @AssignableField("Field is public")
    public Object publicObject = new Object();

    @NonTransitivelyImmutableField("Field is initialized with an non-transitively immutable array")
    @NonAssignableField("Field is final")
    private final Object[] arrayWithOneEscapingObject;

    @NonTransitivelyImmutableField("One object of the array can escape via a getter method")
    @NonAssignableField("Field is final")
    private final Object[] arrayAccessedByGetterMethod = new Object[]{new Object(), new Object()};

    public Object getSecondElement(){
        return arrayAccessedByGetterMethod[1];
    }

    ArraysWithDifferentTypes(String[] stringArray) {
        this.privateStringArrayEscapingViaConstructor = stringArray;
        arrayWithOneEscapingObject = new Object[]{publicObject};
    }
}

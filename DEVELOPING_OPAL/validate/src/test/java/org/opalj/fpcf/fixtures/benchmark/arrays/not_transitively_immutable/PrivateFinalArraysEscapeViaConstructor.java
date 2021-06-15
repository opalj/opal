/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.benchmark.arrays.not_transitively_immutable;

//import edu.cmu.cs.glacier.qual.Immutable;
import org.opalj.fpcf.properties.immutability.classes.NonTransitivelyImmutableClass;
import org.opalj.fpcf.properties.immutability.field_assignability.NonAssignableField;
import org.opalj.fpcf.properties.immutability.fields.NonTransitivelyImmutableField;
import org.opalj.fpcf.properties.immutability.types.NonTransitivelyImmutableType;

/**
 * This classes encompasses array-typed fields that are all assigned in the constructor.
 */
//@Immutable
@NonTransitivelyImmutableType("Class is non-transitively immutable and final")
@NonTransitivelyImmutableClass("Class has only transitively immutable fields")
public final class PrivateFinalArraysEscapeViaConstructor {

    //@Immutable
    @NonTransitivelyImmutableField("Array has a primitive type but is assigned in the constructor and, thus, escapes")
    @NonAssignableField("Array is final")
    private final char[] charArray;

    //@Immutable
    @NonTransitivelyImmutableField("Array has a primitive type but is assigned in the constructor and, thus, escapes")
    @NonAssignableField("Array is final")
    private final byte[] byteArray;

    //@Immutable
    @NonTransitivelyImmutableField("Array has a primitive type but is assigned in the constructor and, thus, escapes")
    @NonAssignableField("Array is final")
    private final int[] intArray;

    //@Immutable
    @NonTransitivelyImmutableField("Array has a primitive type but is assigned in the constructor and, thus, escapes")
    @NonAssignableField("Array is final")
    private final long[] longArray;

    //@Immutable
    @NonTransitivelyImmutableField("Array has a primitive type but is assigned in the constructor and, thus, escapes")
    @NonAssignableField("Array is final")
    private final Object[] objectArray;

    public PrivateFinalArraysEscapeViaConstructor(char[] charArray, byte[] byteArray, int[] intArray,
                                                  long[] longArray, Object[] objectArray) {
        this.charArray = charArray;
        this.byteArray = byteArray;
        this.intArray = intArray;
        this.longArray = longArray;
        this.objectArray = objectArray;
    }
}

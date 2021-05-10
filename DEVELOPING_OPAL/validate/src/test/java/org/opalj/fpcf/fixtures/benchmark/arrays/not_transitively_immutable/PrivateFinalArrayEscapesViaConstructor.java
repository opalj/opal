/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.benchmark.arrays.not_transitively_immutable;

//import edu.cmu.cs.glacier.qual.Immutable;
import org.opalj.fpcf.properties.immutability.classes.NonTransitivelyImmutableClass;
import org.opalj.fpcf.properties.immutability.fields.NonTransitivelyImmutableField;
import org.opalj.fpcf.properties.immutability.references.NonAssignableFieldReference;
import org.opalj.fpcf.properties.immutability.types.NonTransitiveImmutableType;

@NonTransitiveImmutableType("")
@NonTransitivelyImmutableClass("")
public final class PrivateFinalArrayEscapesViaConstructor {

    //@Immutable
    @NonTransitivelyImmutableField("")
    @NonAssignableFieldReference("")
    private final char[] charArray;

    //@Immutable
    @NonTransitivelyImmutableField("")
    @NonAssignableFieldReference("")
    private final byte[] byteArray;

    //@Immutable
    @NonTransitivelyImmutableField("")
    @NonAssignableFieldReference("")
    private final int[] intArray;

    //@Immutable
    @NonTransitivelyImmutableField("")
    @NonAssignableFieldReference("")
    private final long[] longArray;

    //@Immutable
    @NonTransitivelyImmutableField("")
    @NonAssignableFieldReference("")
    private final Object[] objectArray;

    public PrivateFinalArrayEscapesViaConstructor(char[] charArray, byte[] byteArray, int[] intArray,
                                                  long[] longArray, Object[] objectArray) {
        this.charArray = charArray;
        this.byteArray = byteArray;
        this.intArray = intArray;
        this.longArray = longArray;
        this.objectArray = objectArray;
    }
}

/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.benchmark.arrays.not_transitively_immutable;

//import edu.cmu.cs.glacier.qual.Immutable;
import org.opalj.fpcf.properties.immutability.classes.NonTransitivelyImmutableClass;
import org.opalj.fpcf.properties.immutability.fields.NonTransitivelyImmutableField;
import org.opalj.fpcf.properties.immutability.field_assignability.EffectivelyNonAssignableField;
import org.opalj.fpcf.properties.immutability.types.NonTransitiveImmutableType;

@NonTransitiveImmutableType("")
@NonTransitivelyImmutableClass("")
public final class PrivateFinalArrayEscapesViaConstructor {

    //@Immutable
    @NonTransitivelyImmutableField("")
    @EffectivelyNonAssignableField("")
    private final char[] charArray;

    //@Immutable
    @NonTransitivelyImmutableField("")
    @EffectivelyNonAssignableField("")
    private final byte[] byteArray;

    //@Immutable
    @NonTransitivelyImmutableField("")
    @EffectivelyNonAssignableField("")
    private final int[] intArray;

    //@Immutable
    @NonTransitivelyImmutableField("")
    @EffectivelyNonAssignableField("")
    private final long[] longArray;

    //@Immutable
    @NonTransitivelyImmutableField("")
    @EffectivelyNonAssignableField("")
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

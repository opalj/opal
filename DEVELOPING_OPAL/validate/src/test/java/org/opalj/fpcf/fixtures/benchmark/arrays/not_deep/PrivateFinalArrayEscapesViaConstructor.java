/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.benchmark.arrays.not_deep;

//import edu.cmu.cs.glacier.qual.Immutable;
import org.opalj.fpcf.properties.immutability.classes.ShallowImmutableClass;
import org.opalj.fpcf.properties.immutability.fields.ShallowImmutableField;
import org.opalj.fpcf.properties.immutability.references.ImmutableFieldReference;
import org.opalj.fpcf.properties.immutability.types.ShallowImmutableType;

@ShallowImmutableType("")
@ShallowImmutableClass("")
public final class PrivateFinalArrayEscapesViaConstructor {

    //@Immutable
    @ShallowImmutableField("")
    @ImmutableFieldReference("")
    private final char[] charArray;

    //@Immutable
    @ShallowImmutableField("")
    @ImmutableFieldReference("")
    private final byte[] byteArray;

    //@Immutable
    @ShallowImmutableField("")
    @ImmutableFieldReference("")
    private final int[] intArray;

    //@Immutable
    @ShallowImmutableField("")
    @ImmutableFieldReference("")
    private final long[] longArray;

    //@Immutable
    @ShallowImmutableField("")
    @ImmutableFieldReference("")
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

/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.benchmark.arrays;

import org.opalj.fpcf.properties.immutability.classes.ShallowImmutableClass;
import org.opalj.fpcf.properties.immutability.fields.ShallowImmutableField;
import org.opalj.fpcf.properties.immutability.references.ImmutableFieldReference;
import org.opalj.fpcf.properties.immutability.types.ShallowImmutableType;

@ShallowImmutableType("")
@ShallowImmutableClass("")
public final class PrivateFinalArrayEscapesViaConstructor {

    @ShallowImmutableField("")
    @ImmutableFieldReference("")
    private final char[] charArray;

    @ShallowImmutableField("")
    @ImmutableFieldReference("")
    private final byte[] byteArray;

    @ShallowImmutableField("")
    @ImmutableFieldReference("")
    private final int[] intArray;

    @ShallowImmutableField("")
    @ImmutableFieldReference("")
    private final long[] longArray;

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

/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.immutability.field_references;

import org.opalj.fpcf.properties.immutability.references.ImmutableFieldReference;

public class PrivateFinalArrayEscapesViaConstructor {

    @ImmutableFieldReference("")
    private final char[] charArray;

    @ImmutableFieldReference("")
    private final byte[] byteArray;

    @ImmutableFieldReference("")
    private final int[] intArray;

    @ImmutableFieldReference("")
    private final long[] longArray;

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

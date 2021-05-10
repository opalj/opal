/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.immutability.field_references;

import org.opalj.fpcf.properties.immutability.references.NonAssignableFieldReference;

public class PrivateFinalArrayEscapesViaConstructor {

    @NonAssignableFieldReference("")
    private final char[] charArray;

    @NonAssignableFieldReference("")
    private final byte[] byteArray;

    @NonAssignableFieldReference("")
    private final int[] intArray;

    @NonAssignableFieldReference("")
    private final long[] longArray;

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

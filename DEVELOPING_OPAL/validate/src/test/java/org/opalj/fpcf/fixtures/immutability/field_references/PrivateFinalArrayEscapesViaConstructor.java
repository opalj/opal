/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.immutability.field_references;

import org.opalj.fpcf.properties.immutability.field_assignability.EffectivelyNonAssignableField;

public class PrivateFinalArrayEscapesViaConstructor {

    @EffectivelyNonAssignableField("")
    private final char[] charArray;

    @EffectivelyNonAssignableField("")
    private final byte[] byteArray;

    @EffectivelyNonAssignableField("")
    private final int[] intArray;

    @EffectivelyNonAssignableField("")
    private final long[] longArray;

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

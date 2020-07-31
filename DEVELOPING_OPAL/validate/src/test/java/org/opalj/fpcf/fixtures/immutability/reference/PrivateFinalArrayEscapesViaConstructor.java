package org.opalj.fpcf.fixtures.immutability.reference;

import org.opalj.fpcf.properties.reference_immutability.ImmutableReferenceAnnotation;

public class PrivateFinalArrayEscapesViaConstructor {

    @ImmutableReferenceAnnotation("")
    private final char[] charArray;

    @ImmutableReferenceAnnotation("")
    private final byte[] byteArray;

    @ImmutableReferenceAnnotation("")
    private final int[] intArray;

    @ImmutableReferenceAnnotation("")
    private final long[] longArray;

    @ImmutableReferenceAnnotation("")
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

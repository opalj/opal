package org.opalj.fpcf.fixtures.immutability.reference;

import org.opalj.fpcf.properties.reference_immutability.ImmutableReferenceEscapesAnnotation;

public class PrivateFinalArrayEscapesViaConstructor {

    @ImmutableReferenceEscapesAnnotation("")
    private final char[] charArray;

    @ImmutableReferenceEscapesAnnotation("")
    private final byte[] byteArray;

    @ImmutableReferenceEscapesAnnotation("")
    private final int[] intArray;

    @ImmutableReferenceEscapesAnnotation("")
    private final long[] longArray;

    @ImmutableReferenceEscapesAnnotation("")
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

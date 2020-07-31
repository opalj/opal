package org.opalj.fpcf.fixtures.immutability.reference_immutability_lazy_initialization;

public class S {
    private final char value[];

    /** Cache the hash code for the string */

    //@LazyInitializedNotThreadSafeButDeterministicReferenceAnnotation("")
    private int hash; // Default to 0


    public S(S original) {
        this.value = original.value;
        this.hash = original.hash;
    }

    public int hashCode() {
        int h = hash;

        if (h == 0 && value.length > 0) {
            char val[] = value;

            for (int i = 0; i < value.length; i++) {
                h = 31 * h + val[i];
            }
            hash = h;
        }
        return h;
    }
}

package org.opalj.fpcf.fixtures.reference_immutability_sandbox;

import org.opalj.fpcf.properties.reference_immutability.ImmutableReferenceAnnotation;

class ExceptionInInitialization {

    /**
     * @note As the field write is dead, this field is really 'effectively final' as it will never
     * be different from the default value.
     */
    @ImmutableReferenceAnnotation("")
    private int x;

    private int getZero() {
        return 0;
    }

    public int init() {
        int y = this.x;
        if (y == 0) {
            int z = 10 /getZero();
            y = x = 5;
        }
        return y;
    }
}
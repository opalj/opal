/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.immutability.classes;

import org.opalj.fpcf.properties.immutability.references.LazyInitializedNotThreadSafeButDeterministicReference;
import org.opalj.fpcf.properties.immutability.references.MutableReference;

class PossibleExceptionInInitialization {

    @LazyInitializedNotThreadSafeButDeterministicReference("Incorrect because lazy initialization is may " +
            "not happen due to exception")
    private int x;

    public int init(int i) {
        int y = this.x;
        if (y == 0) {
            int z = 10 / i;
            y = x = 5;
        }
        return y;
    }
}

class CaughtExceptionInInitialization {

    @MutableReference("Incorrect because lazy initialization is may not happen due to exception")
    private int x;

    public int init(int i) {
        int y = this.x;
        try {
            if (y == 0) {
                int z = 10 / i;
                y = x = 5;
            }
            return y;
        } catch (Exception e) {
            return 0;
        }
    }
}
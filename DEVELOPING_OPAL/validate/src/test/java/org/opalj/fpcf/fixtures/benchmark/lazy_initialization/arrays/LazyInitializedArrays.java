package org.opalj.fpcf.fixtures.benchmark.lazy_initialization.arrays;

import org.opalj.fpcf.properties.immutability.field_assignability.LazyInitializedNotThreadSafeFieldReference;
import org.opalj.fpcf.properties.immutability.field_assignability.LazyInitializedThreadSafeFieldReference;

/**
 * This classes tests lazy initialization of arrays to the reference.
 * This definition of lazy initialization only considers the lazy initialization of the arrays and not of its elements.
 */
public class LazyInitializedArrays {

    //@Immutable
    @LazyInitializedNotThreadSafeFieldReference("The array is lazily initialized.")
    private int[] simpleLazyInitialized;

    public int[] getSimpleLazyInitialized() {
        if (simpleLazyInitialized == null) {
            simpleLazyInitialized = new int[]{1, 2, 3};
        }
        simpleLazyInitialized[1]  = 5;
        return simpleLazyInitialized;
    }

    //@Immutable
    @LazyInitializedThreadSafeFieldReference("The array is synchronized lazily initialized.")
    private int[] simpleSynchronizedLazyInitialized;

    public synchronized int[] getSynchronizedLazyInitialized() {
        if (simpleSynchronizedLazyInitialized == null) {
            simpleSynchronizedLazyInitialized = new int[]{1, 2, 3};
        }
        simpleSynchronizedLazyInitialized[1]  = 5;
        return simpleSynchronizedLazyInitialized;
    }

    //@Immutable
    @LazyInitializedThreadSafeFieldReference("The array is synchronized lazily initialized.")
    private int[] complexSynchronizedLazyInitialized;

    public synchronized int[] getComplexSynchronizedLazyInitialized() {
        int[] tmp;
        if (complexSynchronizedLazyInitialized == null) {
            tmp = new int[3];
            for (int i = 0; i < 3; i++)
                tmp[i] = i;
            this.complexSynchronizedLazyInitialized = tmp;
        }
        return this.complexSynchronizedLazyInitialized;
    }

    //@Immutable
    @LazyInitializedNotThreadSafeFieldReference("The array is lazily initialized.")
    private int[] complexLazyInitialized;

    public int[] getComplexLazyInitialized() {
        int[] tmp;
        if (complexLazyInitialized == null) {
            tmp = new int[3];
            for (int i = 0; i < 3; i++)
                tmp[i] = i;
            this.complexLazyInitialized = tmp;
        }
        return this.complexLazyInitialized;
    }

}

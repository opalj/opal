/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.immutability.openworld.lazyinitialization.arrays;

import org.opalj.fpcf.properties.immutability.classes.MutableClass;
import org.opalj.fpcf.properties.immutability.field_assignability.UnsafelyLazilyInitializedField;
import org.opalj.fpcf.properties.immutability.field_assignability.LazilyInitializedField;
import org.opalj.fpcf.properties.immutability.types.MutableType;

/**
 * This classes checks lazy initialization of arrays fields.
 * The definition of lazy initialization only considers the lazy initialization of the arrays and not of its elements.
 */
@MutableType("Class is mutable")
@MutableClass("Class has a mutable field")
public class LazyInitializedArrays {

    @UnsafelyLazilyInitializedField("The array is unsafely lazily initialized.")
    private int[] simpleLazyInitialized;

    public int[] getSimpleLazyInitialized() {
        if (simpleLazyInitialized == null) {
            simpleLazyInitialized = new int[]{1, 2, 3};
        }
        simpleLazyInitialized[1]  = 5;
        return simpleLazyInitialized;
    }

    @LazilyInitializedField("The array is synchronized lazily initialized.")
    private int[] simpleSynchronizedLazyInitialized;

    public synchronized int[] getSynchronizedLazyInitialized() {
        if (simpleSynchronizedLazyInitialized == null) {
            simpleSynchronizedLazyInitialized = new int[]{1, 2, 3};
        }
        simpleSynchronizedLazyInitialized[1]  = 5;
        return simpleSynchronizedLazyInitialized;
    }

    @LazilyInitializedField("The array is synchronized lazily initialized.")
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
}

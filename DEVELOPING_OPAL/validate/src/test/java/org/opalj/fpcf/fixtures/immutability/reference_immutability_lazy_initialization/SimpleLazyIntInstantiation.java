package org.opalj.fpcf.fixtures.immutability.reference_immutability_lazy_initialization;

import org.opalj.fpcf.properties.reference_immutability_lazy_initialization.LazyInitializationAnnotation;

public class SimpleLazyIntInstantiation{
    @LazyInitializationAnnotation("")
    private int i = 0;
    public int hashcode() {
        if(i==0)
            i = 5;
        return i;
    }
}

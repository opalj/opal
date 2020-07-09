package org.opalj.fpcf.fixtures.immutability.reference_immutability_lazy_initialization;

import org.opalj.fpcf.properties.reference_immutability.LazyInitializedNotThreadSafeOrNotDeterministicReferenceAnnotation;

public class SimpleLazyObjectsInstantiation{
    @LazyInitializedNotThreadSafeOrNotDeterministicReferenceAnnotation("")
    private static SimpleLazyObjectsInstantiation instance;
    public static SimpleLazyObjectsInstantiation getInstance() {
        if(instance==null)
            instance = new SimpleLazyObjectsInstantiation();
        return instance;
    }
}


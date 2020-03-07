package org.opalj.fpcf.fixtures.immutability.reference_immutability_lazy_initialization;

import org.opalj.fpcf.properties.reference_immutability_lazy_initialization.NotThreadSafeLazyInitializationAnnotation;

public class SimpleLazyObjectsInstantiation{
    @NotThreadSafeLazyInitializationAnnotation("")
    private static SimpleLazyObjectsInstantiation instance;
    public static SimpleLazyObjectsInstantiation getInstance() {
        if(instance==null)
            instance = new SimpleLazyObjectsInstantiation();
        return instance;
    }
}


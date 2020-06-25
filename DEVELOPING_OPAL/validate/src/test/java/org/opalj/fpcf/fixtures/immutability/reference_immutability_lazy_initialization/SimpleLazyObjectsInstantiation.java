package org.opalj.fpcf.fixtures.immutability.reference_immutability_lazy_initialization;

import org.opalj.fpcf.properties.reference_immutability.MutableReferenceAnnotation;

public class SimpleLazyObjectsInstantiation{
    @MutableReferenceAnnotation("")
    private static SimpleLazyObjectsInstantiation instance;
    public static SimpleLazyObjectsInstantiation getInstance() {
        if(instance==null)
            instance = new SimpleLazyObjectsInstantiation();
        return instance;
    }
}


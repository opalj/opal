package org.opalj.fpcf.fixtures.reference_immutability_lazy_initialization;

import org.opalj.fpcf.properties.reference_immutability_lazy_initialization.NotThreadSafeLazyInitializationAnnotation;

public class SimpleLazyObjectsInstantiation{
    @NotThreadSafeLazyInitializationAnnotation("")
    private SimpleLazyObjectsInstantiation instance;
    public SimpleLazyObjectsInstantiation getInstance() {
        if(instance==null)
            instance = new SimpleLazyObjectsInstantiation();
        return instance;
    }
}


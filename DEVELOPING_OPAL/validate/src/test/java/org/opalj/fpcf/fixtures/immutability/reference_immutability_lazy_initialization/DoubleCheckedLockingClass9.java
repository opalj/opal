package org.opalj.fpcf.fixtures.immutability.reference_immutability_lazy_initialization;

import org.opalj.fpcf.properties.reference_immutability.MutableReferenceAnnotation;

public class DoubleCheckedLockingClass9 {

    @MutableReferenceAnnotation("")
    private DoubleCheckedLockingClass9 instance;

    public DoubleCheckedLockingClass9 getInstance() {
        if (instance != null) {
            synchronized (this) {
                if (instance != null) {
                    instance = new DoubleCheckedLockingClass9();
                }
            }
        }
        return instance;
    }
}

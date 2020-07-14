package org.opalj.fpcf.fixtures.immutability.reference_immutability_lazy_initialization;

import org.opalj.fpcf.properties.reference_immutability.LazyInitializedNotThreadSafeOrNotDeterministicReferenceAnnotation;

public class DoubleCheckedLockingClass19 {

    @LazyInitializedNotThreadSafeOrNotDeterministicReferenceAnnotation("")
    private DoubleCheckedLockingClass19 instance;

    public DoubleCheckedLockingClass19 getInstance() {
        if (instance == null) {
            synchronized (this) {
                if (instance == null) {
                }
            }
            instance = new DoubleCheckedLockingClass19();
        }
        return instance;
    }
}

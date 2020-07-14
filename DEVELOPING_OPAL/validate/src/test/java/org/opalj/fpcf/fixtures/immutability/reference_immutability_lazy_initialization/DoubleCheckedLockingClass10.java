package org.opalj.fpcf.fixtures.immutability.reference_immutability_lazy_initialization;

import org.opalj.fpcf.properties.reference_immutability.MutableReferenceAnnotation;

public class DoubleCheckedLockingClass10 {

    @MutableReferenceAnnotation("")
    private DoubleCheckedLockingClass10 instance;

    public DoubleCheckedLockingClass10 getInstance() {
        if (instance == null) {
            synchronized (this) {
                if (instance == null) {
                }
            }
        }
        instance = new DoubleCheckedLockingClass10();
        return instance;
    }
}

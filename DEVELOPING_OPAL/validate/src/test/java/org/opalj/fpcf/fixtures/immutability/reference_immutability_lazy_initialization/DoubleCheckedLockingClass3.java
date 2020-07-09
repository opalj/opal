package org.opalj.fpcf.fixtures.immutability.reference_immutability_lazy_initialization;//source: https://javarevisited.blogspot.com/2014/05/double-checked-locking-on-singleton-in-java.html


import org.opalj.fpcf.properties.reference_immutability.LazyInitializedNotThreadSafeOrNotDeterministicReferenceAnnotation;

public class DoubleCheckedLockingClass3 {

    @LazyInitializedNotThreadSafeOrNotDeterministicReferenceAnnotation("")
    private DoubleCheckedLockingClass3 instance;
    public DoubleCheckedLockingClass3 getInstance() {
        if(instance==null){
            synchronized(DoubleCheckedLockingClass3.class) {
                    instance = new DoubleCheckedLockingClass3();
            }
        }
        return instance;
    }
}



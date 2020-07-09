package org.opalj.fpcf.fixtures.immutability.reference_immutability_lazy_initialization;//source: https://javarevisited.blogspot.com/2014/05/double-checked-locking-on-singleton-in-java.html


import org.opalj.fpcf.properties.reference_immutability.LazyInitializedNotThreadSafeOrNotDeterministicReferenceAnnotation;

public class DoubleCheckedLockingClass5 {

    @LazyInitializedNotThreadSafeOrNotDeterministicReferenceAnnotation("")
    private DoubleCheckedLockingClass5 instance;
    public DoubleCheckedLockingClass5 getInstance() {
        if(instance==null){
                if(instance==null){
                    instance = new DoubleCheckedLockingClass5();
                }
            }
        return instance;
    }
}



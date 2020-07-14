package org.opalj.fpcf.fixtures.immutability.reference_immutability_lazy_initialization;//source: https://javarevisited.blogspot.com/2014/05/double-checked-locking-on-singleton-in-java.html


import org.opalj.fpcf.properties.reference_immutability.LazyInitializedThreadSafeReferenceAnnotation;

public class DoubleCheckedLockingClass16 {

    @LazyInitializedThreadSafeReferenceAnnotation("")
    private DoubleCheckedLockingClass16 instance;
    public DoubleCheckedLockingClass16 getInstance() {
        if(instance==null){
            if(instance==null){
            synchronized(this) {
                    if(instance==null) {
                        instance = new DoubleCheckedLockingClass16();
                    }
                }
            }
        }
        return instance;
    }
}



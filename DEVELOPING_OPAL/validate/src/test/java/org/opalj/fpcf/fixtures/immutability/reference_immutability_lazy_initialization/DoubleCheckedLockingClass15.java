package org.opalj.fpcf.fixtures.immutability.reference_immutability_lazy_initialization;//source: https://javarevisited.blogspot.com/2014/05/double-checked-locking-on-singleton-in-java.html


import org.opalj.fpcf.properties.reference_immutability.LazyInitializedThreadSafeReferenceAnnotation;

public class DoubleCheckedLockingClass15 {

    @LazyInitializedThreadSafeReferenceAnnotation("")
    private DoubleCheckedLockingClass15 instance;
    public DoubleCheckedLockingClass15 getInstance() {
        if(instance==null){
            synchronized(this) {
                if(instance==null){
                    if(instance==null) {
                        instance = new DoubleCheckedLockingClass15();
                    }
                }
            }
        }
        return instance;
    }
}



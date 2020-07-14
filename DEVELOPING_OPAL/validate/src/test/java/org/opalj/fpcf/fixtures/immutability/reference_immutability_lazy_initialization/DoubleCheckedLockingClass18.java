package org.opalj.fpcf.fixtures.immutability.reference_immutability_lazy_initialization;//source: https://javarevisited.blogspot.com/2014/05/double-checked-locking-on-singleton-in-java.html


import org.opalj.fpcf.properties.reference_immutability.LazyInitializedThreadSafeReferenceAnnotation;

public class DoubleCheckedLockingClass18 {

    @LazyInitializedThreadSafeReferenceAnnotation("")
    private DoubleCheckedLockingClass18 instance;
    private boolean lock = true;
    public DoubleCheckedLockingClass18 getInstance() {
        if(instance==null && lock){
            synchronized(this) {
                if(instance==null && lock){
                    instance = new DoubleCheckedLockingClass18();
                }
            }
        }
        return instance;
    }
}



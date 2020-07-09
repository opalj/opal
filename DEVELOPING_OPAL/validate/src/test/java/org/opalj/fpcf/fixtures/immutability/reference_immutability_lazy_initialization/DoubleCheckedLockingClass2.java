package org.opalj.fpcf.fixtures.immutability.reference_immutability_lazy_initialization;//source: https://javarevisited.blogspot.com/2014/05/double-checked-locking-on-singleton-in-java.html


import org.opalj.fpcf.properties.reference_immutability.LazyInitializedThreadSafeReferenceAnnotation;

public class DoubleCheckedLockingClass2 {

    @LazyInitializedThreadSafeReferenceAnnotation("")
    private DoubleCheckedLockingClass2 instance;
    private DoubleCheckedLockingClass2 instance2 = new DoubleCheckedLockingClass2();
    public DoubleCheckedLockingClass2 getInstance() {
        if(instance==null){
            synchronized(this) {
                if(instance==null){
                    instance = new DoubleCheckedLockingClass2();
                }
            }
        }
        return instance;
    }
}



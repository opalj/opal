package org.opalj.fpcf.fixtures.immutability.reference_immutability_lazy_initialization;//source: https://javarevisited.blogspot.com/2014/05/double-checked-locking-on-singleton-in-java.html


import org.opalj.fpcf.properties.reference_immutability.LazyInitializedThreadSafeReferenceAnnotation;

public class DoubleCheckedLockingClass17 {

    @LazyInitializedThreadSafeReferenceAnnotation("")
    private DoubleCheckedLockingClass17 instance;
    public DoubleCheckedLockingClass17 getInstance() {
        if(instance==null){
            if(instance==null) {
                if (instance == null) {
                    synchronized (this) {
                        if (instance == null) {
                            if (instance == null) {
                                instance = new DoubleCheckedLockingClass17();
                            }
                        }
                    }
                }
            }
        }
        return instance;
    }
}



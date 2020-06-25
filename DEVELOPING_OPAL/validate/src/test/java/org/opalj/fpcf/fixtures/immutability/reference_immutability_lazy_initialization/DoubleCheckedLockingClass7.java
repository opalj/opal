package org.opalj.fpcf.fixtures.immutability.reference_immutability_lazy_initialization;//source: https://javarevisited.blogspot.com/2014/05/double-checked-locking-on-singleton-in-java.html


import org.opalj.fpcf.properties.reference_immutability.MutableReferenceAnnotation;

public class DoubleCheckedLockingClass7 {

    @MutableReferenceAnnotation("")
    private DoubleCheckedLockingClass7 instance;
    public DoubleCheckedLockingClass7 getInstance() {
        if(instance==null){
            synchronized(instance) {
                if(instance==null){
                    instance = new DoubleCheckedLockingClass7();
                }

            }
            instance = new DoubleCheckedLockingClass7();
        }
        return instance;
    }
}



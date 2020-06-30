package org.opalj.fpcf.fixtures.immutability.reference_immutability_lazy_initialization;//source: https://javarevisited.blogspot.com/2014/05/double-checked-locking-on-singleton-in-java.html


import org.opalj.fpcf.properties.reference_immutability.MutableReferenceAnnotation;

public class DoubleCheckedLockingClass8 {

    @MutableReferenceAnnotation("")
    private DoubleCheckedLockingClass8 instance;
    public DoubleCheckedLockingClass8 getInstance() {
        if(instance==null){
            synchronized(this) {
                if(instance==null){
                    instance = new DoubleCheckedLockingClass8();
                }
            }
        }
        instance = new DoubleCheckedLockingClass8();
        return instance;
    }
}



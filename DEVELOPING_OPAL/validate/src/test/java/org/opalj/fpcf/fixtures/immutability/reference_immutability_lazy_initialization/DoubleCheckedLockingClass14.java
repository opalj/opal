package org.opalj.fpcf.fixtures.immutability.reference_immutability_lazy_initialization;//source: https://javarevisited.blogspot.com/2014/05/double-checked-locking-on-singleton-in-java.html


import org.opalj.fpcf.properties.reference_immutability.MutableReferenceAnnotation;

public class DoubleCheckedLockingClass14 {

    @MutableReferenceAnnotation("")
    private DoubleCheckedLockingClass14 instance;
    public DoubleCheckedLockingClass14 getInstance() {
        if(instance==null){
        }
        synchronized(this) {
        }
        if(instance==null){

        }
        instance = new DoubleCheckedLockingClass14();
        return instance;
    }
}



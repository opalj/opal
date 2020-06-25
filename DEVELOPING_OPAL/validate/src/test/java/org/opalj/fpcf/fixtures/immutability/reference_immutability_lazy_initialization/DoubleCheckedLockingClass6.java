package org.opalj.fpcf.fixtures.immutability.reference_immutability_lazy_initialization;//source: https://javarevisited.blogspot.com/2014/05/double-checked-locking-on-singleton-in-java.html


import org.opalj.fpcf.properties.reference_immutability.MutableReferenceAnnotation;

public class DoubleCheckedLockingClass6 {

    @MutableReferenceAnnotation("")
    private DoubleCheckedLockingClass6 instance;
    public DoubleCheckedLockingClass6 getInstance() {
        if(instance==null){
            synchronized(instance) {
                if(instance==null){
                    instance = new DoubleCheckedLockingClass6();
                }
                instance = new DoubleCheckedLockingClass6();
            }

        }
        return instance;
    }
}



package org.opalj.fpcf.fixtures.immutability.reference_immutability_lazy_initialization;//source: https://javarevisited.blogspot.com/2014/05/double-checked-locking-on-singleton-in-java.html

import org.opalj.fpcf.properties.reference_immutability.MutableReferenceAnnotation;

public class DoubleCheckedLockingClass4 {

    @MutableReferenceAnnotation("")
    private DoubleCheckedLockingClass4 instance;
    public DoubleCheckedLockingClass4 getInstance() {
            synchronized(DoubleCheckedLockingClass4.class) {
                if(instance==null){
                    instance = new DoubleCheckedLockingClass4();
                }
            }
        return instance;
    }
}



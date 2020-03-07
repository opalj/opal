package org.opalj.fpcf.fixtures.immutability.reference_immutability_lazy_initialization;//source: https://javarevisited.blogspot.com/2014/05/double-checked-locking-on-singleton-in-java.html


import org.opalj.fpcf.properties.reference_immutability_lazy_initialization.LazyInitializationAnnotation;

public class DoubleCheckedLockingClass1{

    @LazyInitializationAnnotation("")
    private static DoubleCheckedLockingClass1 instance;
    public static DoubleCheckedLockingClass1 getInstance() {
        if(instance==null){
            synchronized(DoubleCheckedLockingClass1.class) {
                if(instance==null){
                    instance = new DoubleCheckedLockingClass1();
                }
            }
        }
        return instance;
    }
}



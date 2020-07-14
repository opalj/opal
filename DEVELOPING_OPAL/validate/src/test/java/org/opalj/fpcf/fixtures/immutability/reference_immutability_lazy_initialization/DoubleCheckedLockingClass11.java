package org.opalj.fpcf.fixtures.immutability.reference_immutability_lazy_initialization;//source: https://javarevisited.blogspot.com/2014/05/double-checked-locking-on-singleton-in-java.html


import org.opalj.fpcf.properties.reference_immutability.LazyInitializedThreadSafeReferenceAnnotation;

public class DoubleCheckedLockingClass11 {

    @LazyInitializedThreadSafeReferenceAnnotation("")
    private DoubleCheckedLockingClass11 instance;
    public DoubleCheckedLockingClass11 getInstance() {
        for(int i=0; i<1000; i++){}
        if(instance==null){
            for(int i=0; i<1000; i++){}
            synchronized(this) {
                for(int i=0; i<1000; i++){}
                if(instance==null){
                    for(int i=0; i<1000; i++){}
                    instance = new DoubleCheckedLockingClass11();
                }
            }
        }
        return instance;
    }
}



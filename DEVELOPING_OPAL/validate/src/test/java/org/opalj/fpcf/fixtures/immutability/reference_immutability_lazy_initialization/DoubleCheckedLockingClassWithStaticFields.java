package org.opalj.fpcf.fixtures.immutability.reference_immutability_lazy_initialization;//source: https://javarevisited.blogspot.com/2014/05/double-checked-locking-on-singleton-in-java.html


import org.opalj.fpcf.properties.reference_immutability.LazyInitializedThreadSafeReferenceAnnotation;

public class DoubleCheckedLockingClassWithStaticFields {

    @LazyInitializedThreadSafeReferenceAnnotation("")
    private static DoubleCheckedLockingClassWithStaticFields instance;
    public static DoubleCheckedLockingClassWithStaticFields getInstance() {
        if(instance==null){
            synchronized(DoubleCheckedLockingClassWithStaticFields.class) {
                if(instance==null){
                    instance = new DoubleCheckedLockingClassWithStaticFields();
                }
            }
        }
        return instance;
    }
}



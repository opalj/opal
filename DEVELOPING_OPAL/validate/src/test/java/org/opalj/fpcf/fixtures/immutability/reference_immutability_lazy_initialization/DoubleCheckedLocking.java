package org.opalj.fpcf.fixtures.immutability.reference_immutability_lazy_initialization;

import org.opalj.fpcf.properties.reference_immutability.LazyInitializedNotThreadSafeReferenceAnnotation;
import org.opalj.fpcf.properties.reference_immutability.LazyInitializedThreadSafeReferenceAnnotation;
import org.opalj.fpcf.properties.reference_immutability.MutableReferenceAnnotation;

public class DoubleCheckedLocking {
}

class DoubleCheckedLockingClass1{
    @LazyInitializedThreadSafeReferenceAnnotation("")
    private DoubleCheckedLockingClass1 instance;
    public DoubleCheckedLockingClass1 getInstance() {
        if(instance==null){
            synchronized(this) {
                if(instance==null){
                    instance = new DoubleCheckedLockingClass1();
                }
            }
        }
        return instance;
    }
}

class DoubleCheckedLockingClass2 {

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

class DoubleCheckedLockingClass3 {

    @LazyInitializedNotThreadSafeReferenceAnnotation("")
    private DoubleCheckedLockingClass3 instance;
    public DoubleCheckedLockingClass3 getInstance() {
        if(instance==null){
            synchronized(DoubleCheckedLockingClass3.class) {
                instance = new DoubleCheckedLockingClass3();
            }
        }
        return instance;
    }
}

class DoubleCheckedLockingClass4 {

    @LazyInitializedThreadSafeReferenceAnnotation("")
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

class DoubleCheckedLockingClass5 {

    @LazyInitializedNotThreadSafeReferenceAnnotation("")
    private DoubleCheckedLockingClass5 instance;
    public DoubleCheckedLockingClass5 getInstance() {
        if(instance==null){
            if(instance==null){
                instance = new DoubleCheckedLockingClass5();
            }
        }
        return instance;
    }
}

class DoubleCheckedLockingClass6 {

    @LazyInitializedNotThreadSafeReferenceAnnotation("")
    private DoubleCheckedLockingClass6 instance;
    public DoubleCheckedLockingClass6 getInstance() {
        if(instance==null){
            synchronized(this) {
                if(instance==null){
                    //instance = new DoubleCheckedLockingClass6();
                }
                instance = new DoubleCheckedLockingClass6();
            }

        }
        return instance;
    }
}

class DoubleCheckedLockingClass7 {

    @MutableReferenceAnnotation("")
    private DoubleCheckedLockingClass7 instance;
    public DoubleCheckedLockingClass7 getInstance() {
        if(instance==null){
            synchronized(this) {
                if(instance==null){
                    instance = new DoubleCheckedLockingClass7();
                }

            }
            instance = new DoubleCheckedLockingClass7();
        }
        return instance;
    }
}

class DoubleCheckedLockingClass8 {

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

class DoubleCheckedLockingClass9 {

    @MutableReferenceAnnotation("")
    private DoubleCheckedLockingClass9 instance;

    public DoubleCheckedLockingClass9 getInstance() {
        if (instance != null) {
            synchronized (this) {
                if (instance != null) {
                    instance = new DoubleCheckedLockingClass9();
                }
            }
        }
        return instance;
    }
}

class DoubleCheckedLockingClass10 {

    @MutableReferenceAnnotation("")
    private DoubleCheckedLockingClass10 instance;

    public DoubleCheckedLockingClass10 getInstance() {
        if (instance == null) {
            synchronized (this) {
                if (instance == null) {
                }
            }
        }
        instance = new DoubleCheckedLockingClass10();
        return instance;
    }
}

class DoubleCheckedLockingClass11 {

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

class DoubleCheckedLockingClass12 {

    @LazyInitializedThreadSafeReferenceAnnotation("")
    private DoubleCheckedLockingClass12 instance;
    public DoubleCheckedLockingClass12 getInstance() {
        if(instance==null){
        }
        synchronized(this) {
            if(instance==null){
                instance = new DoubleCheckedLockingClass12();
            }
        }
        return instance;
    }
}

class DoubleCheckedLockingClass13 {

    @LazyInitializedNotThreadSafeReferenceAnnotation("")
    private DoubleCheckedLockingClass13 instance;
    public DoubleCheckedLockingClass13 getInstance() {
        if(instance==null){
        }
        synchronized(this) {
        }
        if(instance==null){
            instance = new DoubleCheckedLockingClass13();
        }
        return instance;
    }
}

class DoubleCheckedLockingClass14 {

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

class DoubleCheckedLockingClass15 {

    @LazyInitializedThreadSafeReferenceAnnotation("")
    private DoubleCheckedLockingClass15 instance;
    public DoubleCheckedLockingClass15 getInstance() {
        if(instance==null){
            synchronized(this) {
                if(instance==null){
                    if(instance==null) {
                        instance = new DoubleCheckedLockingClass15();
                    }
                }
            }
        }
        return instance;
    }
}

class DoubleCheckedLockingClass16 {

    @LazyInitializedThreadSafeReferenceAnnotation("")
    private DoubleCheckedLockingClass16 instance;
    public DoubleCheckedLockingClass16 getInstance() {
        if(instance==null){
            if(instance==null){
                synchronized(this) {
                    if(instance==null) {
                        instance = new DoubleCheckedLockingClass16();
                    }
                }
            }
        }
        return instance;
    }
}

class DoubleCheckedLockingClass17 {

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

class DoubleCheckedLockingClass18 {

    @LazyInitializedThreadSafeReferenceAnnotation("")
    private DoubleCheckedLockingClass18 instance;
    private boolean lock = true;
    public DoubleCheckedLockingClass18 getInstance() {
        if(instance==null && lock){
            synchronized(this) {
                if(instance==null && lock){
                    instance = new DoubleCheckedLockingClass18();
                }
            }
        }
        return instance;
    }
}










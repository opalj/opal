package org.opalj.fpcf.fixtures.benchmark.lazy_initialization.objects;

import org.opalj.fpcf.fixtures.benchmark.generals.ClassWithMutableFields;
import org.opalj.fpcf.properties.immutability.field_assignability.LazilyInitializedField;

/**
 * This class encompasses different forms of lazy initialized fields with object type.
 */
public class LazyInitialization {

    //@Immutable
    @LazilyInitializedField("field is correctly lazily initialized guarded within a synchronized method")
    private Integer lazyInitializedIntegerWithinASynchronizedMethod;

    public synchronized Integer getLazyInitializedIntegerWithinASynchronizedMethod(){
        if(this.lazyInitializedIntegerWithinASynchronizedMethod ==0)
            this.lazyInitializedIntegerWithinASynchronizedMethod = new Integer(5);
        return this.lazyInitializedIntegerWithinASynchronizedMethod;
    }

    @LazilyInitializedField("standard double checked locked initialized integer field")
    private Integer doubleCheckedLockedIntegerField;

    public int getDoubleCheckedLockedIntegerField() {
        if(doubleCheckedLockedIntegerField==0){
            synchronized(this) {
                if(doubleCheckedLockedIntegerField==0){
                    doubleCheckedLockedIntegerField = 42;
                }
            }
        }
        return doubleCheckedLockedIntegerField;
    }

    @LazilyInitializedField("a simple variation of double checked locking without the outer guard")
    private Object onlyOneGuardWithinTheSynchronization;

    public Object getOnlyOneGuardWithinTheSynchronization() {
        synchronized(Object.class) {
            if(onlyOneGuardWithinTheSynchronization ==null){
                onlyOneGuardWithinTheSynchronization = new Object();
            }
        }
        return onlyOneGuardWithinTheSynchronization;
    }

    @LazilyInitializedField("")
    private ClassWithMutableFields functionCallAfterAssignment;

    public synchronized void getTM1(){
        if(functionCallAfterAssignment ==null){
            functionCallAfterAssignment = new ClassWithMutableFields();
        }
        functionCallAfterAssignment.nop();
    }


    @LazilyInitializedField("The field is lazily initialized within dcl pattern that is implemented with early return")
    private Object dclWithEarlyReturn;
    public Object getDclWithEarlyReturn(){
        if(dclWithEarlyReturn!=null)
            return dclWithEarlyReturn;
        synchronized(this) {
            if(dclWithEarlyReturn==null)
                dclWithEarlyReturn = new Object();
        }
        return dclWithEarlyReturn;
    }

    @LazilyInitializedField("The field is synchronized guarded lazily initialized.")
    private Object loopsInDCLPattern;
    public Object getLoopsInDCLPattern() {
        for(int i=0; i<1000; i++){}
        if(loopsInDCLPattern ==null){
            for(int i=0; i<1000; i++){}
            synchronized(this) {
                for(int i=0; i<1000; i++){}
                if(loopsInDCLPattern ==null){
                    for(int i=0; i<1000; i++){}
                    loopsInDCLPattern = new Object();
                }
            }
        }
        return loopsInDCLPattern;
    }

    @LazilyInitializedField("The field is lazy initialized within a dcl pattern with multiple guards.")
    private Object multipleGuardsInDCLPattern;
    public Object getMultipleGuardsInDCLPattern() {
        if(multipleGuardsInDCLPattern ==null){}
        if(multipleGuardsInDCLPattern ==null){
            if(multipleGuardsInDCLPattern ==null) {
                if (multipleGuardsInDCLPattern == null) {
                    synchronized (this) {
                        if (multipleGuardsInDCLPattern == null) {
                            if (multipleGuardsInDCLPattern == null) {
                                multipleGuardsInDCLPattern = new Object();
                            }
                        }
                    }
                }
            }
        }
        return multipleGuardsInDCLPattern;
    }
}

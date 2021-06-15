package org.opalj.fpcf.fixtures.benchmark.lazy_initialization.objects;

import org.opalj.fpcf.properties.immutability.field_assignability.AssignableField;
import org.opalj.fpcf.properties.immutability.field_assignability.UnsafelyLazilyInitializedField;

import java.util.Random;

/**
 * This class encompasses different forms of unsafely lazily initialization
 */
public class UnsafelyLazilyInitialization {

    @UnsafelyLazilyInitializedField("the write to the object reference simpleLazyInitialization is not atomic")
    private static Object simpleLazyInitialization;

    public static Object init() {
        if (simpleLazyInitialization == null)
            simpleLazyInitialization = new Object();
        return simpleLazyInitialization;
    }
    @UnsafelyLazilyInitializedField("The field is not thread safe and not deterministic written")
    private int notThreadSafeRandomWrite;

    public int getNotThreadSafeRandomWrite(){
        if(notThreadSafeRandomWrite==0){
            notThreadSafeRandomWrite = new Random().nextInt();
        }
        return notThreadSafeRandomWrite;
    }

    @UnsafelyLazilyInitializedField("the field nonnull check is not synchronized guarded")
    private Object noSynchronizedGuard;
    public Object getNoSynchronizedGuard() {
        if(noSynchronizedGuard ==null){
            synchronized(Object.class) {
                noSynchronizedGuard = new Object();
            }
        }
        return noSynchronizedGuard;
    }
    @UnsafelyLazilyInitializedField(value = "the field write is not synchronized guarded")
    private Object twoNotSynchronizedGuards;
    public Object getTwoNotSynchronizedGuards() {
        if(twoNotSynchronizedGuards ==null){
            if(twoNotSynchronizedGuards ==null){
                twoNotSynchronizedGuards = new Object();
            }
        }
        return twoNotSynchronizedGuards;
    }

    @UnsafelyLazilyInitializedField("the field is written outside the synchronized block")
    private Object writeOutsideTheSynchronizedGuard;
    public Object getWriteOutsideTheSynchronizedGuard() {
        if(writeOutsideTheSynchronizedGuard ==null){
            synchronized(this) {
                if(writeOutsideTheSynchronizedGuard ==null){

                }
                writeOutsideTheSynchronizedGuard = new Object();
            }

        }
        return writeOutsideTheSynchronizedGuard;
    }

    @UnsafelyLazilyInitializedField(value = "only a guard around the field write")
    private Object notNestedDCLWriteOnlyInGuard;
    public Object getNotNestedDCLWriteOnlyInGuard() {
        if(notNestedDCLWriteOnlyInGuard ==null){
        }
        synchronized(this) {
        }
        if(notNestedDCLWriteOnlyInGuard ==null){
            notNestedDCLWriteOnlyInGuard = new Object();
        }
        return notNestedDCLWriteOnlyInGuard;
    }

    @UnsafelyLazilyInitializedField("the field read for the guard is outside the synchronized block")
    private Object fieldReadOutsideSynchronizedBlock;

    public Object getFieldReadOutsideSynchronizedBlock(){
        Object tmpInteger = fieldReadOutsideSynchronizedBlock;
        synchronized (this){
            if(tmpInteger==null)
                fieldReadOutsideSynchronizedBlock = new Object();
        }
        return fieldReadOutsideSynchronizedBlock;
    }

    @UnsafelyLazilyInitializedField(value = "the field read for the guard is outside the synchronized block")
    private Object fieldReadOutsideSynchronizedBlockEarlyReturn;

    public Object getFieldReadOutsideSynchronizedBlockEarlyReturn(){
        Object tmpInteger = fieldReadOutsideSynchronizedBlockEarlyReturn;
        if(tmpInteger!=null)
            return fieldReadOutsideSynchronizedBlockEarlyReturn;
        synchronized (this){
            if(tmpInteger==null)
                fieldReadOutsideSynchronizedBlockEarlyReturn = new Object();
        }
        return fieldReadOutsideSynchronizedBlockEarlyReturn;
    }
}

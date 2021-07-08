package org.opalj.fpcf.fixtures.benchmark.lazy_initialization.objects;

import org.opalj.fpcf.fixtures.benchmark.commons.CustomObject;
import org.opalj.fpcf.properties.immutability.field_assignability.AssignableField;
import org.opalj.fpcf.properties.immutability.field_assignability.UnsafelyLazilyInitializedField;
//import edu.cmu.cs.glacier.qual.Immutable;

import java.util.Random;

/**
 * This class encompasses different forms of unsafely lazily initialization
 */
public class UnsafelyLazilyInitialization {

    //@Immutable
    @UnsafelyLazilyInitializedField("the write to the object reference simpleLazyInitialization is not atomic")
    private static CustomObject simpleLazyInitialization;

    public static CustomObject init() {
        if (simpleLazyInitialization == null)
            simpleLazyInitialization = new CustomObject();
        return simpleLazyInitialization;
    }

    //@Immutable
    @UnsafelyLazilyInitializedField("The field is not thread safe and not deterministic written")
    private int notThreadSafeRandomWrite;

    public int getNotThreadSafeRandomWrite(){
        if(notThreadSafeRandomWrite==0){
            notThreadSafeRandomWrite = new Random().nextInt();
        }
        return notThreadSafeRandomWrite;
    }

    //@Immutable
    @UnsafelyLazilyInitializedField("the field nonnull check is not synchronized guarded")
    private CustomObject noSynchronizedGuard;
    public CustomObject getNoSynchronizedGuard() {
        if(noSynchronizedGuard ==null){
            synchronized(CustomObject.class) {
                noSynchronizedGuard = new CustomObject();
            }
        }
        return noSynchronizedGuard;
    }

    //@Immutable
    @UnsafelyLazilyInitializedField(value = "the field write is not synchronized guarded")
    private CustomObject twoNotSynchronizedGuards;
    public CustomObject getTwoNotSynchronizedGuards() {
        if(twoNotSynchronizedGuards ==null){
            if(twoNotSynchronizedGuards ==null){
                twoNotSynchronizedGuards = new CustomObject();
            }
        }
        return twoNotSynchronizedGuards;
    }

    //@Immutable
    @UnsafelyLazilyInitializedField("the field is written outside the synchronized block")
    private CustomObject writeOutsideTheSynchronizedGuard;
    public CustomObject getWriteOutsideTheSynchronizedGuard() {
        if(writeOutsideTheSynchronizedGuard ==null){
            synchronized(this) {
                if(writeOutsideTheSynchronizedGuard ==null){

                }
                writeOutsideTheSynchronizedGuard = new CustomObject();
            }

        }
        return writeOutsideTheSynchronizedGuard;
    }

    //@Immutable
    @UnsafelyLazilyInitializedField(value = "only a guard around the field write")
    private CustomObject notNestedDCLWriteOnlyInGuard;
    public CustomObject getNotNestedDCLWriteOnlyInGuard() {
        if(notNestedDCLWriteOnlyInGuard ==null){
        }
        synchronized(this) {
        }
        if(notNestedDCLWriteOnlyInGuard ==null){
            notNestedDCLWriteOnlyInGuard = new CustomObject();
        }
        return notNestedDCLWriteOnlyInGuard;
    }

    //@Immutable
    @UnsafelyLazilyInitializedField("the field read for the guard is outside the synchronized block")
    private CustomObject fieldReadOutsideSynchronizedBlock;

    public CustomObject getFieldReadOutsideSynchronizedBlock(){
        CustomObject tmpCustomObject = fieldReadOutsideSynchronizedBlock;
        synchronized (this){
            if(tmpCustomObject==null)
                fieldReadOutsideSynchronizedBlock = new CustomObject();
        }
        return fieldReadOutsideSynchronizedBlock;
    }

    //@Immutable
    @UnsafelyLazilyInitializedField("the field read for the guard is outside the synchronized block")
    private CustomObject fieldReadOutsideSynchronizedBlockEarlyReturn;

    public CustomObject getFieldReadOutsideSynchronizedBlockEarlyReturn(){
        CustomObject tmpCustomObject = fieldReadOutsideSynchronizedBlockEarlyReturn;
        if(tmpCustomObject!=null)
            return fieldReadOutsideSynchronizedBlockEarlyReturn;
        synchronized (this){
            if(tmpCustomObject==null)
                fieldReadOutsideSynchronizedBlockEarlyReturn = new CustomObject();
        }
        return fieldReadOutsideSynchronizedBlockEarlyReturn;
    }
}

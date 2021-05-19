/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.benchmark.lazy_initialization.objects;

//import edu.cmu.cs.glacier.qual.Immutable;
import org.opalj.fpcf.properties.immutability.fields.TransitivelyImmutableField;
import org.opalj.fpcf.properties.immutability.field_assignability.LazyInitializedThreadSafeFieldReference;

import java.util.Random;

/**
 * This classes encompasses different variation of double checked locking as a form of lazy initialization.
 *
 * @author Tobias Roth
 *
 */
//@Immutable
public class DoubleCheckedLockingDeep {

    @TransitivelyImmutableField("")
    @LazyInitializedThreadSafeFieldReference("")
    private Object o;

    public synchronized Object getO(){
        if(this.o==null)
            this.o = new Object();
        return this.o;
    }

    @TransitivelyImmutableField("")
    @LazyInitializedThreadSafeFieldReference("standard double checked locked initialized int field")
    private int doubleCheckedLockedIntField;

    public int getDoubleCheckedLockedIntField() {
        if(doubleCheckedLockedIntField==0){
            synchronized(this) {
                if(doubleCheckedLockedIntField==0){
                    doubleCheckedLockedIntField = 42;
                }
            }
        }
        return doubleCheckedLockedIntField;
    }

    @TransitivelyImmutableField("")
    @LazyInitializedThreadSafeFieldReference("field write is not deterministic but only once written due to dcl")
    private int initializedWithDCLRandomWrite;

    public int getInitializedWithDCLRandomWrite(){
        if(initializedWithDCLRandomWrite==0){
            synchronized(this){
                if(initializedWithDCLRandomWrite==0){
                    initializedWithDCLRandomWrite = new Random().nextInt();
                }
            }
        }
        return initializedWithDCLRandomWrite;
    }

    @TransitivelyImmutableField("")
    @LazyInitializedThreadSafeFieldReference("dcl is implemented with early return")
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


    @TransitivelyImmutableField("")
    @LazyInitializedThreadSafeFieldReference("write within a dcl and try catch")
    private Object dclWithTryCatch;

    public Object DCL5(){
        if(dclWithTryCatch==null){
            synchronized(this){
                if(dclWithTryCatch==null){
                    try{
                        dclWithTryCatch = new Object();
                    }
                    catch (Exception e)
                    {
                        throw e;
                    }
                }
            }
        }
        return dclWithTryCatch;
    }


    @TransitivelyImmutableField("")
    @LazyInitializedThreadSafeFieldReference("lazy initialization in a synchronized method")
    private Integer synchronizedSimpleLazyInitializedIntegerField;

    public synchronized void initNO2(){
        if(synchronizedSimpleLazyInitializedIntegerField==0)
            synchronizedSimpleLazyInitializedIntegerField = 5;
    }


    @TransitivelyImmutableField("")
    @LazyInitializedThreadSafeFieldReference(value = "standard double checked locking")
    private Object standardDCL;
    public Object getStandardDCL() {
        if(standardDCL ==null){
            synchronized(this) {
                if(standardDCL ==null){
                    standardDCL = new Object();
                }
            }
        }
        return standardDCL;
    }

    @TransitivelyImmutableField("")
    @LazyInitializedThreadSafeFieldReference("a simple variation of double checked locking without the outer guard")
    private Object onlyOneGuardWithinTheSynchronization;

    public Object getOnlyOneGuardWithinTheSynchronization() {
        synchronized(Object.class) {
            if(onlyOneGuardWithinTheSynchronization ==null){
                onlyOneGuardWithinTheSynchronization = new Object();
            }
        }
        return onlyOneGuardWithinTheSynchronization;
    }

    @TransitivelyImmutableField("")
    @LazyInitializedThreadSafeFieldReference("dcl pattern with loops in it")
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

    @TransitivelyImmutableField("")
    @LazyInitializedThreadSafeFieldReference("no correct complecte dcl pattern but sufficient for thread " +
            "safety due to a correct guard in a synchronized block")
    private Object outerGuardEndsBeforeSynchronization;
    public Object getOuterGuardEndsBeforeSynchronization() {
        if(outerGuardEndsBeforeSynchronization ==null){
        }
        synchronized(this) {
            if(outerGuardEndsBeforeSynchronization ==null){
                outerGuardEndsBeforeSynchronization = new Object();
            }
        }
        return outerGuardEndsBeforeSynchronization;
    }


    @TransitivelyImmutableField("")
    @LazyInitializedThreadSafeFieldReference("")
    private Object multipleGuardsInDCLPattern;
    public Object getMultipleGuardsInDCLPattern() {
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

/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.benchmark.lazy_initialization.objects;

//import edu.cmu.cs.glacier.qual.Immutable;
import org.opalj.fpcf.properties.immutability.fields.DeepImmutableField;
import org.opalj.fpcf.properties.immutability.references.LazyInitializedNotThreadSafeFieldReference;
import org.opalj.fpcf.properties.immutability.references.LazyInitializedThreadSafeFieldReference;
import org.opalj.fpcf.properties.immutability.references.MutableFieldReference;

import java.util.Random;

/**
 * This classes encompasses different variation of double checked locking as a form of lazy initialization.
 *
 * @author Tobias Roth
 *
 */
public class DoubleCheckedLocking {

    //@Immutable
    @DeepImmutableField("")
    @LazyInitializedThreadSafeFieldReference("")
    private Object o;

    public synchronized Object getO(){
        if(this.o==null)
            this.o = new Object();
        return this.o;
    }

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

    @LazyInitializedNotThreadSafeFieldReference("The field is not thread safe and not deterministic written")
    private int notThreadSafeRandomWrite;

    public int getNotThreadSafeRandomWrite(){
        if(notThreadSafeRandomWrite==0){
            notThreadSafeRandomWrite = new Random().nextInt();
        }
        return notThreadSafeRandomWrite;
    }

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

    @MutableFieldReference("no correct lazy initialization because all exceptions are caught in the inner guard")
    Object noDCLAllExceptionsCaughtInsidInnerGuard;

    public Object getNoDCL(){
        if(noDCLAllExceptionsCaughtInsidInnerGuard==null){
            synchronized(this){
                if(noDCLAllExceptionsCaughtInsidInnerGuard==null){
                    try{
                        noDCLAllExceptionsCaughtInsidInnerGuard = new Object();
                    }
                    catch(Exception e){

                    }
                }
            }
        }
        return noDCLAllExceptionsCaughtInsidInnerGuard;
    }

    @MutableFieldReference("No correct lazy initialization because all exceptions are caught in the complete dcl")
    Object noDCLAllExceptionsAreCaughtInTheCompleteDCL;

    public Object getNoDCLAllExceptionsAreCaughtInTheCompleteDCL(){
        try{
            if(noDCLAllExceptionsAreCaughtInTheCompleteDCL ==null){
                synchronized(this){
                    if(noDCLAllExceptionsAreCaughtInTheCompleteDCL ==null){
                        noDCLAllExceptionsAreCaughtInTheCompleteDCL = new Object();
                    }

                }
            }
        }
        catch(Exception e){
        }
        return noDCLAllExceptionsAreCaughtInTheCompleteDCL;
    }

    @MutableFieldReference("no correct dcl pattern because all exceptions are caught in the outer guard")
    Object instance;

    public Object NoDCL1(){

        if(instance==null){
            try{
                synchronized(this){
                    if(instance==null){

                        instance = new Object();
                    }
                }}catch(Exception e){}}
        return instance;
    }


    @MutableFieldReference("no correct dcl, because the two try-catch-blocks")
    Object noDCLTwoTryCatchBlocks;

    public Object getNoDCLTwoTryCatchBlocks(){

        if(noDCLTwoTryCatchBlocks==null){
            try{
                synchronized(this){
                    if(noDCLTwoTryCatchBlocks==null){
                        try{
                            noDCLTwoTryCatchBlocks = new Object();
                        }
                        catch (Exception e)
                        {
                            throw e;
                        }
                    }
                }
            }
            catch(Exception e){

            }
        }
        return noDCLTwoTryCatchBlocks;
    }

    @MutableFieldReference("no correct dcl because wrong exception forwarding")
    Object noDCLWrongExceptionForwarding;

    public Object getNoDCLWrongExceptionForwarding() throws IndexOutOfBoundsException{
        if(noDCLWrongExceptionForwarding==null){
            synchronized(this){
                if(noDCLWrongExceptionForwarding==null){
                    try{
                        noDCLWrongExceptionForwarding = new Object();
                    }
                    catch (Exception e)
                    {
                        throw new IndexOutOfBoundsException();
                    }
                }
            }
        }
        return noDCLWrongExceptionForwarding;
    }


    @LazyInitializedThreadSafeFieldReference("standard dcl initialization of array reference")
    private Object[] lazyInitializedArrayReference;

    public Object[] getLazyInitializedArrayReference() {
        if(lazyInitializedArrayReference==null){
            synchronized(this) {
                if(lazyInitializedArrayReference==null){
                    lazyInitializedArrayReference = new Object[10];
                }
            }
        }
        return lazyInitializedArrayReference;
    }

    @LazyInitializedNotThreadSafeFieldReference("The field is not thread safe lazy initialized but within a guard")
    private int[] notThreadSafeLazyInitializedArray;

    public int[] getValues(){
        if(notThreadSafeLazyInitializedArray==null){
            notThreadSafeLazyInitializedArray = new int[] {1,2,3,4,5,6,7,8,9,10};
        }
        return notThreadSafeLazyInitializedArray;
    }

    @LazyInitializedThreadSafeFieldReference("the field is guarded initialized within a synchronized method")

    private int[] synchronizedArrayInitialization;

    public synchronized int[] getSynchronizedArrayInitialization(){
        if(synchronizedArrayInitialization==null){
            synchronizedArrayInitialization = new int[] {1,2,3,4,5,6,7,8,9,10};
        }
        return synchronizedArrayInitialization;
    }

    @DeepImmutableField("")
    @LazyInitializedThreadSafeFieldReference("lazy initialization in a synchronized method")
    private Integer synchronizedSimpleLazyInitializedIntegerField;

    public synchronized void initNO2(){
        if(synchronizedSimpleLazyInitializedIntegerField==0)
            synchronizedSimpleLazyInitializedIntegerField = 5;
    }

    @LazyInitializedNotThreadSafeFieldReference("not synchronized")
    private Object notSynchronized;

    public Object getNotSynchronized() {
        if(notSynchronized==null)
            notSynchronized = new Object();
        return notSynchronized;
    }


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


    @LazyInitializedNotThreadSafeFieldReference("the field write is not synchronized guarded")
    private Object noSynchronizedGuard;
    public Object getNoSynchronizedGuard() {
        if(noSynchronizedGuard ==null){
            synchronized(Object.class) {
                noSynchronizedGuard = new Object();
            }
        }
        return noSynchronizedGuard;
    }

    @DeepImmutableField("")
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

    @LazyInitializedNotThreadSafeFieldReference(value = "the field write is not synchronized guarded two times")
    private Object twoNotSynchronizedGuards;
    public Object getTwoNotSynchronizedGuards() {
        if(twoNotSynchronizedGuards ==null){
            if(twoNotSynchronizedGuards ==null){
                twoNotSynchronizedGuards = new Object();
            }
        }
        return twoNotSynchronizedGuards;
    }

    @LazyInitializedNotThreadSafeFieldReference("the field is written outside the synchronized block")
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

    @MutableFieldReference("no valid lazy initialization")
    private Object multipleWritesWithinTheDCLPattern;
    public Object getMultipleWritesWithinTheDCLPattern() {
        if(multipleWritesWithinTheDCLPattern ==null){
            synchronized(this) {
                if(multipleWritesWithinTheDCLPattern ==null){
                    multipleWritesWithinTheDCLPattern = new Object();
                }
            }
            multipleWritesWithinTheDCLPattern = new Object();
        }
        return multipleWritesWithinTheDCLPattern;
    }

    @MutableFieldReference("no valid lazy initialization")
    private Object alsoWrittenOutsideTheDCLPattern;
    public Object getAlsoWrittenOutsideTheDCLPattern() {
        if(alsoWrittenOutsideTheDCLPattern ==null){
            synchronized(this) {
                if(alsoWrittenOutsideTheDCLPattern ==null){
                    alsoWrittenOutsideTheDCLPattern = new Object();
                }
            }
        }
        alsoWrittenOutsideTheDCLPattern = new Object();
        return alsoWrittenOutsideTheDCLPattern;
    }

    @MutableFieldReference(value = "no lazy initialization due to wrong guard statements")
    private Object wrongGuardStatement;

    public Object getWrongGuardStatement() {
        if (wrongGuardStatement != null) {
            synchronized (this) {
                if (wrongGuardStatement != null) {
                    wrongGuardStatement = new Object();
                }
            }
        }
        return wrongGuardStatement;
    }

    @MutableFieldReference(value = "no valid lazy initialization")
    private Object writeOutsideDCL;

    public Object getWriteOutsideDCL() {
        if (writeOutsideDCL == null) {
            synchronized (this) {
                if (writeOutsideDCL == null) {
                }
            }
        }
        writeOutsideDCL = new Object();
        return writeOutsideDCL;
    }

    @DeepImmutableField("")
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

    @LazyInitializedNotThreadSafeFieldReference(value = "only a guard around the field write")
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

    @MutableFieldReference("field write outside guards and synchronized blocks")
    private Object notNestedDCLWriteOutside;
    public Object getNotNestedDCLWriteOutside() {
        if(notNestedDCLWriteOutside ==null){
        }
        synchronized(this) {
        }
        if(notNestedDCLWriteOutside ==null){

        }
        notNestedDCLWriteOutside = new Object();
        return notNestedDCLWriteOutside;
    }


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

    @MutableFieldReference("guard not only dependent on field value")
    private Object dclWithLockAnd;

    public Object getDclWithLockAnd(boolean lock) {
        if(dclWithLockAnd ==null && lock){
            synchronized(this) {
                if(dclWithLockAnd ==null && lock){
                    dclWithLockAnd = new Object();
                }
            }
        }
        return dclWithLockAnd;
    }

    @MutableFieldReference("guard not only dependent on field value")
    private Object dclWithLockOr;

    public Object getDclWithLockOr(boolean lock) {
        if(dclWithLockOr ==null && lock){
            synchronized(this) {
                if(dclWithLockOr ==null && lock){
                    dclWithLockOr = new Object();
                }
            }
        }
        return dclWithLockOr;
    }

    @LazyInitializedNotThreadSafeFieldReference(value = "the field read for the guard is outside the synchronized block")
    private Object fieldReadOutsideSynchronizedBlock;

    public Object getFieldReadOutsideSynchronizedBlock(){
        Object tmpo1 = fieldReadOutsideSynchronizedBlock;
        synchronized (this){
            if(tmpo1==null)
                fieldReadOutsideSynchronizedBlock = tmpo1 = new Object();
        }
        return fieldReadOutsideSynchronizedBlock;
    }

    @LazyInitializedNotThreadSafeFieldReference(value = "the field read for the guard is outside the synchronized block")
    private Object fieldReadOutsideSynchronizedBlockEarlyReturn;

    public Object getFieldReadOutsideSynchronizedBlockEarlyReturn(){
        Object tmpo2 = fieldReadOutsideSynchronizedBlockEarlyReturn;
        if(tmpo2!=null)
            return fieldReadOutsideSynchronizedBlockEarlyReturn;
        synchronized (this){
            if(tmpo2==null)
                fieldReadOutsideSynchronizedBlockEarlyReturn = new Object();
        }
        return fieldReadOutsideSynchronizedBlockEarlyReturn;
    }

}

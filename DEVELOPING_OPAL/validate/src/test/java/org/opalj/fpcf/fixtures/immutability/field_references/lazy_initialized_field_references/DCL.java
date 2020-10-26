/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.immutability.field_references.lazy_initialized_field_references;

import org.opalj.fpcf.properties.immutability.references.LazyInitializedNotThreadSafeFieldReference;
import org.opalj.fpcf.properties.immutability.references.LazyInitializedThreadSafeFieldReference;
import org.opalj.fpcf.properties.immutability.references.MutableFieldReference;
import org.opalj.tac.fpcf.analyses.immutability.L3FieldImmutabilityAnalysis;
import org.opalj.tac.fpcf.analyses.immutability.fieldreference.L0FieldReferenceImmutabilityAnalysis;

import java.util.Random;

/**
 * This class encompasses variations of double checked locking pattern and
 * counter examples.
 */
public class DCL {

    @LazyInitializedThreadSafeFieldReference(value = "standard double checked locked initialized int field",
            analyses = L0FieldReferenceImmutabilityAnalysis.class)
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

    @LazyInitializedThreadSafeFieldReference(value = "field write is not deterministic but only once written due to dcl",
            analyses = L0FieldReferenceImmutabilityAnalysis.class)
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

    @MutableFieldReference(value = "The field is not thread safe and not deterministic written",
    analyses = L0FieldReferenceImmutabilityAnalysis.class)
    private int notThreadSafeRandomWrite;

    public int getNotThreadSafeRandomWrite(){
        if(notThreadSafeRandomWrite==0){
            notThreadSafeRandomWrite = new Random().nextInt();
        }
        return notThreadSafeRandomWrite;
    }

    @LazyInitializedThreadSafeFieldReference(value = "dcl is implemented with early return",
            analyses = L0FieldReferenceImmutabilityAnalysis.class)
    private Object dclWithEarlyReturn;
    public Object getInstance(){
        if(dclWithEarlyReturn!=null)
            return dclWithEarlyReturn;
        synchronized(this) {
            if(dclWithEarlyReturn==null)
                dclWithEarlyReturn = new Object();
        }
        return dclWithEarlyReturn;
    }


    @LazyInitializedThreadSafeFieldReference(value = "write within a dcl and try catch",
            analyses = L0FieldReferenceImmutabilityAnalysis.class)
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

    @MutableFieldReference(value = "no correct lazy initialization because " +
            "all exceptions are caught in the inner guard",
            analyses = L0FieldReferenceImmutabilityAnalysis.class)
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

    @MutableFieldReference(value = "No correct lazy initialization because all exceptions are caught in the complete dcl",
    analyses = L0FieldReferenceImmutabilityAnalysis.class)
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

    @MutableFieldReference(value = "no correct dcl pattern because all exceptions are caught in the outer guard",
    analyses = L0FieldReferenceImmutabilityAnalysis.class)
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


    @MutableFieldReference(value = "no correct dcl, because the two try-catch-blocks",
            analyses = L0FieldReferenceImmutabilityAnalysis.class)
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

    @MutableFieldReference(value = "no correct dcl because wrong exception forwarding",
    analyses = L0FieldReferenceImmutabilityAnalysis.class)
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


    @LazyInitializedThreadSafeFieldReference(value = "standard dcl initialization of array reference",
    analyses = L0FieldReferenceImmutabilityAnalysis.class)
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





    @LazyInitializedNotThreadSafeFieldReference(value = "The field is not thread safe lazy initialized but within a guard",
    analyses = L0FieldReferenceImmutabilityAnalysis.class)
    private int[] notThreadSafeLazyInitializedArray;

    public int[] getValues(){
        if(notThreadSafeLazyInitializedArray==null){
            notThreadSafeLazyInitializedArray = new int[] {1,2,3,4,5,6,7,8,9,10};
        }
        return notThreadSafeLazyInitializedArray;
    }


    @LazyInitializedThreadSafeFieldReference(value = "the field is guarded initialized within a synchronized method",
    analyses = L0FieldReferenceImmutabilityAnalysis.class)

    private int[] synchronizedArrayInitialization;

    public synchronized int[] getSynchronizedArrayInitialization(){
        if(synchronizedArrayInitialization==null){
            synchronizedArrayInitialization = new int[] {1,2,3,4,5,6,7,8,9,10};
        }
        return synchronizedArrayInitialization;
    }
}

class DoubleCheckedLockingClassWithStaticFields {

        @LazyInitializedThreadSafeFieldReference(value = "standard dcl pattern within a static method",
                analyses = L3FieldImmutabilityAnalysis.class)
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
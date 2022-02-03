/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.immutability.field_references.lazy_initialized_field_references;

import org.opalj.br.fpcf.analyses.L0FieldAssignabilityAnalysis;
import org.opalj.fpcf.properties.immutability.field_assignability.UnsafelyLazilyInitializedField;
import org.opalj.fpcf.properties.immutability.field_assignability.LazilyInitializedField;
import org.opalj.fpcf.properties.immutability.field_assignability.AssignableField;
import org.opalj.tac.fpcf.analyses.immutability.fieldassignability.L3FieldAssignabilityAnalysis;

/**
 * This classes encompasses different variation of double checked locking as a form of lazy initialization.
 *
 * @author Tobias Roth
 *
 */
public class DoubleCheckedLocking {

    @LazilyInitializedField(value = "standard double checked locking",
            analyses = L3FieldAssignabilityAnalysis.class)
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


    @UnsafelyLazilyInitializedField(value = "the field write is not synchronized guarded",
            analyses = L3FieldAssignabilityAnalysis.class)
    private Object noSynchronizedGuard;
    public Object getNoSynchronizedGuard() {
        if(noSynchronizedGuard ==null){
            synchronized(Object.class) {
                noSynchronizedGuard = new Object();
            }
        }
        return noSynchronizedGuard;
    }

    @LazilyInitializedField(value = "a simple variation of double checked locking without the outer guard",
            analyses = L3FieldAssignabilityAnalysis.class)
    private Object onlyOneGuardWithinTheSynchronization;
    public Object getOnlyOneGuardWithinTheSynchronization() {
        synchronized(Object.class) {
            if(onlyOneGuardWithinTheSynchronization ==null){
                onlyOneGuardWithinTheSynchronization = new Object();
            }
        }
        return onlyOneGuardWithinTheSynchronization;
    }

    @UnsafelyLazilyInitializedField(value = "the field write is not synchronized guarded two times",
            analyses = L3FieldAssignabilityAnalysis.class)
    private Object twoNotSynchronizedGuards;
    public Object getTwoNotSynchronizedGuards() {
        if(twoNotSynchronizedGuards ==null){
            if(twoNotSynchronizedGuards ==null){
                twoNotSynchronizedGuards = new Object();
            }
        }
        return twoNotSynchronizedGuards;
    }

    @UnsafelyLazilyInitializedField(value = "the field is written outside the synchronized block",
            analyses = L3FieldAssignabilityAnalysis.class)
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

    @AssignableField(value = "no valid lazy initialization", analyses = L3FieldAssignabilityAnalysis.class)
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

    @AssignableField(value = "no valid lazy initialization", analyses = L3FieldAssignabilityAnalysis.class)
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

    @AssignableField(value = "no lazy initialization due to wrong guard statements",
            analyses = L3FieldAssignabilityAnalysis.class)
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

    @AssignableField(value = "no valid lazy initialization", analyses = L3FieldAssignabilityAnalysis.class)
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

    @LazilyInitializedField(value = "dcl pattern with loops in it",
            analyses = L3FieldAssignabilityAnalysis.class)
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

    @LazilyInitializedField(value = "no correct complecte dcl pattern but sufficient for thread " +
            "safety due to a correct guard in a synchronized block", analyses = L3FieldAssignabilityAnalysis.class)
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

    @UnsafelyLazilyInitializedField(value = "only a guard around the field write",
            analyses = L3FieldAssignabilityAnalysis.class)
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

    @AssignableField(value = "field write outside guards and synchronized blocks",
    analyses = L3FieldAssignabilityAnalysis.class)
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


    @LazilyInitializedField("")
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

    //@LazyInitializedThreadSafeReference("")
    @AssignableField(value = "guard not only dependent on field value",
            analyses = L3FieldAssignabilityAnalysis.class)
    private Object dclWithLockAnd;
    //private boolean lock = true;
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

    //@LazyInitializedThreadSafeReference("")
    @AssignableField(value = "guard not only dependent on field value",
            analyses = L3FieldAssignabilityAnalysis.class)
    private Object dclWithLockOr;
    //private boolean lock = true;
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

    @UnsafelyLazilyInitializedField(value = "the field read for the guard is outside the synchronized block",
            analyses = L0FieldAssignabilityAnalysis.class)
    private Object fieldReadOutsideSynchronizedBlock;

    public Object getFieldReadOutsideSynchronizedBlock(){
        Object tmpo1 = fieldReadOutsideSynchronizedBlock;
        synchronized (this){
            if(tmpo1==null)
                fieldReadOutsideSynchronizedBlock = tmpo1 = new Object();
        }
        return fieldReadOutsideSynchronizedBlock;
    }

    @UnsafelyLazilyInitializedField(value = "the field read for the guard is outside the synchronized block", analyses = L0FieldAssignabilityAnalysis.class)
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
















package org.opalj.fpcf.fixtures.cifi_benchmark.lazy_initialization.objects;

import org.opalj.fpcf.properties.immutability.field_assignability.AssignableField;
import org.opalj.fpcf.properties.immutability.field_assignability.LazilyInitializedField;
//import edu.cmu.cs.glacier.qual.Immutable;

/**
 * This class encompasses different forms of (un)correct lazy initialization in combination with try catch blocks
 */
public class TryCatch {

    //@Immutable
    @LazilyInitializedField("The exception that is caught is also thrown.")
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

    //@Immutable
    @AssignableField("no correct lazy initialization because all exceptions are caught in the inner guard")
    Object noDCLAllExceptionsCaughtInsideInnerGuard;

    public Object getNoDCL(){
        if(noDCLAllExceptionsCaughtInsideInnerGuard ==null){
            synchronized(this){
                if(noDCLAllExceptionsCaughtInsideInnerGuard ==null){
                    try{
                        noDCLAllExceptionsCaughtInsideInnerGuard = new Object();
                    }
                    catch(Exception e){ }
                }
            }
        }
        return noDCLAllExceptionsCaughtInsideInnerGuard;
    }

    //@Immutable
    @AssignableField("No correct lazy initialization because all exceptions are caught in the complete dcl")
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

    //@Immutable
    @AssignableField("no correct lazy initialization, because all exceptions are caught in the outer guard")
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

    //@Immutable
    @AssignableField("no correct lazy initialization, because the two try-catch-blocks")
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

    //@Immutable
    @AssignableField("no correct lazy initialization, because wrong exception forwarding")
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
}

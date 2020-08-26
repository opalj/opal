package org.opalj.fpcf.fixtures.immutability.reference_immutability_lazy_initialization;

import org.opalj.fpcf.properties.reference_immutability.LazyInitializedNotThreadSafeReferenceAnnotation;
import org.opalj.fpcf.properties.reference_immutability.LazyInitializedThreadSafeReferenceAnnotation;
import org.opalj.fpcf.properties.reference_immutability.MutableReferenceAnnotation;

import java.util.Random;

public class DCL {

}

class DCLint {
    @LazyInitializedThreadSafeReferenceAnnotation("")
    private int n;
    public int getN() {
        if(n==0){
            synchronized(this) {
                if(n==0){
                    n = 42;
                }
            }
        }
        return n;
    }
}

class DCLIntRandom {

    @LazyInitializedThreadSafeReferenceAnnotation("")
    private int r;

    public int getR(){
        if(r==0){
            synchronized(this){
                if(r==0){
                    r = new Random().nextInt();
                }
            }
        }
        return r;
    }
}
class NoDCLIntRandom {

    @MutableReferenceAnnotation("")
    private int r;

    public int getR(){
        if(r==0){
        r = new Random().nextInt();
        }
        return r;
    }
}

class DCLwithEarlyReturns {
    @LazyInitializedThreadSafeReferenceAnnotation("")
    private DCLwithEarlyReturns instance;
    public DCLwithEarlyReturns getInstance(){
        if(instance!=null)
            return instance;
            synchronized(this) {
                if(instance==null)
                instance = new DCLwithEarlyReturns();
            }
        return instance;
    }
    }


    class DCLChallenge {
        @LazyInitializedThreadSafeReferenceAnnotation("")
        private static org.omg.CORBA.TypeCode __typeCode = null;
        private static boolean __active = false;
        private static String  _id = "IDL:omg.org/CORBA/ValueMember:1.0";
        synchronized public static org.omg.CORBA.TypeCode type ()
        {
            if (__typeCode == null)
            {
                synchronized (org.omg.CORBA.TypeCode.class)
                {
                    if (__typeCode == null)
                    {
                        if (__active)
                        {
                            return org.omg.CORBA.ORB.init().create_recursive_tc ( _id );
                        }
                        __active = true;
                        org.omg.CORBA.StructMember[] _members0 = new org.omg.CORBA.StructMember [7];
                        org.omg.CORBA.TypeCode _tcOf_members0 = null;
                        _tcOf_members0 = org.omg.CORBA.ORB.init ().create_string_tc (0);
                        _tcOf_members0 = org.omg.CORBA.ORB.init ().create_alias_tc (org.omg.CORBA.IdentifierHelper.id (), "Identifier", _tcOf_members0);
                        _members0[0] = new org.omg.CORBA.StructMember (
                                "name",
                                _tcOf_members0,
                                null);
                        _tcOf_members0 = org.omg.CORBA.ORB.init ().create_string_tc (0);
                        _tcOf_members0 = org.omg.CORBA.ORB.init ().create_alias_tc (org.omg.CORBA.RepositoryIdHelper.id (), "RepositoryId", _tcOf_members0);
                        _members0[1] = new org.omg.CORBA.StructMember (
                                "id",
                                _tcOf_members0,
                                null);
                        _tcOf_members0 = org.omg.CORBA.ORB.init ().create_string_tc (0);
                        _tcOf_members0 = org.omg.CORBA.ORB.init ().create_alias_tc (org.omg.CORBA.RepositoryIdHelper.id (), "RepositoryId", _tcOf_members0);
                        _members0[2] = new org.omg.CORBA.StructMember (
                                "defined_in",
                                _tcOf_members0,
                                null);
                        _tcOf_members0 = org.omg.CORBA.ORB.init ().create_string_tc (0);
                        _tcOf_members0 = org.omg.CORBA.ORB.init ().create_alias_tc (org.omg.CORBA.VersionSpecHelper.id (), "VersionSpec", _tcOf_members0);
                        _members0[3] = new org.omg.CORBA.StructMember (
                                "version",
                                _tcOf_members0,
                                null);
                        _tcOf_members0 = org.omg.CORBA.ORB.init ().get_primitive_tc (org.omg.CORBA.TCKind.tk_TypeCode);
                        _members0[4] = new org.omg.CORBA.StructMember (
                                "type",
                                _tcOf_members0,
                                null);
                        _tcOf_members0 = org.omg.CORBA.IDLTypeHelper.type ();
                        _members0[5] = new org.omg.CORBA.StructMember (
                                "type_def",
                                _tcOf_members0,
                                null);
                        _tcOf_members0 = org.omg.CORBA.ORB.init ().get_primitive_tc (org.omg.CORBA.TCKind.tk_short);
                        _tcOf_members0 = org.omg.CORBA.ORB.init ().create_alias_tc (org.omg.CORBA.VisibilityHelper.id (), "Visibility", _tcOf_members0);
                        _members0[6] = new org.omg.CORBA.StructMember (
                                "access",
                                _tcOf_members0,
                                null);
                        __typeCode = org.omg.CORBA.ORB.init ().create_struct_tc (org.omg.CORBA.ValueMemberHelper.id (), "ValueMember", _members0);
                        __active = false;
                    }
                }
            }
            return __typeCode;
        }
    }

class DCL5 {

    @LazyInitializedThreadSafeReferenceAnnotation("")
    private DCL5 instance;

    public DCL5 DCL5(){
        if(instance==null){
            synchronized(this){
                if(instance==null){
                    try{
                        instance = new DCL5();
                    }
                    catch (Exception e)
                    {
                        throw e;
                    }
                }
            }
        }
        return instance;
    }
}

class DCL6 {

    @LazyInitializedThreadSafeReferenceAnnotation("")
    private DCL6 instance;

    public DCL6 getInstance() throws Exception{
        if(instance==null){
            synchronized(this){
                if(instance==null){
                    instance = new DCL6();
                }
            }
        }
        return instance;
    }
}

class SimpleLockingSynchronizedBlock {
    @LazyInitializedThreadSafeReferenceAnnotation("")
    private SimpleLockingSynchronizedBlock instance;
    private SimpleLockingSynchronizedBlock() {}
    public SimpleLockingSynchronizedBlock getInstance(){
        synchronized(this){
            if(instance==null)
                instance = new SimpleLockingSynchronizedBlock();
        }
        return instance;
    }
}

class SimpleLockingSynchronizedFunction1 {
    @LazyInitializedThreadSafeReferenceAnnotation("")
    private SimpleLockingSynchronizedFunction1 instance;
    private SimpleLockingSynchronizedFunction1() {}
    public synchronized SimpleLockingSynchronizedFunction1 getInstance() {
        if(instance ==null)
            instance = new SimpleLockingSynchronizedFunction1();
        return instance;
    }
}




class NoDCL1 {

    @MutableReferenceAnnotation("")
    NoDCL1 instance;

    public NoDCL1 NoDCL1(){
        if(instance==null){
            synchronized(this){
                if(instance==null){
                    try{
                        instance = new NoDCL1();
                    }
                    catch(Exception e){}
                }}}
        return instance;
    }

}

class NoDCL2 {

    @MutableReferenceAnnotation("")
    NoDCL2 instance;

    public NoDCL2 NoDCL1(){
        try{
            if(instance==null){
                synchronized(this){
                    if(instance==null){

                        instance = new NoDCL2();
                    }

                }}}catch(Exception e){}
        return instance;
    }

}

class NoDCL3 {

    @MutableReferenceAnnotation("")
    NoDCL3 instance;

    public NoDCL3 NoDCL1(){

        if(instance==null){
            try{
                synchronized(this){
                    if(instance==null){

                        instance = new NoDCL3();
                    }
                }}catch(Exception e){}}
        return instance;
    }

}

class NoDCL4 {

    @MutableReferenceAnnotation("")
    NoDCL4 instance;

    public NoDCL4 NoDCL1(){

        if(instance==null){
            try{
                synchronized(this){
                    if(instance==null){
                        try{
                            instance = new NoDCL4();
                        }
                        catch (Exception e)
                        {
                            throw e;
                        }
                    }
                }}catch(Exception e){}}
        return instance;
    }

}


class NoDCL6 {

    @MutableReferenceAnnotation("")
    NoDCL6 instance;

    public NoDCL6 NoDCL6() throws IndexOutOfBoundsException{
        if(instance==null){
            synchronized(this){
                if(instance==null){
                    try{
                        instance = new NoDCL6();
                    }
                    catch (Exception e)
                    {
                        throw new IndexOutOfBoundsException();
                    }
                }
            }
        }
        return instance;
    }
}

class DoubleCheckedLockingClassWithStaticFields {

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

class DoubleCheckedLockingClassArray1 {

    @LazyInitializedThreadSafeReferenceAnnotation("")
    private Object[] instance;
    public Object[] getInstance() {
        if(instance==null){
            synchronized(this) {
                if(instance==null){
                    instance = new Object[10];
                }
            }
        }
        return instance;
    }
}

class DoubleCheckedLockingClass19 {

    @LazyInitializedNotThreadSafeReferenceAnnotation("")
    private DoubleCheckedLockingClass19 instance;

    public DoubleCheckedLockingClass19 getInstance() {
        if (instance == null) {
            synchronized (this) {
                if (instance == null) {
                }
            }
            instance = new DoubleCheckedLockingClass19();
        }
        return instance;
    }
}

class ArrayLazyInitializationNotThreadSafe {
    @LazyInitializedNotThreadSafeReferenceAnnotation("During the initialization phase there is no lock")
    private int[] values;

    public int[] getValues(){
        if(values==null){
            values = new int[] {1,2,3,4,5,6,7,8,9,10};
        }
        return values;
    }
}

class ArrayLazyInitializationThreadSafe {
    @LazyInitializedThreadSafeReferenceAnnotation("it is a lock via the synchronized method")
    private int[] values;

    public synchronized  int[] getValues(){
        if(values==null){
            values = new int[] {1,2,3,4,5,6,7,8,9,10};
        }
        return values;
    }
}

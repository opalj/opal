package org.opalj.fpcf.fixtures.immutability.reference_immutability_lazy_initialization;

import org.opalj.fpcf.properties.reference_immutability.LazyInitializedThreadSafeReferenceAnnotation;

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

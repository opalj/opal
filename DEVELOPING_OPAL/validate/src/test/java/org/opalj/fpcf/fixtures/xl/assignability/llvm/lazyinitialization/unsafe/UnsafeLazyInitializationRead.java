package org.opalj.fpcf.fixtures.xl.assignability.llvm.lazyinitialization.unsafe;

import org.opalj.fpcf.properties.immutability.field_assignability.AssignableField;

public class UnsafeLazyInitializationRead {

    public static void main(String[] args){
        UnsafeLazyInitializationRead slr = new UnsafeLazyInitializationRead();
        Object o = slr.getO();
        System.out.println(o);
    }

    native Object getO();

    @AssignableField("field o can be read be c code")
    private Object o;

    public Object returnO() {
        if (this.o==null)
            o = new Object();
        return o;
    }
}
package org.opalj.fpcf.fixtures.xl.assignability.llvm.lazyinitialization.unsafe;

import org.opalj.fpcf.properties.immutability.field_assignability.AssignableField;

public class UnsafeLazyInitializationReadAndWritten {

    public static void main(String[] args){
        UnsafeLazyInitializationReadAndWritten slrw = new UnsafeLazyInitializationReadAndWritten();
        Object o = slrw.getO();
        slrw.setO(new Object());
        System.out.println(o);
    }

    @AssignableField("field o can be read and written by C code")
    private Object o;

    native Object getO();
    native void setO(Object o);

    public Object returnO() {
        if (this.o==null)
            o = new Object();
        return o;
    }
}
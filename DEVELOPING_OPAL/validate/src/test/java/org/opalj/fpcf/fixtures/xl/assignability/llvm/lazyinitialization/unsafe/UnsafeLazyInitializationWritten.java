package org.opalj.fpcf.fixtures.xl.assignability.llvm.lazyinitialization.unsafe;

import org.opalj.fpcf.properties.immutability.field_assignability.AssignableField;
import org.opalj.fpcf.properties.immutability.field_assignability.LazilyInitializedField;

public class UnsafeLazyInitializationWritten {

    public static void main(String[] args){
        UnsafeLazyInitializationWritten slw = new UnsafeLazyInitializationWritten();
        slw.setO(new Object());
    }

    @AssignableField("field o can be written by C code")
    private Object o;

    native void setO(Object o);

    public Object returnO() {
        if (this.o==null)
            o = new Object();
        return o;
    }
}
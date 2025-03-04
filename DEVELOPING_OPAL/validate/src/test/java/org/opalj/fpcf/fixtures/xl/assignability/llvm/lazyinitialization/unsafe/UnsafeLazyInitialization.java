package org.opalj.fpcf.fixtures.xl.assignability.llvm.lazyinitialization.unsafe;

import org.opalj.fpcf.properties.immutability.field_assignability.LazilyInitializedField;
import org.opalj.fpcf.properties.immutability.field_assignability.UnsafelyLazilyInitializedField;

public class UnsafeLazyInitialization {

    @UnsafelyLazilyInitializedField("field o is lazily initialized")
    private Object o;

    public Object returnO() {
        if (this.o==null)
            o = new Object();
        return o;
    }
}
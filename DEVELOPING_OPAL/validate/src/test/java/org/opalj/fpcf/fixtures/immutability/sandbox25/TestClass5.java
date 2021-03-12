package org.opalj.fpcf.fixtures.immutability.sandbox25;

import org.opalj.fpcf.properties.immutability.fields.ShallowImmutableField;

public class TestClass5 {
    @ShallowImmutableField("")
    private static final Object[] o = new Object[0];

    public Object identity(Object o){
        return o;
    }

    public Object getO(){
        return this.o;
    }
}

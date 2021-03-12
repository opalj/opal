package org.opalj.fpcf.fixtures.immutability.sandbox22;

import org.opalj.fpcf.properties.immutability.fields.ShallowImmutableField;

public class EscapeFailureTest {

    @ShallowImmutableField("")
    private static final Object[] o;

    static {
        o = new Object[]{new MutableClass()};
    }
}


class MutableClass {
    public int n = 5;
}

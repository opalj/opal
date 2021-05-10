package org.opalj.fpcf.fixtures.immutability.sandbox22;

import org.opalj.fpcf.properties.immutability.fields.NonTransitivelyImmutableField;

public class EscapeFailureTest {

    @NonTransitivelyImmutableField("")
    private static final Object[] o;

    static {
        o = new Object[]{new MutableClass()};
    }
}


class MutableClass {
    public int n = 5;
}

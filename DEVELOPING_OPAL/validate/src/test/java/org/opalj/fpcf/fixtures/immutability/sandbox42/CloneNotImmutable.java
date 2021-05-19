package org.opalj.fpcf.fixtures.immutability.sandbox42;

import org.opalj.fpcf.properties.immutability.field_assignability.AssignableField;

public class CloneNotImmutable {

    @AssignableField("")
    private int mutableField;

    public static CloneNotImmutable instance;
    public CloneNotImmutable clone() {
        CloneNotImmutable c = new CloneNotImmutable();

        c.mutableField = mutableField;
        instance = c;
        return c;
    }
}

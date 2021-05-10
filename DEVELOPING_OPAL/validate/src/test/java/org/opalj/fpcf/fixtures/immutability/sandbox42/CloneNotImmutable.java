package org.opalj.fpcf.fixtures.immutability.sandbox42;

import org.opalj.fpcf.properties.immutability.references.AssignableFieldReference;

public class CloneNotImmutable {

    @AssignableFieldReference("")
    private int mutableField;

    public static CloneNotImmutable instance;
    public CloneNotImmutable clone() {
        CloneNotImmutable c = new CloneNotImmutable();

        c.mutableField = mutableField;
        instance = c;
        return c;
    }
}

package org.opalj.fpcf.fixtures.immutability.sandbox42;

import org.opalj.fpcf.properties.immutability.fields.MutableField;
import org.opalj.fpcf.properties.immutability.references.ImmutableFieldReference;
import org.opalj.fpcf.properties.immutability.references.MutableFieldReference;

public class CloneNotImmutable {

    @MutableFieldReference("")
    private int mutableField;

    public static CloneNotImmutable instance;
    public CloneNotImmutable clone() {
        CloneNotImmutable c = new CloneNotImmutable();

        c.mutableField = mutableField;
        instance = c;
        return c;
    }
}

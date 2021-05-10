package org.opalj.fpcf.fixtures.immutability.sandbox42;

import org.opalj.fpcf.properties.immutability.references.NonAssignableFieldReference;

public class Clone {

    @NonAssignableFieldReference("")
    private int i;

    public int getI() {
        return i;
    }

    Clone instance = new Clone();

    public Clone clone(){
        Clone c = new Clone();
        c.i = i;
        return c;
    }
}

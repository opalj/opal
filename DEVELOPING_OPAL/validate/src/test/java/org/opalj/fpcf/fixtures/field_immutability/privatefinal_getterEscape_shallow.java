package org.opalj.fpcf.fixtures.field_immutability;

public class privatefinal_getterEscape_shallow {
    public TrivialMutableClass getTmc() {
        return tmc;
    }

    private final TrivialMutableClass tmc = new TrivialMutableClass();
}

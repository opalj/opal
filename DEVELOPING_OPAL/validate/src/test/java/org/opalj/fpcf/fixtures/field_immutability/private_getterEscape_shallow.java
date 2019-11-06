package org.opalj.fpcf.fixtures.field_immutability;

public class private_getterEscape_shallow {
    public TrivialMutableClass getTmc() {
        return tmc;
    }

    private TrivialMutableClass tmc = new TrivialMutableClass();

}

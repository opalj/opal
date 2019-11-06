package org.opalj.fpcf.fixtures.field_immutability;

public class privateFinalFieldBlank_costructorEscape_shallow {

    private final TrivialMutableClass tmc;

    public privateFinalFieldBlank_costructorEscape_shallow(TrivialMutableClass tmc) {
        this.tmc = tmc;
    }
}

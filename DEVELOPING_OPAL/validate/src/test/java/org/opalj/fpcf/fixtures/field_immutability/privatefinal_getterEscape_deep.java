package org.opalj.fpcf.fixtures.field_immutability;

public class privatefinal_getterEscape_deep {
    public FinalEmptyClass getFec() {
        return fec;
    }

    private final FinalEmptyClass fec = new FinalEmptyClass();

}

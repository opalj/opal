package org.opalj.fpcf.fixtures.field_immutability;

import org.opalj.fpcf.properties.field_immutability.MutableFieldAnnotation;
import org.opalj.fpcf.properties.reference_immutability.MutableReferenceAnnotation;

public class private_setter_deep {

    public void setFec(FinalEmptyClass fec) {
        this.fec = fec;
    }

    private FinalEmptyClass fec = new FinalEmptyClass();

}

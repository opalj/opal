package org.opalj.fpcf.fixtures.field_immutability;

import org.opalj.fpcf.properties.field_immutability.MutableFieldAnnotation;
import org.opalj.fpcf.properties.reference_immutability.MutableReferenceAnnotation;

public class private_setter_shallow {

    public void setTmc(TrivialMutableClass tmc) {
        this.tmc = tmc;
    }

    private TrivialMutableClass tmc = new TrivialMutableClass();

}

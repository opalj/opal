package org.opalj.fpcf.fixtures.field_immutability;

import org.opalj.fpcf.properties.class_immutability.MutableClassAnnotation;
import org.opalj.fpcf.properties.field_immutability.MutableFieldAnnotation;
import org.opalj.fpcf.properties.reference_immutability.MutableReferenceAnnotation;

@MutableClassAnnotation("Because it has Mutable Fields")
public class private_setter_deep {

    public void setFec(FinalEmptyClass fec) {
        this.fec = fec;
    }

    @MutableFieldAnnotation("Because of Mutable Reference")
    @MutableReferenceAnnotation("Not final field could be set via setter")
    private FinalEmptyClass fec = new FinalEmptyClass();

}

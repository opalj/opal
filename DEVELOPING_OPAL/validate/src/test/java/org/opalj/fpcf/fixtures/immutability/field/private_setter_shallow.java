package org.opalj.fpcf.fixtures.immutability.field;

import org.opalj.fpcf.properties.class_immutability.MutableClassAnnotation;
import org.opalj.fpcf.properties.field_immutability.MutableFieldAnnotation;
import org.opalj.fpcf.properties.reference_immutability.MutableReferenceAnnotation;
import org.opalj.fpcf.properties.type_immutability.MutableTypeAnnotation;

@MutableTypeAnnotation("Because of not final class")
@MutableClassAnnotation("Because it has Mutable Fields")
public class private_setter_shallow {

    public void setTmc(TrivialMutableClass tmc) {
        this.tmc = tmc;
    }

    @MutableFieldAnnotation("Because of Mutable Reference")
    @MutableReferenceAnnotation("Not final field could be set via setter")
    private TrivialMutableClass tmc = new TrivialMutableClass();

}

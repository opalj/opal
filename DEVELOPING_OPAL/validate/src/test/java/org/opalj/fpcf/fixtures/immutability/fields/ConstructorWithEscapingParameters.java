package org.opalj.fpcf.fixtures.immutability.fields;

import org.opalj.fpcf.properties.field_immutability.DeepImmutableFieldAnnotation;
import org.opalj.fpcf.properties.field_immutability.ShallowImmutableFieldAnnotation;
import org.opalj.fpcf.properties.reference_immutability.ImmutableReferenceAnnotation;

public class ConstructorWithEscapingParameters {

    @ShallowImmutableFieldAnnotation("The field is init")
    @ImmutableReferenceAnnotation("The field is only assigned in the constructor.")
    private TrivialClass tc1;

    @DeepImmutableFieldAnnotation("The construtor pararameter of the assigned object not escape")
    @ImmutableReferenceAnnotation("The field is only assigned in the constructor.")
    private TrivialClass tc2;

    ConstructorWithEscapingParameters(Object o1, Object o2){
        tc1 = new TrivialClass(o1, o2);
        tc2 = new TrivialClass(new Object(), new Object());
    }

}

class TrivialClass{
    private Object o1;
    private Object o2;
    public TrivialClass(Object o1, Object o2){
        this.o1 = o1;
        this.o2 = 02;
    }
}

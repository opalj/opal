package org.opalj.fpcf.fixtures.class_immutability;

import org.opalj.fpcf.properties.class_immutability.DependentImmutableClassAnnotation;
import org.opalj.fpcf.properties.field_immutability.DependentImmutableFieldAnnotation;
import org.opalj.fpcf.properties.reference_immutability.ImmutableReferenceAnnotation;
import org.opalj.fpcf.properties.type_immutability.DependentImmutableTypeAnnotation;


@DependentImmutableTypeAnnotation("")
@DependentImmutableClassAnnotation("")
public final class DependentClassWithGenericField_deep11<T1> {

    @DependentImmutableFieldAnnotation(value = "", genericString = "T1")
    @ImmutableReferenceAnnotation("")
    private T1 t1;

    @DependentImmutableFieldAnnotation(value = "dep imm field", genericString = "T1")
    @ImmutableReferenceAnnotation("eff imm ref")
    private DependentClassWithGenericField_deep1<T1> gc;

    public DependentClassWithGenericField_deep11(T1 t1) {
        this.t1 = t1;
        gc = new DependentClassWithGenericField_deep1<T1>(t1);
    }


}




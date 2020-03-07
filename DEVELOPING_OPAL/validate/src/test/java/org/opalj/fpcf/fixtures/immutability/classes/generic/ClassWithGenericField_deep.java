package org.opalj.fpcf.fixtures.immutability.classes.generic;

import org.opalj.fpcf.properties.class_immutability.DeepImmutableClassAnnotation;
import org.opalj.fpcf.properties.class_immutability.DependentImmutableClassAnnotation;
import org.opalj.fpcf.properties.field_immutability.DeepImmutableFieldAnnotation;
import org.opalj.fpcf.properties.reference_immutability.ImmutableReferenceAnnotation;
import org.opalj.fpcf.properties.type_immutability.DeepImmutableTypeAnnotation;
import org.opalj.fpcf.properties.type_immutability.DependentImmutableTypeAnnotation;

@DeepImmutableTypeAnnotation("")
@DeepImmutableClassAnnotation("")
public final class ClassWithGenericField_deep {
    @DeepImmutableFieldAnnotation("deep imm field")
    @ImmutableReferenceAnnotation("eff imm ref")
        private Generic_class2<FinalEmptyClass2,FinalEmptyClass2,FinalEmptyClass2,FinalEmptyClass2,FinalEmptyClass2> gc =
                new Generic_class2<FinalEmptyClass2,FinalEmptyClass2,FinalEmptyClass2,FinalEmptyClass2,FinalEmptyClass2>
                        (new FinalEmptyClass2(), new FinalEmptyClass2(), new FinalEmptyClass2(), new FinalEmptyClass2(), new FinalEmptyClass2());
}

@DependentImmutableTypeAnnotation("")
@DependentImmutableClassAnnotation("")
final class Generic_class2<T1,T2,T3,T4,T5> {

    private T1 t1;

    private T2 t2;

    private T3 t3;

    private T4 t4;

    private T5 t5;

    public Generic_class2(T1 t1, T2 t2, T3 t3, T4 t4, T5 t5){
        this.t1 = t1;
        this.t2 = t2;
        this.t3 = t3;
        this.t4 = t4;
        this.t5 = t5;
    }

}
@DeepImmutableTypeAnnotation("")
@DeepImmutableClassAnnotation("")
final class FinalEmptyClass2 {

}




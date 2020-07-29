package org.opalj.fpcf.fixtures.immutability.classes.generic;

import org.opalj.fpcf.properties.class_immutability.DeepImmutableClassAnnotation;
import org.opalj.fpcf.properties.class_immutability.DependentImmutableClassAnnotation;
import org.opalj.fpcf.properties.class_immutability.MutableClassAnnotation;
import org.opalj.fpcf.properties.class_immutability.ShallowImmutableClassAnnotation;
import org.opalj.fpcf.properties.field_immutability.DeepImmutableFieldAnnotation;
import org.opalj.fpcf.properties.field_immutability.DependentImmutableFieldAnnotation;
import org.opalj.fpcf.properties.field_immutability.MutableFieldAnnotation;
import org.opalj.fpcf.properties.field_immutability.ShallowImmutableFieldAnnotation;
import org.opalj.fpcf.properties.reference_immutability.ImmutableReferenceAnnotation;
import org.opalj.fpcf.properties.reference_immutability.MutableReferenceAnnotation;
import org.opalj.fpcf.properties.type_immutability.DeepImmutableTypeAnnotation;
import org.opalj.fpcf.properties.type_immutability.DependentImmutableTypeAnnotation;
import org.opalj.fpcf.properties.type_immutability.MutableTypeAnnotation;


@DependentImmutableTypeAnnotation("")
@DependentImmutableClassAnnotation("")
public final class DependentClassWithGenericField_deep1<T1> {

    @DependentImmutableFieldAnnotation(value = "", genericString = "T1")
    @ImmutableReferenceAnnotation("")
    private T1 t1;

    @DependentImmutableFieldAnnotation(value = "dep imm field", genericString = "T1")
    @ImmutableReferenceAnnotation("eff imm ref")
    private SimpleGenericClass<T1,FinalEmptyClass,FinalEmptyClass> gc;

    public DependentClassWithGenericField_deep1(T1 t1) {
        this.t1 = t1;
        gc = new SimpleGenericClass<T1, FinalEmptyClass,FinalEmptyClass>
                (t1, new FinalEmptyClass(), new FinalEmptyClass());
    }
}

@DeepImmutableTypeAnnotation("")
@DeepImmutableClassAnnotation("")
final class DependentClassWithGenericField_deep01 {

    @DeepImmutableFieldAnnotation(value = "")
    private FinalEmptyClass fec;

    @DeepImmutableFieldAnnotation("")
    @ImmutableReferenceAnnotation("eff imm ref")
    private SimpleGenericClass<FinalEmptyClass,FinalEmptyClass,FinalEmptyClass> gc;

    public DependentClassWithGenericField_deep01(FinalEmptyClass fec) {
        this.fec = fec;
        gc = new SimpleGenericClass<FinalEmptyClass, FinalEmptyClass,FinalEmptyClass>
                (fec, new FinalEmptyClass(), new FinalEmptyClass());
    }
}

@DependentImmutableTypeAnnotation("")
@DependentImmutableClassAnnotation("")
final class DependentClassWithGenericField_deep2<T1> {

    @DependentImmutableFieldAnnotation(value = "dep imm field", genericString = "T1")
    @ImmutableReferenceAnnotation("eff imm ref")
    private SimpleGenericClass<T1,FinalEmptyClass,FinalEmptyClass> sgc;

    public DependentClassWithGenericField_deep2(T1 t1) {
        sgc = new SimpleGenericClass<T1,FinalEmptyClass,FinalEmptyClass>
                (t1, new FinalEmptyClass(), new FinalEmptyClass());
    }

}

@DependentImmutableTypeAnnotation("")
@DependentImmutableClassAnnotation("")
final class DependentClassWithGenericField_deep11<T1> {

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

@MutableTypeAnnotation("")
@ShallowImmutableClassAnnotation("")
class DependentClassWithGenericTypeArgument<T> {
    private final SimpleGenericClass<SimpleMutableClass,T,T> sgc;
    DependentClassWithGenericTypeArgument(SimpleGenericClass<SimpleMutableClass,T,T> sgc) {
        this.sgc = sgc;
    }
}

@DeepImmutableTypeAnnotation("")
@DeepImmutableClassAnnotation("It has no fields and is final")
final class FinalEmptyClass {

}

@DependentImmutableTypeAnnotation("Dependent Immutability of T1,...,T5")
@DependentImmutableClassAnnotation("Dependent Immutability of T1,...,T5")
final class SimpleGenericClass<T1,T2,T3> {
    @DependentImmutableFieldAnnotation(value = "T1",genericString = "T1")
    @ImmutableReferenceAnnotation("effectively")
    private T1 t1;
    @DependentImmutableFieldAnnotation(value = "T2", genericString =  "T2")
    @ImmutableReferenceAnnotation("effectively")
    private T2 t2;
    @DependentImmutableFieldAnnotation(value = "T3", genericString = "T3")
    @ImmutableReferenceAnnotation("effectively")
    private T3 t3;

    public SimpleGenericClass(T1 t1, T2 t2, T3 t3){
        this.t1 = t1;
        this.t2 = t2;
        this.t3 = t3;
    }
}

@DependentImmutableTypeAnnotation("")
@DependentImmutableClassAnnotation("")
final class GenericAndDeepImmutableFields<T1, T2> {

    @DependentImmutableFieldAnnotation(value = "T1", genericString = "T1")
    @ImmutableReferenceAnnotation("")
    private T1 t1;

    @DependentImmutableFieldAnnotation(value = "T2", genericString = "T2")
    @ImmutableReferenceAnnotation("")
    private T2 t2;

    @DeepImmutableFieldAnnotation("")
    @ImmutableReferenceAnnotation("")
    private FinalEmptyClass fec;

    GenericAndDeepImmutableFields(T1 t1, T2 t2, FinalEmptyClass fec){
        this.t1 = t1;
        this.t2 = t2;
        this.fec = fec;
    }
}

@MutableTypeAnnotation("")
@MutableClassAnnotation("Because of mutable field")
class GenericAndMutableFields<T1, T2> {
    @MutableFieldAnnotation("Because of mutable reference")
    @MutableReferenceAnnotation("Because of public field")
    public T1 t1;
    @DependentImmutableFieldAnnotation(value = "Because of generic type", genericString = "T2")
    @ImmutableReferenceAnnotation("Because of effectively immutable final")
    private T2 t2;
    GenericAndMutableFields(T1 t1, T2 t2){
        this.t1 = t1;
        this.t2 = t2;
    }
}

@MutableTypeAnnotation("")
@ShallowImmutableClassAnnotation("")
class GenericAndShallowImmutableFields<T1, T2> {

    @DependentImmutableFieldAnnotation(value = "T1", genericString = "T1")
    private T1 t1;
    @DependentImmutableFieldAnnotation(value = "T2", genericString = "T2")
    private T2 t2;
    @ShallowImmutableFieldAnnotation("")
    private SimpleMutableClass  smc;
    GenericAndShallowImmutableFields(T1 t1, T2 t2, SimpleMutableClass smc){
        this.t1 = t1;
        this.t2 = t2;
        this.smc = smc;
    }

}

@MutableTypeAnnotation("")
@DeepImmutableClassAnnotation("")
class GenericClassWithDeepImmParam<A extends FinalEmptyClass> {
    @DeepImmutableFieldAnnotation("")
    private A a;
    GenericClassWithDeepImmParam(A a) {
        this.a = a;
    }
}

@MutableTypeAnnotation("")
@ShallowImmutableClassAnnotation("")
class GenericClassWithExtendingFinalMutableType<A extends FinalMutableClass> {

    @ShallowImmutableFieldAnnotation("")
    @ImmutableReferenceAnnotation("")
    private A a;

    GenericClassWithExtendingFinalMutableType(A a){
        this.a = a;
    }
}

@MutableTypeAnnotation("")
@MutableClassAnnotation("")
final class FinalMutableClass{
    public int n = 0;
}

@DeepImmutableTypeAnnotation("")
@DeepImmutableClassAnnotation("")
final class ClassWithGenericField_deep {
    @DeepImmutableFieldAnnotation("deep imm field")
    @ImmutableReferenceAnnotation("eff imm ref")
    private SimpleGenericClass<FinalEmptyClass,FinalEmptyClass,FinalEmptyClass> gc =
            new SimpleGenericClass<FinalEmptyClass,FinalEmptyClass,FinalEmptyClass>
                    (new FinalEmptyClass(), new FinalEmptyClass(), new FinalEmptyClass());
}

@MutableTypeAnnotation("")
@MutableClassAnnotation("")
final class ClassWithGenericField_mutable {
    @MutableFieldAnnotation("deep imm field")
    @MutableReferenceAnnotation("eff imm ref")
    public SimpleGenericClass<FinalEmptyClass,FinalEmptyClass,FinalEmptyClass> gc =
            new SimpleGenericClass<FinalEmptyClass,FinalEmptyClass,FinalEmptyClass>
                    (new FinalEmptyClass(), new FinalEmptyClass(), new FinalEmptyClass());
}

@DeepImmutableTypeAnnotation("")
@DeepImmutableClassAnnotation("")
final class ClassWithGenericField_shallow {
    @DeepImmutableFieldAnnotation("")
    @ImmutableReferenceAnnotation("")
    private SimpleGenericClass<SimpleMutableClass,FinalEmptyClass,FinalEmptyClass> gc =
            new SimpleGenericClass<SimpleMutableClass,FinalEmptyClass,FinalEmptyClass>
                    (new SimpleMutableClass(), new FinalEmptyClass(), new FinalEmptyClass());
}

@MutableTypeAnnotation("")
@MutableClassAnnotation("")
class DependentClassInheritingMutableOne<T> extends SimpleMutableClass {
    private final T field;
    public DependentClassInheritingMutableOne(T field) {
        this.field = field;
    }
}

@MutableTypeAnnotation("")
@MutableClassAnnotation("")
class SimpleMutableClass{ public int n = 0;}







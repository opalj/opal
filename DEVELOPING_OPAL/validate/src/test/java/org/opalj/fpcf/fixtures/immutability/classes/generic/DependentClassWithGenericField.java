/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.immutability.classes.generic;

import org.opalj.br.fpcf.analyses.L0ClassImmutabilityAnalysis;
import org.opalj.br.fpcf.analyses.L0FieldImmutabilityAnalysis;
import org.opalj.br.fpcf.analyses.L0TypeImmutabilityAnalysis;
import org.opalj.fpcf.properties.immutability.classes.DeepImmutableClass;
import org.opalj.fpcf.properties.immutability.classes.DependentImmutableClass;
import org.opalj.fpcf.properties.immutability.classes.MutableClass;
import org.opalj.fpcf.properties.immutability.classes.ShallowImmutableClass;
import org.opalj.fpcf.properties.immutability.fields.DeepImmutableField;
import org.opalj.fpcf.properties.immutability.fields.DependentImmutableField;
import org.opalj.fpcf.properties.immutability.fields.MutableField;
import org.opalj.fpcf.properties.immutability.fields.ShallowImmutableField;
import org.opalj.fpcf.properties.immutability.references.ImmutableFieldReference;
import org.opalj.fpcf.properties.immutability.references.MutableFieldReference;
import org.opalj.fpcf.properties.immutability.types.DeepImmutableType;
import org.opalj.fpcf.properties.immutability.types.DependentImmutableType;
import org.opalj.fpcf.properties.immutability.types.MutableType;
import org.opalj.fpcf.properties.immutability.types.ShallowImmutableType;
import org.opalj.tac.fpcf.analyses.L1FieldImmutabilityAnalysis;
import org.opalj.tac.fpcf.analyses.L2FieldImmutabilityAnalysis;
import org.opalj.tac.fpcf.analyses.immutability.L1ClassImmutabilityAnalysis;
import org.opalj.tac.fpcf.analyses.immutability.L1TypeImmutabilityAnalysis;
import org.opalj.tac.fpcf.analyses.immutability.L3FieldImmutabilityAnalysis;
import org.opalj.tac.fpcf.analyses.immutability.fieldreference.L0FieldReferenceImmutabilityAnalysis;


@DependentImmutableType("")
@DependentImmutableClass("")
public final class DependentClassWithGenericField<T> {

    @MutableField(value="can not handle effective immutability", analyses = L0FieldImmutabilityAnalysis.class)
    @ShallowImmutableField(value = "can not handle generics",
            analyses = {L1FieldImmutabilityAnalysis.class,
                    L2FieldImmutabilityAnalysis.class})
    @DependentImmutableField(value = "immutable field reference with generic type T",
    analyses = L3FieldImmutabilityAnalysis.class)
    @ImmutableFieldReference(value = "effective immutable reference", analyses = L3FieldImmutabilityAnalysis.class)
    private T t;

    @MutableField(value="can not handle effective immutability", analyses = L0FieldImmutabilityAnalysis.class)
    @ShallowImmutableField(value = "can not handle generics",
    analyses = {L1FieldImmutabilityAnalysis.class, L2FieldImmutabilityAnalysis.class})
    @DependentImmutableField(value = "dep imm field", analyses = L3FieldImmutabilityAnalysis.class)
    @ImmutableFieldReference(value = "effective immutable reference",
            analyses = L0FieldReferenceImmutabilityAnalysis.class)
    private SimpleGenericClass<T,FinalEmptyClass,FinalEmptyClass> gc;

    public DependentClassWithGenericField(T t) {
        this.t = t;
        gc = new SimpleGenericClass<>(t, new FinalEmptyClass(), new FinalEmptyClass());
    }
}

@DeepImmutableType(value = "class is not extensible", analyses = L1TypeImmutabilityAnalysis.class)
@DeepImmutableClass(value = "has only deep immutable fields", analyses = L1ClassImmutabilityAnalysis.class)
final class DependentClassWithGenericField_deep01 {

    @DeepImmutableField(value = "immutable field reference with deep immutable type",
            analyses = L3FieldImmutabilityAnalysis.class)
    @ImmutableFieldReference(value = "effective immutable reference",
            analyses = L0FieldReferenceImmutabilityAnalysis.class)
    private FinalEmptyClass fec;

    @DeepImmutableField(value = "the genericity was conretised with deep immutable types",
    analyses = L3FieldImmutabilityAnalysis.class)
    @ImmutableFieldReference(value = "effective immutable field reference",
            analyses = L3FieldImmutabilityAnalysis.class)
    private SimpleGenericClass<FinalEmptyClass,FinalEmptyClass,FinalEmptyClass> gc;

    public DependentClassWithGenericField_deep01(FinalEmptyClass fec) {
        this.fec = fec;
        gc = new SimpleGenericClass<>(fec, new FinalEmptyClass(), new FinalEmptyClass());
    }
}

@DependentImmutableType(value = "has only dependent immutable fields and is not extensible",
        analyses = L1TypeImmutabilityAnalysis.class)
@DependentImmutableClass(value = "has only dependent immutable fields and is not extensible",
analyses = L1ClassImmutabilityAnalysis.class)
final class DependentClassWithGenericFieldWithOneLeftGenericParameter<T> {

    @DependentImmutableField(value = "has one left generic parameter T and no shallow or mutable types", 
            analyses = L3FieldImmutabilityAnalysis.class)
    @ImmutableFieldReference(value = "effective immutable reference", 
            analyses = L0FieldReferenceImmutabilityAnalysis.class)
    private SimpleGenericClass<T,FinalEmptyClass,FinalEmptyClass> sgc;

    public DependentClassWithGenericFieldWithOneLeftGenericParameter(T t) {
        sgc = new SimpleGenericClass<>(t, new FinalEmptyClass(), new FinalEmptyClass());
    }

}



@MutableType("")
@ShallowImmutableClass("")
class DependentClassWithMutableGenericTypeArgument<T> {

    @ShallowImmutableField(value = "mutable type",
            analyses = {L0FieldImmutabilityAnalysis.class, L1FieldImmutabilityAnalysis.class,
            L2FieldImmutabilityAnalysis.class, L3FieldImmutabilityAnalysis.class})
    @ImmutableFieldReference(value = "final field", analyses = L0FieldReferenceImmutabilityAnalysis.class)
    private final SimpleGenericClass<SimpleMutableClass,T,T> sgc;
    
    DependentClassWithMutableGenericTypeArgument(SimpleGenericClass<SimpleMutableClass,T,T> sgc) {
        this.sgc = sgc;
    }
}

@DeepImmutableType(value = "class is not extensible",
        analyses = {L0TypeImmutabilityAnalysis.class, L1TypeImmutabilityAnalysis.class})
@DeepImmutableClass(value = "class has no fields and is final",
        analyses = {L0ClassImmutabilityAnalysis.class, L1ClassImmutabilityAnalysis.class})
final class FinalEmptyClass {

}

@DependentImmutableType(value = "class is not extensible", analyses = L1TypeImmutabilityAnalysis.class)
@DependentImmutableClass(value = "class has only dependent immutable fields",
        analyses = L1ClassImmutabilityAnalysis.class)
final class SimpleGenericClass<T1,T2,T3> {

    @MutableField(value="can not handle effective immutability", analyses = L0FieldImmutabilityAnalysis.class)
    @ShallowImmutableField(value = "can not handle generics",
            analyses = {L1FieldImmutabilityAnalysis.class, L2FieldImmutabilityAnalysis.class})
    @DependentImmutableField(value = "immutable reference with generic type",
            analyses = L3FieldImmutabilityAnalysis.class)
    @ImmutableFieldReference(value = "effective immutable field reference",
            analyses = L0FieldReferenceImmutabilityAnalysis.class)
    private T1 t1;

    @MutableField(value="can not handle effective immutability", analyses = L0FieldImmutabilityAnalysis.class)
    @ShallowImmutableField(value = "can not handle generics",
            analyses = {L1FieldImmutabilityAnalysis.class, L2FieldImmutabilityAnalysis.class})
    @DependentImmutableField(value = "immutable reference with generic type",
            analyses = L3FieldImmutabilityAnalysis.class)
    @ImmutableFieldReference(value = "effective immutable field reference",
            analyses = L0FieldReferenceImmutabilityAnalysis.class)
    private T2 t2;

    @MutableField(value="can not handle effective immutability", analyses = L0FieldImmutabilityAnalysis.class)
    @ShallowImmutableField(value = "can not handle generics",
            analyses = {L1FieldImmutabilityAnalysis.class, L2FieldImmutabilityAnalysis.class})
    @DependentImmutableField(value = "immutable reference with generic type",
            analyses = L3FieldImmutabilityAnalysis.class)
    @ImmutableFieldReference(value = "effective immutable field reference",
            analyses = L0FieldReferenceImmutabilityAnalysis.class)
    private T3 t3;

    public SimpleGenericClass(T1 t1, T2 t2, T3 t3){
        this.t1 = t1;
        this.t2 = t2;
        this.t3 = t3;
    }
}

@DependentImmutableType("")
@DependentImmutableClass("")
final class GenericAndDeepImmutableFields<T1, T2> {

    @MutableField(value="can not handle effective immutability", analyses = L0FieldImmutabilityAnalysis.class)
    @ShallowImmutableField(value = "can not handle generics",
            analyses = {L1FieldImmutabilityAnalysis.class, L2FieldImmutabilityAnalysis.class})
    @DependentImmutableField(value = "immutable reference with generic type",
            analyses = L3FieldImmutabilityAnalysis.class)
    @ImmutableFieldReference(value = "effective immutable field reference",
            analyses = L0FieldReferenceImmutabilityAnalysis.class)
    private T1 t1;

    @MutableField(value="can not handle effective immutability", analyses = L0FieldImmutabilityAnalysis.class)
    @ShallowImmutableField(value = "can not handle generics",
            analyses = {L1FieldImmutabilityAnalysis.class, L2FieldImmutabilityAnalysis.class})
    @DependentImmutableField(value = "immutable reference with generic type",
            analyses = L3FieldImmutabilityAnalysis.class)
    @ImmutableFieldReference(value = "effective immutable field reference",
            analyses = L0FieldReferenceImmutabilityAnalysis.class)
    private T2 t2;

    @MutableField(value="can not handle effective immutability", analyses = L0FieldImmutabilityAnalysis.class)
    @DeepImmutableField(value = "immutable reference with deep immutable type",
            analyses = L3FieldImmutabilityAnalysis.class)
    @ShallowImmutableField(value = "can not handle deep immutability",
            analyses = {L1FieldImmutabilityAnalysis.class, L2FieldImmutabilityAnalysis.class} )
    @ImmutableFieldReference(value = "effective immutable field reference",
            analyses = L0FieldReferenceImmutabilityAnalysis.class)
    private FinalEmptyClass fec;

    GenericAndDeepImmutableFields(T1 t1, T2 t2, FinalEmptyClass fec){
        this.t1 = t1;
        this.t2 = t2;
        this.fec = fec;
    }
}

@MutableType(value = "class has a mutable field",
        analyses = {L0TypeImmutabilityAnalysis.class, L1TypeImmutabilityAnalysis.class})
@MutableClass(value = "class has a mutable field",
        analyses = {L0ClassImmutabilityAnalysis.class, L1ClassImmutabilityAnalysis.class})
class ClassWithGenericAndMutableFields<T1, T2> {

    @MutableField("has a mutable field reference")
    @MutableFieldReference("field is public")
    public T1 t1;

    @MutableField(value="can not handle effective immutability", analyses = L0FieldImmutabilityAnalysis.class)
    @ShallowImmutableField(value = "can not handle generics",
            analyses = {L1FieldImmutabilityAnalysis.class, L2FieldImmutabilityAnalysis.class})
    @DependentImmutableField(value = "immutable reference with generic type",
            analyses = L3FieldImmutabilityAnalysis.class)
    @ImmutableFieldReference(value = "effective immutable field reference",
            analyses = L0FieldReferenceImmutabilityAnalysis.class)
    private T2 t2;

    ClassWithGenericAndMutableFields(T1 t1, T2 t2){
        this.t1 = t1;
        this.t2 = t2;
    }
}

@MutableType(value = "class is extensible",
        analyses = {L0TypeImmutabilityAnalysis.class, L1TypeImmutabilityAnalysis.class})
@ShallowImmutableClass(value="upper bound is a shallow immutable field",
        analyses = {L0ClassImmutabilityAnalysis.class, L1ClassImmutabilityAnalysis.class})
class GenericAndShallowImmutableFields<T1, T2> {

    @MutableField(value="can not handle effective immutability", analyses = L0FieldImmutabilityAnalysis.class)
    @ShallowImmutableField(value = "can not handle generics",
            analyses = {L1FieldImmutabilityAnalysis.class, L2FieldImmutabilityAnalysis.class})
    @DependentImmutableField(value = "immutable reference with generic type",
            analyses = L3FieldImmutabilityAnalysis.class)
    @ImmutableFieldReference(value = "effective immutable field reference",
            analyses = L0FieldReferenceImmutabilityAnalysis.class)
    private T1 t1;

    @MutableField(value="can not handle effective immutability", analyses = L0FieldImmutabilityAnalysis.class)
    @ShallowImmutableField(value = "can not handle generics",
            analyses = {L1FieldImmutabilityAnalysis.class, L2FieldImmutabilityAnalysis.class})
    @DependentImmutableField(value = "immutable reference with generic type",
            analyses = L3FieldImmutabilityAnalysis.class)
    @ImmutableFieldReference(value = "effective immutable field reference",
            analyses = L0FieldReferenceImmutabilityAnalysis.class)
    private T2 t2;

    @MutableField(value="can not handle effective immutability", analyses = L0FieldImmutabilityAnalysis.class)
    @ShallowImmutableField(value = "immutable field reference, with mutable type that escapes over the constructor",
    analyses = {L1FieldImmutabilityAnalysis.class, L2FieldImmutabilityAnalysis.class,
    L3FieldImmutabilityAnalysis.class})
    @ImmutableFieldReference(value = "effective immutable field reference",
            analyses = L0FieldReferenceImmutabilityAnalysis.class)
    private SimpleMutableClass  smc;

    GenericAndShallowImmutableFields(T1 t1, T2 t2, SimpleMutableClass smc){
        this.t1 = t1;
        this.t2 = t2;
        this.smc = smc;
    }
}

@MutableType(value = "class is extensible",
        analyses = {L0TypeImmutabilityAnalysis.class, L1TypeImmutabilityAnalysis.class})
@DeepImmutableClass(value = "has only a deep immutable field",
        analyses = {L1ClassImmutabilityAnalysis.class, L0ClassImmutabilityAnalysis.class})
class GenericClassWithDeepImmParam<A extends FinalEmptyClass> {

    @MutableField(value="can not handle effective immutability", analyses = L0FieldImmutabilityAnalysis.class)
    @ShallowImmutableField(value = "can not handle generics",
            analyses = {L1FieldImmutabilityAnalysis.class,
            L2FieldImmutabilityAnalysis.class})
    @DeepImmutableField(value = "immutable field reference with generic type inheriting a final deep immutable type",
    analyses = L3FieldImmutabilityAnalysis.class)
    @ImmutableFieldReference(value="effective immutable field reference",
            analyses = L0FieldReferenceImmutabilityAnalysis.class)
    private A a;

    GenericClassWithDeepImmParam(A a) {
        this.a = a;
    }
}

@MutableType(value = "class is extensible",
        analyses = {L0TypeImmutabilityAnalysis.class, L1TypeImmutabilityAnalysis.class})
@ShallowImmutableClass(value = "has only a shallow immutable instance field",
analyses = {L0ClassImmutabilityAnalysis.class, L1ClassImmutabilityAnalysis.class})
class GenericClassWithExtendingFinalMutableType<A extends FinalMutableClass> {

    @MutableField(value="can not handle effective immutability", analyses = L0FieldImmutabilityAnalysis.class)
    @ShallowImmutableField(value = "immutable reference with a generic types that inherits a mutable type",
            analyses = {L1FieldImmutabilityAnalysis.class, L2FieldImmutabilityAnalysis.class, L3FieldImmutabilityAnalysis.class})
    @ImmutableFieldReference(value = "effective immutable field reference",
            analyses = L0FieldReferenceImmutabilityAnalysis.class)
    private A a;

    GenericClassWithExtendingFinalMutableType(A a){
        this.a = a;
    }
}

@MutableType("class has a mutable field")
@MutableClass("class has a mutable field")
final class FinalMutableClass{

    @MutableField(value="mutable field reference",
            analyses = {L0FieldImmutabilityAnalysis.class, L1FieldImmutabilityAnalysis.class,
            L2FieldImmutabilityAnalysis.class, L3FieldImmutabilityAnalysis.class})
    @MutableFieldReference(value = "public field", analyses = L0FieldReferenceImmutabilityAnalysis.class)
    public int n = 0;
}

@ShallowImmutableType(value = "class is not extensible", analyses = L0TypeImmutabilityAnalysis.class)
@ShallowImmutableClass(value = "can not handle generics", analyses = L0ClassImmutabilityAnalysis.class)
@DeepImmutableType(value = "class is not extensible", analyses = L1TypeImmutabilityAnalysis.class)
@DeepImmutableClass(value = "has only deep immutable fields", analyses = L1ClassImmutabilityAnalysis.class)
final class ClassWithGenericField_deep {

    @MutableField(value="can not handle effective immutability", analyses = L0FieldImmutabilityAnalysis.class)
    @ShallowImmutableField(value = "can not handle generics",
            analyses = {L1FieldImmutabilityAnalysis.class, L2FieldImmutabilityAnalysis.class})
    @DeepImmutableField(value = "deep imm field", analyses = L3FieldImmutabilityAnalysis.class)
    @ImmutableFieldReference(value = "effective immutable reference",
            analyses = L0FieldReferenceImmutabilityAnalysis.class)
    private SimpleGenericClass<FinalEmptyClass,FinalEmptyClass,FinalEmptyClass> gc =
            new SimpleGenericClass<>(new FinalEmptyClass(), new FinalEmptyClass(), new FinalEmptyClass());
}

@MutableType(value = "has only mutable fields",
        analyses = {L0TypeImmutabilityAnalysis.class, L1TypeImmutabilityAnalysis.class})
@MutableClass(value = "has only mutable fields",
        analyses = {L0ClassImmutabilityAnalysis.class, L1ClassImmutabilityAnalysis.class})
final class MutableClassWithGenericField {

    @MutableField(value = "field is public",
            analyses = {L0FieldImmutabilityAnalysis.class, L1FieldImmutabilityAnalysis.class,
            L1FieldImmutabilityAnalysis.class, L2FieldImmutabilityAnalysis.class})
    @MutableFieldReference(value = "field is public", analyses = L0FieldReferenceImmutabilityAnalysis.class)
    public SimpleGenericClass<FinalEmptyClass,FinalEmptyClass,FinalEmptyClass> gc =
            new SimpleGenericClass<>(new FinalEmptyClass(), new FinalEmptyClass(), new FinalEmptyClass());
}

//@DeepImmutableType(value = "class is not extensible", analyses = L1TypeImmutabilityAnalysis.class)
//@DeepImmutableClass(value = "has only deep immutable fields", analyses = L1ClassImmutabilityAnalysis.class)
@ShallowImmutableType(value = "class is not extensible", analyses = {L0TypeImmutabilityAnalysis.class, L1TypeImmutabilityAnalysis.class})
@ShallowImmutableClass(value = "can not handle generics", analyses = {L0ClassImmutabilityAnalysis.class, L1ClassImmutabilityAnalysis.class})
final class ClassWithGenericField_shallow {

    @MutableField(value="can not handle effective immutability", analyses = L0FieldImmutabilityAnalysis.class)
    @ShallowImmutableField(value="the generic type is concretised with mutable types",
            analyses = {L1FieldImmutabilityAnalysis.class,
            L2FieldImmutabilityAnalysis.class, L3FieldImmutabilityAnalysis.class})
    @ImmutableFieldReference(value = "field is effectively final",
            analyses = L0FieldReferenceImmutabilityAnalysis.class)
    private SimpleGenericClass<SimpleMutableClass,FinalEmptyClass,FinalEmptyClass> gc =
            new SimpleGenericClass<SimpleMutableClass,FinalEmptyClass,FinalEmptyClass>
                    (new SimpleMutableClass(), new FinalEmptyClass(), new FinalEmptyClass());
}

@MutableType(value = "class is extensible",
        analyses = {L0TypeImmutabilityAnalysis.class, L1TypeImmutabilityAnalysis.class})
@MutableClass(value = "class extends a mutable class",
        analyses = {L0ClassImmutabilityAnalysis.class, L1ClassImmutabilityAnalysis.class})
class DependentClassInheritingMutableOne<T> extends SimpleMutableClass {

    @DependentImmutableField(value="effective final field with generic type T",
    analyses = L3FieldImmutabilityAnalysis.class)
    @ShallowImmutableField(value= "can not handle generic type",
    analyses = {L0FieldImmutabilityAnalysis.class, L1FieldImmutabilityAnalysis.class,
            L2FieldImmutabilityAnalysis.class})
    @ImmutableFieldReference(value = "effective final field", analyses = L0FieldReferenceImmutabilityAnalysis.class)
    private final T field;

    public DependentClassInheritingMutableOne(T field) {
        this.field = field;
    }
}

@MutableType(value = "class is extensible",
        analyses = {L0TypeImmutabilityAnalysis.class, L1TypeImmutabilityAnalysis.class})
@MutableClass(value = "class has only a mutable field",
        analyses = {L0ClassImmutabilityAnalysis.class, L1ClassImmutabilityAnalysis.class})
class SimpleMutableClass{

    @MutableField(value = "field is public",
            analyses = {L0FieldImmutabilityAnalysis.class, L1FieldImmutabilityAnalysis.class,
                    L2FieldImmutabilityAnalysis.class, L3FieldImmutabilityAnalysis.class})
    @MutableFieldReference(value="field is public", analyses = L0FieldReferenceImmutabilityAnalysis.class)
    public int n = 0;
}
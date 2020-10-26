/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.immutability.classes.multinested_genericClasses;

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

@ShallowImmutableType(value = "class is not extensible", analyses = L0TypeImmutabilityAnalysis.class)
@ShallowImmutableClass(value = "can not handle generics", analyses = L0ClassImmutabilityAnalysis.class)
@DependentImmutableType(value = "class is not extensible", analyses = L1TypeImmutabilityAnalysis.class)
@DependentImmutableClass(value = "has only dependent immutable fields", analyses = L1ClassImmutabilityAnalysis.class)
public final class GenericBaseClass<T1,T2,T3> {

    @MutableField(value="can not handle effective immutability", analyses = L0FieldImmutabilityAnalysis.class)
    @DependentImmutableField(value = "immutable reference with generic type",
            analyses = L3FieldImmutabilityAnalysis.class)
    @ShallowImmutableField(value="can not work with generic type",
            analyses = {L1FieldImmutabilityAnalysis.class, L2FieldImmutabilityAnalysis.class})
    @ImmutableFieldReference(value="effective final field", analyses = L0FieldReferenceImmutabilityAnalysis.class)
    private T1 t1;

    @MutableField(value="can not handle effective immutability", analyses = L0FieldImmutabilityAnalysis.class)
    @DependentImmutableField(value = "immutable reference with generic type",
            analyses = L3FieldImmutabilityAnalysis.class)
    @ShallowImmutableField(value="can not work with generic type",
            analyses = {L1FieldImmutabilityAnalysis.class, L2FieldImmutabilityAnalysis.class})
    @ImmutableFieldReference(value="effective final field", analyses = L0FieldReferenceImmutabilityAnalysis.class)
    private T2 t2;

    @MutableField(value="can not handle effective immutability", analyses = L0FieldImmutabilityAnalysis.class)
    @DependentImmutableField(value = "immutable reference with generic type",
            analyses = L3FieldImmutabilityAnalysis.class)
    @ShallowImmutableField(value="can not work with generic type",
            analyses = {L1FieldImmutabilityAnalysis.class, L2FieldImmutabilityAnalysis.class})
    @ImmutableFieldReference(value="effective final field", analyses = L0FieldReferenceImmutabilityAnalysis.class)
    private T3 t3;

    public GenericBaseClass(T1 t1, T2 t2, T3 t3){
        this.t1 = t1;
        this.t2 = t2;
        this.t3 = t3;
    }

}

@ShallowImmutableType(value = "class is not extensible", analyses = L1TypeImmutabilityAnalysis.class)
@ShallowImmutableClass(value = "class has only shallow immutable fields", analyses = L1ClassImmutabilityAnalysis.class)
final class GenericClassWithMutableFinalParameter<T1 extends ClassWithOneMutableField,T2
        extends FinalClassWithoutFields,T3> {

    @MutableField(value="can not handle effective immutability", analyses = L0FieldImmutabilityAnalysis.class)
    @ShallowImmutableField(value = "can not handle generics; " +
            "immutable reference with generic type inheriting final mutable type",
            analyses = {L1FieldImmutabilityAnalysis.class, L2FieldImmutabilityAnalysis.class,
                    L3FieldImmutabilityAnalysis.class})
    @ImmutableFieldReference(value = "effective immutable field reference",
            analyses = L0FieldReferenceImmutabilityAnalysis.class)
    private T1 t1;

    @MutableField(value="can not handle effective immutability", analyses = L0FieldImmutabilityAnalysis.class)
    @ShallowImmutableField(value = "can not handle generics",
            analyses = {L1FieldImmutabilityAnalysis.class, L2FieldImmutabilityAnalysis.class})
    @DeepImmutableField(value = "immutable reference with generic type inheriting finale deep immutable type",
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

    public GenericClassWithMutableFinalParameter(T1 t1, T2 t2, T3 t3){
        this.t1 = t1;
        this.t2 = t2;
        this.t3 = t3;
    }
}

@DependentImmutableType(value = "class has only dependent immutable fields and is not extensible",
        analyses = L1TypeImmutabilityAnalysis.class)
@DependentImmutableClass(value = "class has only dependent immutable fields",
        analyses = L1ClassImmutabilityAnalysis.class)
final class GenericClassLevel2<T> {

    @MutableField(value="can not handle effective immutability", analyses = L0FieldImmutabilityAnalysis.class)
    @ShallowImmutableField(value = "can not handle generics",
            analyses = {L1FieldImmutabilityAnalysis.class, L2FieldImmutabilityAnalysis.class})
    @DependentImmutableField(value = "immutable reference with generic type",
            analyses = L3FieldImmutabilityAnalysis.class)
    @ImmutableFieldReference(value = "effective immutable field reference",
            analyses = L0FieldReferenceImmutabilityAnalysis.class)
    private T t;

    @DependentImmutableField(value = "one generic parameter left and no mutable types",
            analyses = L3FieldImmutabilityAnalysis.class)
    @ImmutableFieldReference(value = "effective immutable field", analyses = L0FieldReferenceImmutabilityAnalysis.class)
    private GenericBaseClass<FinalClassWithoutFields,FinalClassWithoutFields,T> gc;

    public GenericClassLevel2(T t1, FinalClassWithoutFields fec1, FinalClassWithoutFields fec2){
        this.t = t;
        gc = new GenericBaseClass<FinalClassWithoutFields, FinalClassWithoutFields,T>(fec1, fec2, t1);
    }
}

@DependentImmutableType(value = "class has only dependent immutable fields and is not extensible",
        analyses = L1TypeImmutabilityAnalysis.class)
@DependentImmutableClass(value = "class has only dependent immutable fields",
analyses = L1ClassImmutabilityAnalysis.class)
final class GenericClassLevel3<T> {

    @MutableField(value="can not handle effective immutability", analyses = L0FieldImmutabilityAnalysis.class)
    @ShallowImmutableField(value = "can not handle generics",
            analyses = {L1FieldImmutabilityAnalysis.class, L2FieldImmutabilityAnalysis.class})
    @DependentImmutableField(value = "immutable reference with generic type",
            analyses = L3FieldImmutabilityAnalysis.class)
    @ImmutableFieldReference(value = "effective immutable field reference",
            analyses = L0FieldReferenceImmutabilityAnalysis.class)
    private T t;

    @DependentImmutableField(value = "generic type", analyses = L3FieldImmutabilityAnalysis.class)
    @ImmutableFieldReference(value = "effective immutable field", analyses = L0FieldReferenceImmutabilityAnalysis.class)
    private GenericClassLevel2<T> gc;

    public GenericClassLevel3(T t, FinalClassWithoutFields fec){
        this.t = t;
        gc = new GenericClassLevel2<T>(t, fec, fec);
    }
}

@DeepImmutableType(value = "only deep immutable fields and not extensible class",
        analyses = L1TypeImmutabilityAnalysis.class)
@DeepImmutableClass(value = "only deep immutable fields", analyses = L1ClassImmutabilityAnalysis.class)
final class GenericClassLevel4Deep {

    @ImmutableFieldReference(value = "effective immutable field",
            analyses =  L0FieldReferenceImmutabilityAnalysis.class)
    @DeepImmutableField(value = "only dee immutable types", analyses = L3FieldImmutabilityAnalysis.class)
    private GenericClassLevel3<FinalClassWithoutFields> gc;

    public GenericClassLevel4Deep(FinalClassWithoutFields fec1, FinalClassWithoutFields fec2){
        gc = new GenericClassLevel3<FinalClassWithoutFields>(fec1, fec2);
    }
}

@ShallowImmutableType(value = "has one shallow immutable field and is not extensible",
        analyses = {L0TypeImmutabilityAnalysis.class, L1TypeImmutabilityAnalysis.class})
@ShallowImmutableClass(value = "has one shallow immutable field and is not extensible",
        analyses = {L0ClassImmutabilityAnalysis.class, L1ClassImmutabilityAnalysis.class})
final class GenericClassLevel4Shallow {

    @ImmutableFieldReference(value = "field is effective immutable",
            analyses = L0FieldReferenceImmutabilityAnalysis.class)
    @ShallowImmutableField(value = "has a mutable type",
            analyses = {L1FieldImmutabilityAnalysis.class, L2FieldImmutabilityAnalysis.class,
            L3FieldImmutabilityAnalysis.class})
    private GenericClassLevel3<ClassWithOneMutableField> gc;

    public GenericClassLevel4Shallow(ClassWithOneMutableField tmc1, FinalClassWithoutFields fec){
        gc = new GenericClassLevel3<ClassWithOneMutableField>(tmc1, fec);
    }
}

@DeepImmutableType(value = "has only one deep immutable field and is not extensible",
        analyses = L1TypeImmutabilityAnalysis.class)
@DeepImmutableClass(value = "has only one deep immutable field", analyses = L1ClassImmutabilityAnalysis.class)
@ShallowImmutableType(value = "class is not extensible", analyses = L0TypeImmutabilityAnalysis.class)
@ShallowImmutableClass(value = "has only shallow immutable fields", analyses = L0ClassImmutabilityAnalysis.class)
final class DeepGeneric {

    @MutableField(value="can not handle effective immutability", analyses = L0FieldImmutabilityAnalysis.class)
    @ShallowImmutableField(value = "can not handle generics",
            analyses = {L1FieldImmutabilityAnalysis.class, L2FieldImmutabilityAnalysis.class})
    @DeepImmutableField(value = "only deep immutable types in generics", analyses = L3FieldImmutabilityAnalysis.class)
    @ImmutableFieldReference(value = "effective final field", analyses = L0FieldReferenceImmutabilityAnalysis.class)
    private GenericBaseClass<FinalClassWithoutFields,FinalClassWithoutFields,FinalClassWithoutFields> gc1;

    public DeepGeneric(GenericBaseClass<FinalClassWithoutFields,FinalClassWithoutFields,FinalClassWithoutFields> gc1){
        this.gc1 = gc1;
    }

}

@MutableType(value = "class has mutable fields", analyses = {L0TypeImmutabilityAnalysis.class,
        L1TypeImmutabilityAnalysis.class})
@MutableClass(value = "class has mutable fields", analyses = {L0ClassImmutabilityAnalysis.class,
        L1ClassImmutabilityAnalysis.class})
class One<A,B,C,D> {


    @MutableField(value="can not handle effective immutability", analyses = L0FieldImmutabilityAnalysis.class)
    @ShallowImmutableField(value = "can not handle generics",
            analyses = {L1FieldImmutabilityAnalysis.class, L2FieldImmutabilityAnalysis.class})
    @DependentImmutableField(value = "immutable reference with generic type",
            analyses = L3FieldImmutabilityAnalysis.class)
    @ImmutableFieldReference(value = "effective immutable field reference",
            analyses = L0FieldReferenceImmutabilityAnalysis.class)
    private A a;


    @MutableField(value="can not handle effective immutability", analyses = L0FieldImmutabilityAnalysis.class)
    @ShallowImmutableField(value = "can not handle generics",
            analyses = {L1FieldImmutabilityAnalysis.class, L2FieldImmutabilityAnalysis.class})
    @DependentImmutableField(value = "immutable reference with generic type",
            analyses = L3FieldImmutabilityAnalysis.class)
    @ImmutableFieldReference(value = "effective immutable field reference",
            analyses = L0FieldReferenceImmutabilityAnalysis.class)
    private B b;

    @MutableField(value = "field is public", analyses = {
            L0FieldImmutabilityAnalysis.class, L1FieldImmutabilityAnalysis.class,
            L2FieldImmutabilityAnalysis.class, L3FieldImmutabilityAnalysis.class
    })
    @MutableFieldReference(value = "field is public", analyses = L0FieldReferenceImmutabilityAnalysis.class)
    public ClassWithOneMutableField tmc;

    @ShallowImmutableField("")
    @ImmutableFieldReference("")
    private GenericBaseClass<A,B, ClassWithOneMutableField> gc1;
    public One(A a, B b, ClassWithOneMutableField tmc){
        this.a = a;
        this.b = b;
        this.tmc = tmc;
        this.gc1 = new GenericBaseClass<A,B, ClassWithOneMutableField>(this.a,this.b, this.tmc);
    }
}

@MutableType(value = "class has mutable fields", analyses = {L0TypeImmutabilityAnalysis.class,
L1TypeImmutabilityAnalysis.class})
@MutableClass(value = "class has mutable fields", analyses = {L0ClassImmutabilityAnalysis.class,
L1ClassImmutabilityAnalysis.class})
class OneVirgin<A,B> {

    @MutableField(value="can not handle effective immutability", analyses = L0FieldImmutabilityAnalysis.class)
    @ShallowImmutableField(value = "can not handle generics",
            analyses = {L1FieldImmutabilityAnalysis.class, L2FieldImmutabilityAnalysis.class})
    @DependentImmutableField(value = "immutable reference with generic type",
            analyses = L3FieldImmutabilityAnalysis.class)
    @ImmutableFieldReference(value = "effective immutable field reference",
            analyses = L0FieldReferenceImmutabilityAnalysis.class)
    private A a;

    @MutableField(value="can not handle effective immutability", analyses = L0FieldImmutabilityAnalysis.class)
    @ShallowImmutableField(value = "can not handle generics",
            analyses = {L1FieldImmutabilityAnalysis.class, L2FieldImmutabilityAnalysis.class})
    @DependentImmutableField(value = "immutable reference with generic type",
            analyses = L3FieldImmutabilityAnalysis.class)
    @ImmutableFieldReference(value = "effective immutable field reference",
            analyses = L0FieldReferenceImmutabilityAnalysis.class)
    private B b;

    @MutableField(value = "field is public", analyses = {
            L0FieldImmutabilityAnalysis.class, L1FieldImmutabilityAnalysis.class,
            L2FieldImmutabilityAnalysis.class, L3FieldImmutabilityAnalysis.class
    })
    @MutableFieldReference(value = "field is public", analyses = L0FieldReferenceImmutabilityAnalysis.class)
    public ClassWithOneMutableField tmc;

    GenericBaseClass<A,B,ClassWithOneMutableField> gc1;
    public OneVirgin(A a, B b, ClassWithOneMutableField tmc){
        this.a = a;
        this.b = b;
        this.tmc = tmc;
        this.gc1 = new GenericBaseClass<A,B, ClassWithOneMutableField>(this.a, this.b, this.tmc);
    }
}

@MutableType(value = "class is extensible",
        analyses = {L0TypeImmutabilityAnalysis.class, L1TypeImmutabilityAnalysis.class})
@ShallowImmutableClass(value="has only one shallow immutable field",
        analyses = {L0ClassImmutabilityAnalysis.class, L1ClassImmutabilityAnalysis.class})
class Two<A,B> {

    @ShallowImmutableField(value = "There is a mutable type",
            analyses = {L1FieldImmutabilityAnalysis.class, L2FieldImmutabilityAnalysis.class,
            L3FieldImmutabilityAnalysis.class})
    @ImmutableFieldReference(value = "field is effective immutabe",
            analyses = L0FieldReferenceImmutabilityAnalysis.class)
    private GenericBaseClass<GenericBaseClass<A, A, A>, B, ClassWithOneMutableField> gc1;

    public Two(A a, B b, ClassWithOneMutableField tmc, GenericBaseClass gc1) {
        this.gc1 = new GenericBaseClass<GenericBaseClass<A, A, A>, B, ClassWithOneMutableField>(gc1, b, tmc);
    }
}

@DependentImmutableType(value = "class is not extensible", analyses = L1TypeImmutabilityAnalysis.class)
@DependentImmutableClass(value = "class has only dependent immutable fields",
        analyses = L1ClassImmutabilityAnalysis.class)
class TwoVirgin<A, B, C> {

    @DependentImmutableField(value="field has generic parameter", analyses = L3FieldImmutabilityAnalysis.class)
    @ImmutableFieldReference(value = "field is effective immutable",
            analyses = L0FieldReferenceImmutabilityAnalysis.class)
    private GenericBaseClass<GenericBaseClass<GenericBaseClass, A, A>, B, C> gc1;

    public TwoVirgin(A a, B b, C c, GenericBaseClass<GenericBaseClass, A, A> gc1) {
        this.gc1 = new GenericBaseClass<GenericBaseClass<GenericBaseClass, A, A>, B, C>(gc1,b,c);
    }
}

@DependentImmutableType(value = "class is not extensible", analyses = L1TypeImmutabilityAnalysis.class)
@DependentImmutableClass(value = "class has only dependent immutable fields",
        analyses = L1ClassImmutabilityAnalysis.class)
final class TestTest<T1,T2 extends FinalClassWithoutFields,T3> {


    private GenericBaseClass<T1,T1,T1> t1;

    @MutableField(value="can not handle effective immutability", analyses = L0FieldImmutabilityAnalysis.class)
    @ShallowImmutableField(value = "can not handle generics",
            analyses = {L1FieldImmutabilityAnalysis.class, L2FieldImmutabilityAnalysis.class})
    @DeepImmutableField(value = "immutable reference with generic type inheriting final deep immutable",
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


    public TestTest(GenericBaseClass<T1,T1,T1> t1, T2 t2, T3 t3){
        this.t1 = t1;
        this.t2 = t2;
        this.t3 = t3;
    }
}

@MutableType(value = "class is extensible",
        analyses = {L0TypeImmutabilityAnalysis.class, L1TypeImmutabilityAnalysis.class})
@MutableClass(value = "class has only a mutable field",
        analyses = {L0ClassImmutabilityAnalysis.class, L1ClassImmutabilityAnalysis.class})
class ClassWithOneMutableField {
    @MutableField(value = "field is public",
            analyses = {L0FieldImmutabilityAnalysis.class, L1FieldImmutabilityAnalysis.class,
                    L2FieldImmutabilityAnalysis.class, L3FieldImmutabilityAnalysis.class})
    @MutableFieldReference(value="field is public", analyses = L0FieldReferenceImmutabilityAnalysis.class)
    public int n = 0;
}

@DeepImmutableType(value = "class is not extensible",
        analyses = {L0TypeImmutabilityAnalysis.class, L1TypeImmutabilityAnalysis.class})
@DeepImmutableClass(value = "class has no fields and is final",
        analyses = {L0ClassImmutabilityAnalysis.class, L1ClassImmutabilityAnalysis.class})
final class FinalClassWithoutFields {

}


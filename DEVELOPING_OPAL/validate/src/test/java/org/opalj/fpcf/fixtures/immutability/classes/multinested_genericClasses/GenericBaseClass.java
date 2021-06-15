/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.immutability.classes.multinested_genericClasses;

import org.opalj.br.fpcf.analyses.L0ClassImmutabilityAnalysis;
import org.opalj.br.fpcf.analyses.L0FieldAssignabilityAnalysis;
import org.opalj.br.fpcf.analyses.L0TypeImmutabilityAnalysis;
import org.opalj.fpcf.properties.immutability.classes.TransitivelyImmutableClass;
import org.opalj.fpcf.properties.immutability.classes.DependentlyImmutableClass;
import org.opalj.fpcf.properties.immutability.classes.MutableClass;
import org.opalj.fpcf.properties.immutability.classes.NonTransitivelyImmutableClass;
import org.opalj.fpcf.properties.immutability.fields.TransitivelyImmutableField;
import org.opalj.fpcf.properties.immutability.fields.DependentlyImmutableField;
import org.opalj.fpcf.properties.immutability.fields.MutableField;
import org.opalj.fpcf.properties.immutability.fields.NonTransitivelyImmutableField;
import org.opalj.fpcf.properties.immutability.field_assignability.EffectivelyNonAssignableField;
import org.opalj.fpcf.properties.immutability.field_assignability.AssignableField;
import org.opalj.fpcf.properties.immutability.types.TransitivelyImmutableType;
import org.opalj.fpcf.properties.immutability.types.DependentImmutableType;
import org.opalj.fpcf.properties.immutability.types.MutableType;
import org.opalj.fpcf.properties.immutability.types.NonTransitivelyImmutableType;
import org.opalj.tac.fpcf.analyses.L1FieldAssignabilityAnalysis;
import org.opalj.tac.fpcf.analyses.L2FieldAssignabilityAnalysis;
import org.opalj.tac.fpcf.analyses.immutability.L1ClassImmutabilityAnalysis;
import org.opalj.tac.fpcf.analyses.immutability.L1TypeImmutabilityAnalysis;
import org.opalj.tac.fpcf.analyses.immutability.L0FieldImmutabilityAnalysis;
import org.opalj.tac.fpcf.analyses.immutability.fieldassignability.L3FieldAssignabilityAnalysis;

@NonTransitivelyImmutableType(value = "class is not extensible", analyses = L0TypeImmutabilityAnalysis.class)
@NonTransitivelyImmutableClass(value = "can not handle generics", analyses = L0ClassImmutabilityAnalysis.class)
@DependentImmutableType(value = "class is not extensible", analyses = L1TypeImmutabilityAnalysis.class)
@DependentlyImmutableClass(value = "has only dependent immutable fields", analyses = L1ClassImmutabilityAnalysis.class)
public final class GenericBaseClass<T1,T2,T3> {

    @MutableField(value="can not handle effective immutability", analyses = L0FieldAssignabilityAnalysis.class)
    @DependentlyImmutableField(value = "immutable reference with generic type",
            analyses = L0FieldImmutabilityAnalysis.class)
    @NonTransitivelyImmutableField(value="can not work with generic type",
            analyses = {L1FieldAssignabilityAnalysis.class, L2FieldAssignabilityAnalysis.class})
    @EffectivelyNonAssignableField(value="effective final field", analyses = L3FieldAssignabilityAnalysis.class)
    private T1 t1;

    @MutableField(value="can not handle effective immutability", analyses = L0FieldAssignabilityAnalysis.class)
    @DependentlyImmutableField(value = "immutable reference with generic type",
            analyses = L0FieldImmutabilityAnalysis.class)
    @NonTransitivelyImmutableField(value="can not work with generic type",
            analyses = {L1FieldAssignabilityAnalysis.class, L2FieldAssignabilityAnalysis.class})
    @EffectivelyNonAssignableField(value="effective final field", analyses = L3FieldAssignabilityAnalysis.class)
    private T2 t2;

    @MutableField(value="can not handle effective immutability", analyses = L0FieldAssignabilityAnalysis.class)
    @DependentlyImmutableField(value = "immutable reference with generic type",
            analyses = L0FieldImmutabilityAnalysis.class)
    @NonTransitivelyImmutableField(value="can not work with generic type",
            analyses = {L1FieldAssignabilityAnalysis.class, L2FieldAssignabilityAnalysis.class})
    @EffectivelyNonAssignableField(value="effective final field", analyses = L3FieldAssignabilityAnalysis.class)
    private T3 t3;

    public GenericBaseClass(T1 t1, T2 t2, T3 t3){
        this.t1 = t1;
        this.t2 = t2;
        this.t3 = t3;
    }

}

@NonTransitivelyImmutableType(value = "class is not extensible", analyses = L1TypeImmutabilityAnalysis.class)
@NonTransitivelyImmutableClass(value = "class has only shallow immutable fields", analyses = L1ClassImmutabilityAnalysis.class)
final class GenericClassWithMutableFinalParameter<T1 extends ClassWithOneMutableField,T2
        extends FinalClassWithoutFields,T3> {

    @MutableField(value="can not handle effective immutability", analyses = L0FieldAssignabilityAnalysis.class)
    @NonTransitivelyImmutableField(value = "can not handle generics; " +
            "immutable reference with generic type inheriting final mutable type",
            analyses = {L1FieldAssignabilityAnalysis.class, L2FieldAssignabilityAnalysis.class,
                    L0FieldImmutabilityAnalysis.class})
    @EffectivelyNonAssignableField(value = "effective immutable field reference",
            analyses = L3FieldAssignabilityAnalysis.class)
    private T1 t1;

    @MutableField(value="can not handle effective immutability", analyses = L0FieldAssignabilityAnalysis.class)
    @NonTransitivelyImmutableField(value = "can not handle generics",
            analyses = {L1FieldAssignabilityAnalysis.class, L2FieldAssignabilityAnalysis.class})
    @TransitivelyImmutableField(value = "immutable reference with generic type inheriting finale deep immutable type",
            analyses = L0FieldImmutabilityAnalysis.class)
    @EffectivelyNonAssignableField(value = "effective immutable field reference",
            analyses = L3FieldAssignabilityAnalysis.class)
    private T2 t2;

    @MutableField(value="can not handle effective immutability", analyses = L0FieldAssignabilityAnalysis.class)
    @NonTransitivelyImmutableField(value = "can not handle generics",
            analyses = {L1FieldAssignabilityAnalysis.class, L2FieldAssignabilityAnalysis.class})
    @DependentlyImmutableField(value = "immutable reference with generic type",
            analyses = L0FieldImmutabilityAnalysis.class)
    @EffectivelyNonAssignableField(value = "effective immutable field reference",
            analyses = L3FieldAssignabilityAnalysis.class)
    private T3 t3;

    public GenericClassWithMutableFinalParameter(T1 t1, T2 t2, T3 t3){
        this.t1 = t1;
        this.t2 = t2;
        this.t3 = t3;
    }
}

@DependentImmutableType(value = "class has only dependent immutable fields and is not extensible",
        analyses = L1TypeImmutabilityAnalysis.class)
@DependentlyImmutableClass(value = "class has only dependent immutable fields",
        analyses = L1ClassImmutabilityAnalysis.class)
final class GenericClassLevel2<T> {

    @MutableField(value="can not handle effective immutability", analyses = L0FieldAssignabilityAnalysis.class)
    @NonTransitivelyImmutableField(value = "can not handle generics",
            analyses = {L1FieldAssignabilityAnalysis.class, L2FieldAssignabilityAnalysis.class})
    @DependentlyImmutableField(value = "immutable reference with generic type",
            analyses = L0FieldImmutabilityAnalysis.class)
    @EffectivelyNonAssignableField(value = "effective immutable field reference",
            analyses = L3FieldAssignabilityAnalysis.class)
    private T t;

    @DependentlyImmutableField(value = "one generic parameter left and no mutable types",
            analyses = L0FieldImmutabilityAnalysis.class)
    @EffectivelyNonAssignableField(value = "effective immutable field", analyses = L3FieldAssignabilityAnalysis.class)
    private GenericBaseClass<FinalClassWithoutFields,FinalClassWithoutFields,T> gc;

    public GenericClassLevel2(T t1, FinalClassWithoutFields fec1, FinalClassWithoutFields fec2){
        this.t = t;
        gc = new GenericBaseClass<FinalClassWithoutFields, FinalClassWithoutFields,T>(fec1, fec2, t1);
    }
}

@DependentImmutableType(value = "class has only dependent immutable fields and is not extensible",
        analyses = L1TypeImmutabilityAnalysis.class)
@DependentlyImmutableClass(value = "class has only dependent immutable fields",
analyses = L1ClassImmutabilityAnalysis.class)
final class GenericClassLevel3<T> {

    @MutableField(value="can not handle effective immutability", analyses = L0FieldAssignabilityAnalysis.class)
    @NonTransitivelyImmutableField(value = "can not handle generics",
            analyses = {L1FieldAssignabilityAnalysis.class, L2FieldAssignabilityAnalysis.class})
    @DependentlyImmutableField(value = "immutable reference with generic type",
            analyses = L0FieldImmutabilityAnalysis.class)
    @EffectivelyNonAssignableField(value = "effective immutable field reference",
            analyses = L3FieldAssignabilityAnalysis.class)
    private T t;

    @DependentlyImmutableField(value = "generic type", analyses = L0FieldImmutabilityAnalysis.class)
    @EffectivelyNonAssignableField(value = "effective immutable field", analyses = L3FieldAssignabilityAnalysis.class)
    private GenericClassLevel2<T> gc;

    public GenericClassLevel3(T t, FinalClassWithoutFields fec){
        this.t = t;
        gc = new GenericClassLevel2<T>(t, fec, fec);
    }
}

@TransitivelyImmutableType(value = "only deep immutable fields and not extensible class",
        analyses = L1TypeImmutabilityAnalysis.class)
@TransitivelyImmutableClass(value = "only deep immutable fields", analyses = L1ClassImmutabilityAnalysis.class)
final class GenericClassLevel4Deep {

    @EffectivelyNonAssignableField(value = "effective immutable field",
            analyses =  L3FieldAssignabilityAnalysis.class)
    @TransitivelyImmutableField(value = "only dee immutable types", analyses = L0FieldImmutabilityAnalysis.class)
    private GenericClassLevel3<FinalClassWithoutFields> gc;

    public GenericClassLevel4Deep(FinalClassWithoutFields fec1, FinalClassWithoutFields fec2){
        gc = new GenericClassLevel3<FinalClassWithoutFields>(fec1, fec2);
    }
}

@NonTransitivelyImmutableType(value = "has one shallow immutable field and is not extensible",
        analyses = {L0TypeImmutabilityAnalysis.class, L1TypeImmutabilityAnalysis.class})
@NonTransitivelyImmutableClass(value = "has one shallow immutable field and is not extensible",
        analyses = {L0ClassImmutabilityAnalysis.class, L1ClassImmutabilityAnalysis.class})
final class GenericClassLevel4Shallow {

    @EffectivelyNonAssignableField(value = "field is effective immutable",
            analyses = L3FieldAssignabilityAnalysis.class)
    @NonTransitivelyImmutableField(value = "has a mutable type",
            analyses = {L1FieldAssignabilityAnalysis.class, L2FieldAssignabilityAnalysis.class,
            L0FieldImmutabilityAnalysis.class})
    private GenericClassLevel3<ClassWithOneMutableField> gc;

    public GenericClassLevel4Shallow(ClassWithOneMutableField tmc1, FinalClassWithoutFields fec){
        gc = new GenericClassLevel3<ClassWithOneMutableField>(tmc1, fec);
    }
}

@TransitivelyImmutableType(value = "has only one deep immutable field and is not extensible",
        analyses = L1TypeImmutabilityAnalysis.class)
@TransitivelyImmutableClass(value = "has only one deep immutable field", analyses = L1ClassImmutabilityAnalysis.class)
@NonTransitivelyImmutableType(value = "class is not extensible", analyses = L0TypeImmutabilityAnalysis.class)
@NonTransitivelyImmutableClass(value = "has only shallow immutable fields", analyses = L0ClassImmutabilityAnalysis.class)
final class DeepGeneric {

    @MutableField(value="can not handle effective immutability", analyses = L0FieldAssignabilityAnalysis.class)
    @NonTransitivelyImmutableField(value = "can not handle generics",
            analyses = {L1FieldAssignabilityAnalysis.class, L2FieldAssignabilityAnalysis.class})
    @TransitivelyImmutableField(value = "only deep immutable types in generics", analyses = L0FieldImmutabilityAnalysis.class)
    @EffectivelyNonAssignableField(value = "effective final field", analyses = L3FieldAssignabilityAnalysis.class)
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


    @MutableField(value="can not handle effective immutability", analyses = L0FieldAssignabilityAnalysis.class)
    @NonTransitivelyImmutableField(value = "can not handle generics",
            analyses = {L1FieldAssignabilityAnalysis.class, L2FieldAssignabilityAnalysis.class})
    @DependentlyImmutableField(value = "immutable reference with generic type",
            analyses = L0FieldImmutabilityAnalysis.class)
    @EffectivelyNonAssignableField(value = "effective immutable field reference",
            analyses = L3FieldAssignabilityAnalysis.class)
    private A a;


    @MutableField(value="can not handle effective immutability", analyses = L0FieldAssignabilityAnalysis.class)
    @NonTransitivelyImmutableField(value = "can not handle generics",
            analyses = {L1FieldAssignabilityAnalysis.class, L2FieldAssignabilityAnalysis.class})
    @DependentlyImmutableField(value = "immutable reference with generic type",
            analyses = L0FieldImmutabilityAnalysis.class)
    @EffectivelyNonAssignableField(value = "effective immutable field reference",
            analyses = L3FieldAssignabilityAnalysis.class)
    private B b;

    @MutableField(value = "field is public", analyses = {
            L0FieldAssignabilityAnalysis.class, L1FieldAssignabilityAnalysis.class,
            L2FieldAssignabilityAnalysis.class, L0FieldImmutabilityAnalysis.class
    })
    @AssignableField(value = "field is public", analyses = L3FieldAssignabilityAnalysis.class)
    public ClassWithOneMutableField tmc;

    @NonTransitivelyImmutableField("")
    @EffectivelyNonAssignableField("")
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

    @MutableField(value="can not handle effective immutability", analyses = L0FieldAssignabilityAnalysis.class)
    @NonTransitivelyImmutableField(value = "can not handle generics",
            analyses = {L1FieldAssignabilityAnalysis.class, L2FieldAssignabilityAnalysis.class})
    @DependentlyImmutableField(value = "immutable reference with generic type",
            analyses = L0FieldImmutabilityAnalysis.class)
    @EffectivelyNonAssignableField(value = "effective immutable field reference",
            analyses = L3FieldAssignabilityAnalysis.class)
    private A a;

    @MutableField(value="can not handle effective immutability", analyses = L0FieldAssignabilityAnalysis.class)
    @NonTransitivelyImmutableField(value = "can not handle generics",
            analyses = {L1FieldAssignabilityAnalysis.class, L2FieldAssignabilityAnalysis.class})
    @DependentlyImmutableField(value = "immutable reference with generic type",
            analyses = L0FieldImmutabilityAnalysis.class)
    @EffectivelyNonAssignableField(value = "effective immutable field reference",
            analyses = L3FieldAssignabilityAnalysis.class)
    private B b;

    @MutableField(value = "field is public", analyses = {
            L0FieldAssignabilityAnalysis.class, L1FieldAssignabilityAnalysis.class,
            L2FieldAssignabilityAnalysis.class, L0FieldImmutabilityAnalysis.class
    })
    @AssignableField(value = "field is public", analyses = L3FieldAssignabilityAnalysis.class)
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
@NonTransitivelyImmutableClass(value="has only one shallow immutable field",
        analyses = {L0ClassImmutabilityAnalysis.class, L1ClassImmutabilityAnalysis.class})
class Two<A,B> {

    @NonTransitivelyImmutableField(value = "There is a mutable type",
            analyses = {L1FieldAssignabilityAnalysis.class, L2FieldAssignabilityAnalysis.class,
            L0FieldImmutabilityAnalysis.class})
    @EffectivelyNonAssignableField(value = "field is effective immutabe",
            analyses = L3FieldAssignabilityAnalysis.class)
    private GenericBaseClass<GenericBaseClass<A, A, A>, B, ClassWithOneMutableField> gc1;

    public Two(A a, B b, ClassWithOneMutableField tmc, GenericBaseClass gc1) {
        this.gc1 = new GenericBaseClass<GenericBaseClass<A, A, A>, B, ClassWithOneMutableField>(gc1, b, tmc);
    }
}

@MutableType(value = "class is not extensible", analyses = L1TypeImmutabilityAnalysis.class)
@NonTransitivelyImmutableClass(value = "class has only dependent immutable fields",
        analyses = L1ClassImmutabilityAnalysis.class)
class TwoVirgin<A, B, C> {

    @NonTransitivelyImmutableField(value="field has generic parameter", analyses = L0FieldImmutabilityAnalysis.class)
    @EffectivelyNonAssignableField(value = "field is effective immutable",
            analyses = L3FieldAssignabilityAnalysis.class)
    private GenericBaseClass<GenericBaseClass<GenericBaseClass, A, A>, B, C> gc1;

    public TwoVirgin(A a, B b, C c, GenericBaseClass<GenericBaseClass, A, A> gc1) {
        this.gc1 = new GenericBaseClass<GenericBaseClass<GenericBaseClass, A, A>, B, C>(gc1,b,c);
    }
}

@DependentImmutableType(value = "class is not extensible", analyses = L1TypeImmutabilityAnalysis.class)
@DependentlyImmutableClass(value = "class has only dependent immutable fields",
        analyses = L1ClassImmutabilityAnalysis.class)
final class TestTest<T1,T2 extends FinalClassWithoutFields,T3> {


    private GenericBaseClass<T1,T1,T1> t1;

    @MutableField(value="can not handle effective immutability", analyses = L0FieldAssignabilityAnalysis.class)
    @NonTransitivelyImmutableField(value = "can not handle generics",
            analyses = {L1FieldAssignabilityAnalysis.class, L2FieldAssignabilityAnalysis.class})
    @TransitivelyImmutableField(value = "immutable reference with generic type inheriting final deep immutable",
            analyses = L0FieldImmutabilityAnalysis.class)
    @EffectivelyNonAssignableField(value = "effective immutable field reference",
            analyses = L3FieldAssignabilityAnalysis.class)
    private T2 t2;

    @MutableField(value="can not handle effective immutability", analyses = L0FieldAssignabilityAnalysis.class)
    @NonTransitivelyImmutableField(value = "can not handle generics",
            analyses = {L1FieldAssignabilityAnalysis.class, L2FieldAssignabilityAnalysis.class})
    @DependentlyImmutableField(value = "immutable reference with generic type",
            analyses = L0FieldImmutabilityAnalysis.class)
    @EffectivelyNonAssignableField(value = "effective immutable field reference",
            analyses = L3FieldAssignabilityAnalysis.class)
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
            analyses = {L0FieldAssignabilityAnalysis.class, L1FieldAssignabilityAnalysis.class,
                    L2FieldAssignabilityAnalysis.class, L0FieldImmutabilityAnalysis.class})
    @AssignableField(value="field is public", analyses = L3FieldAssignabilityAnalysis.class)
    public int n = 0;
}

@TransitivelyImmutableType(value = "class is not extensible",
        analyses = {L0TypeImmutabilityAnalysis.class, L1TypeImmutabilityAnalysis.class})
@TransitivelyImmutableClass(value = "class has no fields and is final",
        analyses = {L0ClassImmutabilityAnalysis.class, L1ClassImmutabilityAnalysis.class})
final class FinalClassWithoutFields {

}


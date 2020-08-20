package org.opalj.fpcf.fixtures.immutability.classes.multinested_genericClasses;

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
import org.opalj.fpcf.properties.type_immutability.ShallowImmutableTypeAnnotation;

@DependentImmutableTypeAnnotation("")
@DependentImmutableClassAnnotation("")
public final class Generic_class1<T1,T2,T3,T4,T5> {

    @DependentImmutableFieldAnnotation(value = "T1", genericString = "T1")
    private T1 t1;

    @DependentImmutableFieldAnnotation(value = "T2", genericString = "T2")
    private T2 t2;

    @DependentImmutableFieldAnnotation(value = "T3", genericString = "T3")
    private T3 t3;

    @DependentImmutableFieldAnnotation(value = "T4", genericString = "T4")
    private T4 t4;

    @DependentImmutableFieldAnnotation(value = "T5", genericString = "T5")
    private T5 t5;

    public Generic_class1(T1 t1, T2 t2, T3 t3, T4 t4, T5 t5){
        this.t1 = t1;
        this.t2 = t2;
        this.t3 = t3;
        this.t4 = t4;
        this.t5 = t5;
    }

}

@ShallowImmutableTypeAnnotation("")
@ShallowImmutableClassAnnotation("")
final class Generic_class1_extString<T1 extends ClassWithOneMutableField,T2 extends FinalClassWithoutFields,T3,T4,T5> {

    @ShallowImmutableFieldAnnotation("")
    private T1 t1;

    @DeepImmutableFieldAnnotation(value = "T2")
    private T2 t2;

    @DependentImmutableFieldAnnotation(value = "T3", genericString = "T3")
    private T3 t3;

    @DependentImmutableFieldAnnotation(value = "T4", genericString = "T4")
    private T4 t4;

    @DependentImmutableFieldAnnotation(value = "T5", genericString = "T5")
    private T5 t5;

    public Generic_class1_extString(T1 t1, T2 t2, T3 t3, T4 t4, T5 t5){
        this.t1 = t1;
        this.t2 = t2;
        this.t3 = t3;
        this.t4 = t4;
        this.t5 = t5;
    }

}

@DependentImmutableTypeAnnotation("")
@DependentImmutableClassAnnotation("")
final class Generic_class2<T1,T2,T3> {

    @ImmutableReferenceAnnotation("")
    @DependentImmutableFieldAnnotation(value="T1", genericString = "T1")
    private T1 t1;

    @ImmutableReferenceAnnotation("")
    @DependentImmutableFieldAnnotation(value="T2", genericString = "T2")
    private T2 t2;

    @ImmutableReferenceAnnotation("")
    @DependentImmutableFieldAnnotation(value="T3", genericString = "T3")
    private T3 t3;

    @ImmutableReferenceAnnotation("")
    @DependentImmutableFieldAnnotation(value = "", genericString = "")
    private Generic_class1<FinalClassWithoutFields,FinalClassWithoutFields,T1,T2,T3> gc;


    public Generic_class2(T1 t1, T2 t2, T3 t3, FinalClassWithoutFields fec1, FinalClassWithoutFields fec2){
        this.t1 = t1;
        this.t2 = t2;
        this.t3 = t3;
        gc = new Generic_class1<FinalClassWithoutFields, FinalClassWithoutFields,T1,T2,T3>(fec1, fec2, t1,t2,t3);
    }

}

@DependentImmutableTypeAnnotation("")
@DependentImmutableClassAnnotation("")
final class Generic_class3<T1> {

    @ImmutableReferenceAnnotation("")
    @DependentImmutableFieldAnnotation(value = "T1", genericString = "T1")
    private T1 t1;

    @ImmutableReferenceAnnotation("")
    @DependentImmutableFieldAnnotation(value = "", genericString = "")
    private Generic_class2<T1, FinalClassWithoutFields, FinalClassWithoutFields> gc;

    public Generic_class3(T1 t1, FinalClassWithoutFields fec1, FinalClassWithoutFields fec2, FinalClassWithoutFields fec3, FinalClassWithoutFields fec4){
        this.t1 = t1;
        gc = new Generic_class2<T1, FinalClassWithoutFields, FinalClassWithoutFields>(t1, fec1, fec2, fec3, fec4);
    }
}

@DeepImmutableTypeAnnotation("")
@DeepImmutableClassAnnotation("")
final class Generic_class4_deep {

    @ImmutableReferenceAnnotation("")
    @DeepImmutableFieldAnnotation("")
    private Generic_class3<FinalClassWithoutFields> gc;

    public Generic_class4_deep(FinalClassWithoutFields fec1, FinalClassWithoutFields fec2, FinalClassWithoutFields fec3, FinalClassWithoutFields fec4, FinalClassWithoutFields fec5){
        gc = new Generic_class3<FinalClassWithoutFields>(fec1, fec2, fec3, fec4, fec5);
    }
}

@ShallowImmutableTypeAnnotation("")
@ShallowImmutableClassAnnotation("")
final class Generic_class4_shallow {

    @ImmutableReferenceAnnotation("")
    @ShallowImmutableFieldAnnotation("")
    private Generic_class3<ClassWithOneMutableField> gc;

    public Generic_class4_shallow(ClassWithOneMutableField tmc1, FinalClassWithoutFields fec2, FinalClassWithoutFields fec3, FinalClassWithoutFields fec4, FinalClassWithoutFields fec5){
        gc = new Generic_class3<ClassWithOneMutableField>(tmc1, fec2, fec3, fec4, fec5);
    }
}

@DeepImmutableTypeAnnotation("")
@DeepImmutableClassAnnotation("")
final class DeepGeneric {
    @DeepImmutableFieldAnnotation("")
    @ImmutableReferenceAnnotation("")
    private Generic_class1<FinalClassWithoutFields,FinalClassWithoutFields,FinalClassWithoutFields,FinalClassWithoutFields,FinalClassWithoutFields> gc1;

    public DeepGeneric(Generic_class1<FinalClassWithoutFields,FinalClassWithoutFields,FinalClassWithoutFields,FinalClassWithoutFields,FinalClassWithoutFields> gc1){
        this.gc1 = gc1;
    }

}

@MutableTypeAnnotation("")
@MutableClassAnnotation("")
class One<A,B,C,D> {
    @DependentImmutableFieldAnnotation(value="",genericString = "A")
    @ImmutableReferenceAnnotation("")
    private A a;
    @DependentImmutableFieldAnnotation(value="",genericString = "B")
    @ImmutableReferenceAnnotation("")
    private B b;
    @DependentImmutableFieldAnnotation(value="",genericString = "C")
    @ImmutableReferenceAnnotation("")
    private C c;
    @DependentImmutableFieldAnnotation(value="",genericString = "D")
    @ImmutableReferenceAnnotation("")
    private D d;
    @MutableFieldAnnotation("")
    @MutableReferenceAnnotation("")
    public ClassWithOneMutableField tmc;
    @ShallowImmutableFieldAnnotation("")
    @ImmutableReferenceAnnotation("")
    private Generic_class1<A,B,C, D, ClassWithOneMutableField> gc1;
    public One(A a, B b, C c, D  d, ClassWithOneMutableField tmc){
        this.a = a;
        this.b = b;
        this.c = c;
        this.d = d;
        this.tmc = tmc;
        this.gc1 = new Generic_class1<A,B,C, D, ClassWithOneMutableField>(this.a,this.b,this.c,this.d, this.tmc);
    }
}

@MutableTypeAnnotation("")
@MutableClassAnnotation("")
class OneVirgin<A,B,C,D, E> {
    @DependentImmutableFieldAnnotation(value="",genericString = "A")
    @ImmutableReferenceAnnotation("")
    private A a;
    @DependentImmutableFieldAnnotation(value="",genericString = "B")
    @ImmutableReferenceAnnotation("")
    private B b;
    @DependentImmutableFieldAnnotation(value="",genericString = "C")
    @ImmutableReferenceAnnotation("")
    private C c;
    @DependentImmutableFieldAnnotation(value="",genericString = "D")
    @ImmutableReferenceAnnotation("")
    private D d;

    @MutableFieldAnnotation("")
    @MutableReferenceAnnotation("")
    public ClassWithOneMutableField tmc;

    Generic_class1<A,B,C, D, ClassWithOneMutableField> gc1;
    public OneVirgin(A a, B b, C c, D  d, ClassWithOneMutableField e){
        this.a = a;
        this.b = b;
        this.c = c;
        this.d = d;
        this.tmc = tmc;
        this.gc1 = new Generic_class1<A,B,C, D, ClassWithOneMutableField>(this.a, this.b, this.c, this.d, this.tmc);
    }


}

@MutableTypeAnnotation("")
@ShallowImmutableClassAnnotation("")
class Two<A,B> {

    @ShallowImmutableFieldAnnotation("")
    @ImmutableReferenceAnnotation("")
    private Generic_class1<Generic_class1<A, A, A, A, A>, B, B, B, ClassWithOneMutableField> gc1;

    public Two(A a, B b, ClassWithOneMutableField tmc, Generic_class1 gc1) {
        this.gc1 = new Generic_class1<Generic_class1<A, A, A, A, A>, B, B, B, ClassWithOneMutableField>(gc1,b,b,b,tmc);
    }
}

class TwoVirgin<A,B, C, D, E> {
    @DependentImmutableFieldAnnotation(value="",genericString = "")
    @ImmutableReferenceAnnotation("")
    private Generic_class1<Generic_class1<Generic_class1, A, A, A, A>, B, C, C, C> gc1;

    public TwoVirgin(A a, B b, C c, Generic_class1<Generic_class1, A, A, A, A> gc1) {
        this.gc1 = new Generic_class1<Generic_class1<Generic_class1, A, A, A, A>, B, C, C, C>(gc1,b,c,c,c);
    }
}

@DependentImmutableTypeAnnotation("")
@DependentImmutableClassAnnotation("")
final class TestTest<T1,T2 extends FinalClassWithoutFields,T3,T4,T5> {

    @DependentImmutableFieldAnnotation(value = "T1", genericString = "T1")
    private Generic_class1<T1,T1,T1,T1,T1> t1;

    @DeepImmutableFieldAnnotation(value = "T2")
    private T2 t2;

    @DependentImmutableFieldAnnotation(value = "T3", genericString = "T3")
    private T3 t3;

    @DependentImmutableFieldAnnotation(value = "T4", genericString = "T4")
    private T4 t4;

    @DependentImmutableFieldAnnotation(value = "T5", genericString = "T5")
    private T5 t5;

    public TestTest(Generic_class1<T1,T1,T1,T1,T1> t1, T2 t2, T3 t3, T4 t4, T5 t5){
        this.t1 = t1;
        this.t2 = t2;
        this.t3 = t3;
        this.t4 = t4;
        this.t5 = t5;
    }

}

class ClassWithOneMutableField { public int n = 0;}
final class FinalClassWithoutFields {}


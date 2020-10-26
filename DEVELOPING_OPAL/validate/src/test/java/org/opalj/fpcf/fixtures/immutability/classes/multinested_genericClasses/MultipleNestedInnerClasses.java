package org.opalj.fpcf.fixtures.immutability.classes.multinested_genericClasses;

import org.opalj.fpcf.properties.immutability.fields.DependentImmutableField;
import org.opalj.tac.fpcf.analyses.immutability.L3FieldImmutabilityAnalysis;

public class MultipleNestedInnerClasses {
}
class LevelZero<T>{
    @DependentImmutableField(value = "", analyses = L3FieldImmutabilityAnalysis.class)
    T t;
    public LevelZero(T t){
        this.t = t;
    }
}

class LevelOne<T> {

    @DependentImmutableField(value = "", analyses = L3FieldImmutabilityAnalysis.class)
    private T t;

    public LevelOne(T t){
        this.t = t;
    }
}

class LevelTwo<T> {
    class InnerOne {
        @DependentImmutableField("")
        private T t;
        public InnerOne(T t){
            this.t = t;
        }
    }
}

class LevelThree<A> {
    class InnerOne<B> {
        class InnerTwo<C> {
            @DependentImmutableField("")
            private A a;

            @DependentImmutableField("")
            private B b;

            @DependentImmutableField("")
            private C c;

            public InnerTwo(A a, B b, C c){
                this.a = a;
                this.b = b;
                this.c = c;
            }
        }
    }
}

class LevelFour<A> {
    class InnerOne<B> {
        class InnerTwo<C> {
class InnerThree<D>{
    @DependentImmutableField(value = "", analyses = L3FieldImmutabilityAnalysis.class)
    private A a;

    @DependentImmutableField(value = "", analyses = L3FieldImmutabilityAnalysis.class)
    private B b;

    @DependentImmutableField(value = "", analyses = L3FieldImmutabilityAnalysis.class)
    private C c;

    @DependentImmutableField(value = "", analyses = L3FieldImmutabilityAnalysis.class)
    private D d;

    public InnerThree(A a, B b, C c, D d){
        this.a = a;
        this.b = b;
        this.c = c;
        this.d = d;
    }
}
        }
    }
}

class LevelFive<A> {
    class InnerOne<B> {
        class InnerTwo<C> {
            class InnerThree<D>{
class InnerFour<E>{

    @DependentImmutableField(value = "", analyses = L3FieldImmutabilityAnalysis.class)
    private A a;

    @DependentImmutableField(value = "", analyses = L3FieldImmutabilityAnalysis.class)
    private B b;

    @DependentImmutableField(value = "", analyses = L3FieldImmutabilityAnalysis.class)
    private C c;

    @DependentImmutableField(value = "", analyses = L3FieldImmutabilityAnalysis.class)
    private D d;

    @DependentImmutableField(value = "", analyses = L3FieldImmutabilityAnalysis.class)
    private E e;

    public InnerFour(A a, B b, C c, D d, E e){
        this.a = a;
        this.b = b;
        this.c = c;
        this.d = d;
        this.e = e;
    }
}
            }
        }
    }
}

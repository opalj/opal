package org.opalj.fpcf.fixtures.immutability.classes.multinested_genericClasses;

import org.opalj.fpcf.properties.immutability.fields.DependentlyImmutableField;
import org.opalj.tac.fpcf.analyses.immutability.L0FieldImmutabilityAnalysis;

public class MultipleNestedInnerClasses {
}
class LevelZero<T>{
    @DependentlyImmutableField(value = "", analyses = L0FieldImmutabilityAnalysis.class)
    T t;
    public LevelZero(T t){
        this.t = t;
    }
}

class LevelOne<T> {

    @DependentlyImmutableField(value = "", analyses = L0FieldImmutabilityAnalysis.class)
    private T t;

    public LevelOne(T t){
        this.t = t;
    }
}

class LevelTwo<T> {
    class InnerOne {
        @DependentlyImmutableField("")
        private T t;
        public InnerOne(T t){
            this.t = t;
        }
    }
}

class LevelThree<A> {
    class InnerOne<B> {
        class InnerTwo<C> {
            @DependentlyImmutableField("")
            private A a;

            @DependentlyImmutableField("")
            private B b;

            @DependentlyImmutableField("")
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
    @DependentlyImmutableField(value = "", analyses = L0FieldImmutabilityAnalysis.class)
    private A a;

    @DependentlyImmutableField(value = "", analyses = L0FieldImmutabilityAnalysis.class)
    private B b;

    @DependentlyImmutableField(value = "", analyses = L0FieldImmutabilityAnalysis.class)
    private C c;

    @DependentlyImmutableField(value = "", analyses = L0FieldImmutabilityAnalysis.class)
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

    @DependentlyImmutableField(value = "", analyses = L0FieldImmutabilityAnalysis.class)
    private A a;

    @DependentlyImmutableField(value = "", analyses = L0FieldImmutabilityAnalysis.class)
    private B b;

    @DependentlyImmutableField(value = "", analyses = L0FieldImmutabilityAnalysis.class)
    private C c;

    @DependentlyImmutableField(value = "", analyses = L0FieldImmutabilityAnalysis.class)
    private D d;

    @DependentlyImmutableField(value = "", analyses = L0FieldImmutabilityAnalysis.class)
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

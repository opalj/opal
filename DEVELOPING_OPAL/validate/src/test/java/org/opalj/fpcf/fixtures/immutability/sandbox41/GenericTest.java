package org.opalj.fpcf.fixtures.immutability.sandbox41;

import org.opalj.fpcf.properties.immutability.fields.DependentlyImmutableField;
import org.opalj.fpcf.properties.immutability.fields.NonTransitivelyImmutableField;

public class GenericTest {

}

final class MutableClass{
     public int n = 5;
}
final class EmptyClass{}

final class Generic<A,B,C> {

    @DependentlyImmutableField("")
    private A a;

    @DependentlyImmutableField("")
    private B b;

    @DependentlyImmutableField("")
    private C c;

    public Generic(A a, B b, C c){
        this.a = a;
        this.b = b;
        this.c = c;
    }
}

class Generic2<T>{

    @DependentlyImmutableField("")
    private Generic<T,T,T> a;

    @DependentlyImmutableField("")
    private Generic<T,T, EmptyClass> c;

    @DependentlyImmutableField("")
    private Generic<T,T,Generic<T,T,T>> d;

    @DependentlyImmutableField("")
    private Generic<T,T,Generic<T,T,Generic<T,T,T>>> e;

    @DependentlyImmutableField("")
    private Generic<T,T,Generic<T,T,Generic<T,T,EmptyClass>>> f;

    @NonTransitivelyImmutableField("")
    private Generic<T,T,MutableClass> g;

    @NonTransitivelyImmutableField("")
    private Generic<T,T,Generic<T,T,Generic<T,T,MutableClass>>> h;

}




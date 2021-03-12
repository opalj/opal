package org.opalj.fpcf.fixtures.immutability.sandbox41;

import org.opalj.fpcf.properties.immutability.fields.DependentImmutableField;
import org.opalj.fpcf.properties.immutability.fields.ShallowImmutableField;

public class GenericTest {

}

final class MutableClass{
     public int n = 5;
}
final class EmptyClass{}

final class Generic<A,B,C> {

    @DependentImmutableField("")
    private A a;

    @DependentImmutableField("")
    private B b;

    @DependentImmutableField("")
    private C c;

    public Generic(A a, B b, C c){
        this.a = a;
        this.b = b;
        this.c = c;
    }
}

class Generic2<T>{

    @DependentImmutableField("")
    private Generic<T,T,T> a;

    @DependentImmutableField("")
    private Generic<T,T, EmptyClass> c;

    @DependentImmutableField("")
    private Generic<T,T,Generic<T,T,T>> d;

    @DependentImmutableField("")
    private Generic<T,T,Generic<T,T,Generic<T,T,T>>> e;

    @DependentImmutableField("")
    private Generic<T,T,Generic<T,T,Generic<T,T,EmptyClass>>> f;

    @ShallowImmutableField("")
    private Generic<T,T,MutableClass> g;

    @ShallowImmutableField("")
    private Generic<T,T,Generic<T,T,Generic<T,T,MutableClass>>> h;

}




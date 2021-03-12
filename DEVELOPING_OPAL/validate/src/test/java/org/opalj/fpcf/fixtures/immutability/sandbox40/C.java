package org.opalj.fpcf.fixtures.immutability.sandbox40;

class GC<T>{
    T t;
    public GC(T t){this.t = t;}
}
/*
public class C<A> {
    private final GC gcDeep = new GC<Integer>(1);
    private final GC gcShallow =
            new GC<MutableClass>(new MutableClass());
    private final GC gcDependent;
    public C(A a){
        gcDependent = new GC<A>(a);
    } }
*/
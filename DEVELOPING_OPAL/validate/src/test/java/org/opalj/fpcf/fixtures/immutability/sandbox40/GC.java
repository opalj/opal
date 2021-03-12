package org.opalj.fpcf.fixtures.immutability.sandbox40;
/*
final class GC<T> {
    private final T t;
    public GC(T t){this.t = t; }}

    final class DeepImmutableClass{
    private final GC<Integer> gcDeep= new GC(1);
    }

    final class ShallowImmutableClass{
    private final GC<MutableClass> gcShallow = new GC<MutableClass>(new MutableClass());
    }

    class MutableClass{public int n = 10;}
*/


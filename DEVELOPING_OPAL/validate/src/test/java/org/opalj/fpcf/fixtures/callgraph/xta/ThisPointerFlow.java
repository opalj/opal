package org.opalj.fpcf.fixtures.callgraph.xta;

import org.opalj.fpcf.properties.callgraph.AvailableTypes;

public class ThisPointerFlow {

    @AvailableTypes({
            "[Ljava/lang/String;",
            "org/opalj/fpcf/fixtures/callgraph/xta/ThisPointerFlow$A1",
            "org/opalj/fpcf/fixtures/callgraph/xta/ThisPointerFlow$B"})
    public static void main(String[] args) {
        A obj = new A1();
        obj.foo(new B());
    }

    private static abstract class A {
        // No available types due to unreachable method.
        @AvailableTypes
        void foo(B obj) {
            // ...
        }

        @AvailableTypes({
                "org/opalj/fpcf/fixtures/callgraph/xta/ThisPointerFlow$A1",
                "org/opalj/fpcf/fixtures/callgraph/xta/ThisPointerFlow$B"})
        void bar(B obj) {
            this.baz(obj);
        }

        @AvailableTypes
        abstract void baz(B obj);
    }

    private static class A1 extends A {
        @AvailableTypes({
                "org/opalj/fpcf/fixtures/callgraph/xta/ThisPointerFlow$A1",
                "org/opalj/fpcf/fixtures/callgraph/xta/ThisPointerFlow$B"})
        void foo(B b) {
            // This is not a virtual call site since foo2 is private (uses invokespecial instruction).
            // "this" pointer and types should also flow through the private method.
            this.foo2(b);
        }

        @AvailableTypes({
                "org/opalj/fpcf/fixtures/callgraph/xta/ThisPointerFlow$A1",
                "org/opalj/fpcf/fixtures/callgraph/xta/ThisPointerFlow$B"})
        private void foo2(B b) {
            this.bar(b);
        }

        @AvailableTypes({
                "org/opalj/fpcf/fixtures/callgraph/xta/ThisPointerFlow$A1",
                "org/opalj/fpcf/fixtures/callgraph/xta/ThisPointerFlow$B"})
        void baz(B b) {
            // ...
        }
    }

    private static class B { }
}

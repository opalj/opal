package org.opalj.fpcf.fixtures.callgraph.xta;

import org.opalj.fpcf.properties.callgraph.AvailableTypes;

public class DynamicMethodFlows {

    @AvailableTypes()
    public static void main(String[] args) {
        dynamicTest();
    }

    @AvailableTypes("Lorg/opalj/fpcf/fixtures/callgraph/xta/DynamicMethodFlows$A1;")
    public static void dynamicTest() {
        A obj1 = new A1();
        dynamicTest_callsite(obj1);
    }

    @AvailableTypes({
            "Lorg/opalj/fpcf/fixtures/callgraph/xta/DynamicMethodFlows$A1;",
            "Lorg/opalj/fpcf/fixtures/callgraph/xta/DynamicMethodFlows$B1;"})
    public static void dynamicTest_callsite(A a) {
        // A1 flows from dynamicTest(), we know that the following dynamic dispatch can only
        // resolve to A1.foo(). This is more precise than RTA and CHA, which would resolve the
        // call against both A1 and A2.
        B obj2 = a.foo();

        // Since B1 flows back from A1.foo, we know that this dynamic dispatch can only resolve
        // against B1.foo(), but not against B2.foo().
        obj2.foo(a);
    }

    // === Test Class Hierarchy ===

    // First class hierarchy: A <-- A1 and A <-- A2
    private static abstract class A {
        @AvailableTypes()
        public abstract B foo();
    }

    private static class A1 extends A {
        @AvailableTypes("Lorg/opalj/fpcf/fixtures/callgraph/xta/DynamicMethodFlows$B1;")
        public B foo() {
            return new B1();
        }
    }

    private static class A2 extends A {
        // Type set is empty since this method is unreachable.
        @AvailableTypes()
        public B foo() {
            return new B2();
        }
    }

    // Second class hierarchy: B <-- B1 and B <-- B2
    private static abstract class B {
        // Type set is empty since this method is unreachable.
        @AvailableTypes()
        public abstract void foo(A obj);
    }

    private static class B1 extends B {
        @AvailableTypes("Lorg/opalj/fpcf/fixtures/callgraph/xta/DynamicMethodFlows$A1;")
        public void foo(A obj) {
            // ...
        }
    }

    private static class B2 extends B {
        // Type set is empty since this method is unreachable.
        @AvailableTypes()
        public void foo(A obj) {
            // ...
        }
    }
}
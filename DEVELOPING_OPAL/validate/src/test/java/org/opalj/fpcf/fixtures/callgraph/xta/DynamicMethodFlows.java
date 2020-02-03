package org.opalj.fpcf.fixtures.callgraph.xta;

import org.opalj.fpcf.properties.callgraph.AvailableTypes;

// This class contains a single, more complex integration test, testing type flow
// and dynamic call site resolution in multiple stages.
public class DynamicMethodFlows {

    @AvailableTypes({"[Ljava/lang/String;"})
    public static void main(String[] args) {
        dynamicTest();
    }

    @AvailableTypes("org/opalj/fpcf/fixtures/callgraph/xta/DynamicMethodFlows$A1")
    public static void dynamicTest() {
        A obj1 = new A1();
        dynamicTest_callsite(obj1);
    }

    @AvailableTypes({
            "org/opalj/fpcf/fixtures/callgraph/xta/DynamicMethodFlows$A1",
            "org/opalj/fpcf/fixtures/callgraph/xta/DynamicMethodFlows$B1",
            "org/opalj/fpcf/fixtures/callgraph/xta/DynamicMethodFlows$C1"})
    public static void dynamicTest_callsite(A a) {
        // A1 flows from dynamicTest(), we know that the following dynamic dispatch can only
        // resolve to A1.foo(). This is more precise than RTA and CHA, which would resolve the
        // call against both A1 and A2.
        B obj2 = a.foo();

        // Since B1 flows back from A1.foo, we know that this dynamic dispatch can only resolve
        // against B1.foo(), but not against B2.foo().
        C obj3 = obj2.foo(a);

        // Same reasoning as above: C1 flows through the return value of B1.foo. Since B2.foo
        // (which returns a C2) is not resolved for the call above, the type C2 is not available
        // here. Thus, the following virtual call site is resolved against C1.foo only.
        obj3.foo();
    }

    // === Test Class Hierarchy ===

    // First class hierarchy: A <-- A1 and A <-- A2
    private static abstract class A {
        @AvailableTypes()
        public abstract B foo();
    }

    private static class A1 extends A {
        @AvailableTypes({
                "org/opalj/fpcf/fixtures/callgraph/xta/DynamicMethodFlows$A1", // "this"
                "org/opalj/fpcf/fixtures/callgraph/xta/DynamicMethodFlows$B1"})
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
        public abstract C foo(A obj);
    }

    private static class B1 extends B {
        @AvailableTypes({
                "org/opalj/fpcf/fixtures/callgraph/xta/DynamicMethodFlows$A1",
                "org/opalj/fpcf/fixtures/callgraph/xta/DynamicMethodFlows$B1", // "this"
                "org/opalj/fpcf/fixtures/callgraph/xta/DynamicMethodFlows$C1"})
        public C foo(A obj) {
            return new C1();
        }
    }

    private static class B2 extends B {
        // Type set is empty since this method is unreachable. Because of that,
        // even C2, which has a direct allocation site within foo, is not in the type set!
        @AvailableTypes
        public C foo(A obj) {
            return new C2();
        }
    }

    // Third class hierarchy: C <-- C1 and C <-- C2
    private static abstract class C {
        @AvailableTypes
        public abstract void foo();
    }

    private static class C1 extends C {
        // Since this method is reachable, the type set contains the this pointer.
        @AvailableTypes("org/opalj/fpcf/fixtures/callgraph/xta/DynamicMethodFlows$C1") // "this"
        public void foo() {

        }
    }

    private static class C2 extends C {
        // Empty type set due to unreachable method.
        @AvailableTypes
        public void foo() {

        }
    }
}
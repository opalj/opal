/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.callgraph.fta;

import org.opalj.fpcf.properties.callgraph.AvailableTypes;

import static org.opalj.fpcf.properties.callgraph.TypePropagationVariant.FTA;

/**
 * The main functionality of type propagation is assured via the XTA tests. Since FTA is very similar, this file
 * only contains tests which concern the minor differences.
 *
 * @author Andreas Bauer
 */

@AvailableTypes(variants = FTA)
public class SupplementaryTests {

    @AvailableTypes(
            value = {"[Ljava/lang/String;"},
            variants = FTA)
    public static void main(String[] args) {
        Scope1.test();
        Scope2.test();
    }

    // === Test scope 1 ===
    // A basic test making sure that the field sets are empty and the types are attached to the class instead.

    @AvailableTypes(
            value = {"org/opalj/fpcf/fixtures/callgraph/fta/SupplementaryTests$A",
                    "org/opalj/fpcf/fixtures/callgraph/fta/SupplementaryTests$A1",
                    "org/opalj/fpcf/fixtures/callgraph/fta/SupplementaryTests$B"},
            variants = FTA)
    public static class Scope1 {

        @AvailableTypes(variants = FTA)
        public static A field1;

        @AvailableTypes(variants = FTA)
        public static B field2;

        @AvailableTypes(variants = FTA)
        public static A1 field3;

        @AvailableTypes(
                value = {"org/opalj/fpcf/fixtures/callgraph/fta/SupplementaryTests$A",
                        "org/opalj/fpcf/fixtures/callgraph/fta/SupplementaryTests$A1",
                        "org/opalj/fpcf/fixtures/callgraph/fta/SupplementaryTests$B"},
                variants = FTA)
        public static void test() {
            field1 = new A();
            field2 = new B();
            field3 = new A1();
            read();
        }

        @AvailableTypes(
                value = {"org/opalj/fpcf/fixtures/callgraph/fta/SupplementaryTests$A",
                        "org/opalj/fpcf/fixtures/callgraph/fta/SupplementaryTests$A1"},
                variants = FTA)
        public static void read() {
            // Only A was written to field1, but since all type sets for fields are merged, the A1 is also propagated
            // via the field read.
            A obj = field1;
            Sink.sink(obj);
        }
    }

    // === Test scope 2 ===
    // Tests that the call graph part does not use types written to the class to resolve call in a method.
    // This should never happen with FTA.

    @AvailableTypes(
            value = {"org/opalj/fpcf/fixtures/callgraph/fta/SupplementaryTests$Scope2$C",
                    "org/opalj/fpcf/fixtures/callgraph/fta/SupplementaryTests$Scope2$C1"},
            variants = FTA)
    public static class Scope2 {

        @AvailableTypes(variants = FTA)
        public static class C {
            @AvailableTypes(
                    value = {"org/opalj/fpcf/fixtures/callgraph/fta/SupplementaryTests$Scope2$C",
                             "org/opalj/fpcf/fixtures/callgraph/fta/SupplementaryTests$A"},
                    variants = FTA)
            public void foo(A a) {}
        }

        @AvailableTypes(variants = FTA)
        public static class C1 extends C {
            @AvailableTypes(variants = FTA) // Empty (since it is unreachable)!
            public void foo(A a) {}
        }

        @AvailableTypes(variants = FTA)
        public static C field;

        @AvailableTypes(
                value = "org/opalj/fpcf/fixtures/callgraph/fta/SupplementaryTests$Scope2$C",
                variants = FTA)
        public static void test() {
            // Write both C and C1 to the field. Both types are attached to the set of the class file.
            writeField();
            // Call "callsite" with C only!
            callsite(new C());
        }

        @AvailableTypes(
                value = {"org/opalj/fpcf/fixtures/callgraph/fta/SupplementaryTests$Scope2$C",
                        "org/opalj/fpcf/fixtures/callgraph/fta/SupplementaryTests$Scope2$C1"},
                variants = FTA)
        public static void writeField() {
            field = new C();
            field = new C1();
        }

        @AvailableTypes(
                value = {"org/opalj/fpcf/fixtures/callgraph/fta/SupplementaryTests$Scope2$C",
                         "org/opalj/fpcf/fixtures/callgraph/fta/SupplementaryTests$A"},
                variants = FTA)
        public static void callsite(C c) {
            A a = new A();

            // This callsite should only resolve against the types available in the set of this method (C).
            // The set of the class also contains C1, which should NOT be used to resolve the call.
            // The type set of C1.foo must therefore be empty (since it is never called) while the type set
            // of C.foo should contain A (propagated via the parameter of foo).
            c.foo(a);
        }
    }

    // === Class hierarchy for testing ===
    public static class A {}
    public static class A1 extends A {}
    public static class B {}

    // === Sink method ===
    public static class Sink {
        public static void sink(Object obj) { }
    }
}

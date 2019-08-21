/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.callgraph.mta;

import org.opalj.fpcf.properties.callgraph.AvailableTypes;

import static org.opalj.fpcf.properties.callgraph.TypePropagationVariant.CTA;
import static org.opalj.fpcf.properties.callgraph.TypePropagationVariant.MTA;

/**
 * The main functionality of type propagation is assured via the XTA tests. Since MTA is very similar, this file
 * only contains tests which concern the minor differences.
 *
 * These tests also apply to CTA, since method sets for methods are also merged in CTA (and these tests do not contain
 * any fields which would affect the outcome).
 *
 * @author Andreas Bauer
 */
@AvailableTypes(
        value = {"[Ljava/lang/String;"},
        variants = { MTA, CTA })
public class SupplementaryTests {

    @AvailableTypes(variants = MTA)
    public static void main(String[] args) {
        Scope1.test();
    }

    @AvailableTypes(
            value = {"org/opalj/fpcf/fixtures/callgraph/mta/SupplementaryTests$A",
                     "org/opalj/fpcf/fixtures/callgraph/mta/SupplementaryTests$B"},
            variants = { MTA, CTA })
    public static class Scope1 {
        // In MTA, methods should not have any types attached to them.
        @AvailableTypes(variants = { MTA, CTA })
        public static void test() {
            A obj1 = new A();
            B obj2 = new B();
            sink1(obj1);
            Scope2.sink2(obj2);
            Scope3.test();
        }

        @AvailableTypes(variants = { MTA, CTA })
        public static void sink1(A obj) {}
    }

    @AvailableTypes(
            value = {
                    "org/opalj/fpcf/fixtures/callgraph/mta/SupplementaryTests$A1",
                    "org/opalj/fpcf/fixtures/callgraph/mta/SupplementaryTests$B"
            },
            variants = { MTA, CTA })
    public static class Scope2 {
        @AvailableTypes(variants = { MTA, CTA })
        public static void sink1(A obj) {}

        @AvailableTypes(variants = { MTA, CTA })
        public static void sink2(B obj) {}

        @AvailableTypes(variants = { MTA, CTA })
        public static void sink3(C obj) {}
    }

    @AvailableTypes(
            value = "org/opalj/fpcf/fixtures/callgraph/mta/SupplementaryTests$A1",
            variants = { MTA, CTA })
    public static class Scope3 {
        public static void test() {
            A obj1 = new A1();
            Scope2.sink1(obj1);
        }
    }

    // === Class hierarchy for testing ===
    public static class A {}
    public static class A1 extends A {}
    public static class B {}
    public static class C {}
}

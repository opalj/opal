/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.callgraph.mta;

import org.opalj.fpcf.properties.callgraph.AvailableTypes;
import org.opalj.tac.fpcf.analyses.cg.xta.MTATypePropagationAnalysis;

/**
 * The main functionality of type propagation is assured via the XTA tests. Since MTA is very similar, this file
 * only contains tests which concern the minor differences.
 *
 * @author Andreas Bauer
 */
@AvailableTypes(
        value = {"java/lang/String", "[Ljava/lang/String;"},
        analyses = MTATypePropagationAnalysis.class)
public class SupplementaryTests {

    @AvailableTypes(analyses = MTATypePropagationAnalysis.class)
    public static void main(String[] args) {
        Scope1.test();
    }

    @AvailableTypes(
            value = {"org/opalj/fpcf/fixtures/callgraph/mta/SupplementaryTests$A",
                     "org/opalj/fpcf/fixtures/callgraph/mta/SupplementaryTests$B"},
            analyses = MTATypePropagationAnalysis.class)
    public static class Scope1 {
        // In MTA, methods should not have any types attached to them.
        @AvailableTypes(analyses = MTATypePropagationAnalysis.class)
        public static void test() {
            A obj1 = new A();
            B obj2 = new B();
            sink1(obj1);
            Scope2.sink2(obj2);
            Scope3.test();
        }

        @AvailableTypes(analyses = MTATypePropagationAnalysis.class)
        public static void sink1(A obj) {}
    }

    @AvailableTypes(
            value = {
                    "org/opalj/fpcf/fixtures/callgraph/mta/SupplementaryTests$A1",
                    "org/opalj/fpcf/fixtures/callgraph/mta/SupplementaryTests$B"
            },
            analyses = MTATypePropagationAnalysis.class)
    public static class Scope2 {
        @AvailableTypes(analyses = MTATypePropagationAnalysis.class)
        public static void sink1(A obj) {}

        @AvailableTypes(analyses = MTATypePropagationAnalysis.class)
        public static void sink2(B obj) {}

        @AvailableTypes(analyses = MTATypePropagationAnalysis.class)
        public static void sink3(C obj) {}
    }

    @AvailableTypes(
            value = "org/opalj/fpcf/fixtures/callgraph/mta/SupplementaryTests$A1",
            analyses = MTATypePropagationAnalysis.class)
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

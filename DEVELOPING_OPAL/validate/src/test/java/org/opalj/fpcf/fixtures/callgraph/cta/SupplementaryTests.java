/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.callgraph.cta;

import org.opalj.fpcf.properties.callgraph.AvailableTypes;
import org.opalj.tac.fpcf.analyses.cg.xta.CTATypePropagationAnalysis;

/**
 * The main functionality of type propagation is assured via the XTA tests. Since CTA is very similar, this file
 * only contains tests which concern the minor differences.
 *
 * @author Andreas Bauer
 */

@AvailableTypes(
        value = {"java/lang/String", "[Ljava/lang/String;"},
        analyses = CTATypePropagationAnalysis.class)
public class SupplementaryTests {

    @AvailableTypes(analyses = CTATypePropagationAnalysis.class)
    public static void main(String[] args) {
        Scope1.test();
    }

    @AvailableTypes(
            value = {"org/opalj/fpcf/fixtures/callgraph/cta/SupplementaryTests$A",
                    "org/opalj/fpcf/fixtures/callgraph/cta/SupplementaryTests$A1",
                    "org/opalj/fpcf/fixtures/callgraph/cta/SupplementaryTests$B",
                    "org/opalj/fpcf/fixtures/callgraph/cta/SupplementaryTests$B1"},
            analyses = CTATypePropagationAnalysis.class)
    public static class Scope1 {

        @AvailableTypes(analyses = CTATypePropagationAnalysis.class)
        public static A field1;

        @AvailableTypes(analyses = CTATypePropagationAnalysis.class)
        public static B field2;

        @AvailableTypes(analyses = CTATypePropagationAnalysis.class)
        public static A1 field3;

        @AvailableTypes(analyses = CTATypePropagationAnalysis.class)
        public static void test() {
            field1 = new A();
            field2 = new B();
            other();
        }

        @AvailableTypes(analyses = CTATypePropagationAnalysis.class)
        public static void other() {
            field3 = new A1();
            // A and A1 are propagated to the class Scope2. In XTA, only A1 would've been propagated since neither
            // field3 not the method other would receive A.
            Scope2.sink(field3);

            // B and B1 are propagated to the class Scope3. In XTA, only B1 would be propagated.
            Scope3.field1 = new B1();
        }
    }

    @AvailableTypes(
            value = {"org/opalj/fpcf/fixtures/callgraph/cta/SupplementaryTests$A",
                    "org/opalj/fpcf/fixtures/callgraph/cta/SupplementaryTests$A1"},
            analyses = CTATypePropagationAnalysis.class)
    public static class Scope2 {
        public static void sink(A obj) {}
    }

    @AvailableTypes(
            value = {"org/opalj/fpcf/fixtures/callgraph/cta/SupplementaryTests$B",
                    "org/opalj/fpcf/fixtures/callgraph/cta/SupplementaryTests$B1"},
            analyses = CTATypePropagationAnalysis.class)
    public static class Scope3 {

        @AvailableTypes(analyses = CTATypePropagationAnalysis.class)
        public static B field1;
    }

    // === Class hierarchy for testing ===
    public static class A {}
    public static class A1 extends A {}
    public static class B {}
    public static class B1 extends B {}

    // === Sink method ===
    public static class Sink {
        public static void sink(Object obj) { }
    }
}

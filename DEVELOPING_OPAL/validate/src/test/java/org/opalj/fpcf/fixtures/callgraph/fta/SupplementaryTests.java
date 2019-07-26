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
            value = {"java/lang/String", "[Ljava/lang/String;"},
            variants = FTA)
    public static void main(String[] args) {
        Scope1.test();
    }

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
            // Only A was written to field1, but since all type sets for fields are merged, the field read also
            // propagates A1 which was written to field3!
            A obj = field1;
            Sink.sink(obj);
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

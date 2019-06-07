/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.callgraph.xta;

import org.opalj.fpcf.properties.callgraph.AvailableTypes;

/**
 * Testing type data flow between methods for XTA.
 *
 * @author Andreas Bauer
 */
public class StaticMethodFlows {
    // The main enty point. The annotation ensures that no values flow back since the return
    // type is void.
    // TODO AB It's possible that this method is going to have types through the args parameter.
    @AvailableTypes()
    public static void main(String[] args) {
        // Call the tests. No data-flow here that should influence the results.
        parameterFlow();
        returnValueFlow();
        twoWayFlow();
    }

    // === Test 1: ===
    // Data flow from caller to callee.
    // A2 and B2 are available because of the constructor calls.
    // The respective subsets should flow to the data sinks.
    @AvailableTypes({
            "Lorg/opalj/fpcf/fixtures/callgraph/xta/StaticMethodFlows$A2;",
            "Lorg/opalj/fpcf/fixtures/callgraph/xta/StaticMethodFlows$B2;"})
    public static void parameterFlow() {
        A1 obj1 = new A2();
        parameterFlow_sinkA(obj1);
        B1 obj2 = new B2();
        parameterFlow_sinkB(obj2);
    }

    @AvailableTypes("Lorg/opalj/fpcf/fixtures/callgraph/xta/StaticMethodFlows$A2;")
    public static void parameterFlow_sinkA(A1 obj) {
        // ...
    }

    @AvailableTypes("Lorg/opalj/fpcf/fixtures/callgraph/xta/StaticMethodFlows$B2;")
    public static void parameterFlow_sinkB(B1 obj) {
        // ...
    }

    // === Test 2: ===
    // Data flow from callee to caller, through return value.
    // The source method constructs objects of type A2 and B2, but
    // only A2 flows back to the caller since B2 is not a subtype of A1.
    @AvailableTypes("Lorg/opalj/fpcf/fixtures/callgraph/xta/StaticMethodFlows$A2;")
    public static void returnValueFlow() {
        A1 obj = returnValueFlow_Source();
    }

    @AvailableTypes({
            "Lorg/opalj/fpcf/fixtures/callgraph/xta/StaticMethodFlows$A2;",
            "Lorg/opalj/fpcf/fixtures/callgraph/xta/StaticMethodFlows$B2;"})
    public static A1 returnValueFlow_Source() {
        A1 obj1 = new A2();
        B1 obj2 = new B2();
        return obj1;
    }

    // === Test 3: ===
    // Data flows in both directions.
    @AvailableTypes({
            "Lorg/opalj/fpcf/fixtures/callgraph/xta/StaticMethodFlows$A2;",
            "Lorg/opalj/fpcf/fixtures/callgraph/xta/StaticMethodFlows$B2;"})
    public static void twoWayFlow() {
        A1 obj1 = new A2();
        B1 obj2 = twoWayFlow_SourceAndSink(obj1);
    }

    @AvailableTypes({
            "Lorg/opalj/fpcf/fixtures/callgraph/xta/StaticMethodFlows$A2;",
            "Lorg/opalj/fpcf/fixtures/callgraph/xta/StaticMethodFlows$B2;"})
    public static B1 twoWayFlow_SourceAndSink(A1 obj) {
        B1 obj1 = new B2();
        return obj1;
    }

    // === Test Class Hierarchy ===

    // First class hierarchy: A1 <-- A2
    private static class A1 {
        public void foo() {
            // ...
        }
    }

    private static class A2 extends A1 {
        public void foo() {
            // ...
        }
    }

    // Second class hierarchy: B1 <-- B2
    private static class B1 {
        public void foo() {
            // ...
        }
    }

    private static class B2 extends B1 {
        public void foo() {
            // ...
        }
    }
}
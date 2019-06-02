/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.callgraph.xta;

import org.opalj.fpcf.properties.callgraph.AvailableTypes;
import org.opalj.fpcf.properties.callgraph.DirectCall;
import org.opalj.tac.fpcf.analyses.cg.CHACallGraphAnalysis;
import org.opalj.tac.fpcf.analyses.cg.rta.RTACallGraphAnalysis;

/**
 * Testing type data flow between methods for XTA.
 *
 * @author Andreas Bauer
 */
public class MethodFlows {
    // The main enty point.
    public static void main(String[] args) {
        // Call the tests. No data-flow here that should influence the results.
        parameterFlow();
        returnValueFlow();
    }

    // === Test 1: ===
    // Data flow from caller to callee.
    // A2 and B2 are available because of the constructor calls.
    // The respective subsets should flow to the data sinks.
    @AvailableTypes({
            "Lorg/opalj/fpcf/fixtures/callgraph/xta/A2;",
            "Lorg/opalj/fpcf/fixtures/callgraph/xta/B2;"})
    public static void parameterFlow() {
        A1 obj1 = new A2();
        parameterFlow_sinkA(obj1);
        B1 obj2 = new B2();
        parameterFlow_sinkB(obj2);
    }

    @AvailableTypes("Lorg/opalj/fpcf/fixtures/callgraph/xta/A2;")
    public static void parameterFlow_sinkA(A1 obj) {
        // ...
    }

    @AvailableTypes("Lorg/opalj/fpcf/fixtures/callgraph/xta/B2;")
    public static void parameterFlow_sinkB(B1 obj) {
        // ...
    }

    // === Test 2: ===
    // Data flow from callee to caller, through return value.
    // The source method constructs objects of type A2 and B2, but
    // only A2 flows back to the caller since B2 is not a subtype of A1.
    @AvailableTypes("Lorg/opalj/fpcf/fixtures/callgraph/xta/A2;")
    public static void returnValueFlow() {
        A1 obj = returnValueFlow_Source();
    }

    @AvailableTypes({
            "Lorg/opalj/fpcf/fixtures/callgraph/xta/A2;",
            "Lorg/opalj/fpcf/fixtures/callgraph/xta/B2;"})
    public static A1 returnValueFlow_Source() {
        A1 obj1 = new A2();
        B1 obj2 = new B2();
        return obj1;
    }
}

// First class hierarchy: A1 <-- A2
class A1 {
    public void foo() {
        // ...
    }
}

class A2 extends A1 {
    public void foo() {
        // ...
    }
}

// Second class hierarchy: B1 <-- B2
class B1 {
    public void foo() {
        // ...
    }
}

class B2 extends B1 {
    public void foo() {
        // ...
    }
}
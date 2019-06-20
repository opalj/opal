package org.opalj.fpcf.fixtures.callgraph.xta;

import org.opalj.fpcf.properties.callgraph.AvailableTypes;

public class FieldFlows {
    @AvailableTypes
    public static void main(String[] args) {
        staticFieldFlowTest();
        staticArrayFieldFlowTest();
    }

    // === Type flow through fields ===
    // In this test, a value is written to a (static) field in one method
    // and read in another. Note that there is no direct flow between methods,
    // since both methods have no parameters and void return type. Instead,
    // the single constructed type A1 flows indirectly through the field.

    @AvailableTypes
    private static void staticFieldFlowTest() {
        staticFieldFlowTest_Write();
        staticFieldFlowTest_Read();
    }

    @AvailableTypes("org/opalj/fpcf/fixtures/callgraph/xta/FieldFlows$A1")
    private static A field;

    @AvailableTypes("org/opalj/fpcf/fixtures/callgraph/xta/FieldFlows$A1")
    private static void staticFieldFlowTest_Write() {
        A obj = new A1();
        field = obj;
    }

    @AvailableTypes("org/opalj/fpcf/fixtures/callgraph/xta/FieldFlows$A1")
    private static void staticFieldFlowTest_Read() {
        A obj = field;
    }

    // === Array field test ===
    // This test asserts that flow through fields also works for array types.

    private static void staticArrayFieldFlowTest() {
        staticArrayFieldFlowTest_Read();
        staticArrayFieldFlowTest_Write();
    }

    private static A[] field2;

    @AvailableTypes("[Lorg/opalj/fpcf/fixtures/callgraph/xta/FieldFlows$A1;")
    private static void staticArrayFieldFlowTest_Write() {
        A[] obj = new A1[1];
        field2 = obj;
    }

    @AvailableTypes("[Lorg/opalj/fpcf/fixtures/callgraph/xta/FieldFlows$A1;")
    private static void staticArrayFieldFlowTest_Read() {
        Object obj = field2;
    }

    // First class hierarchy: A <-- A1 and A <-- A2
    private static class A {
    }

    private static class A1 extends A {
    }

    private static class A2 extends A {
    }
}

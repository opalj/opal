package org.opalj.fpcf.fixtures.callgraph.xta;

import org.opalj.fpcf.properties.callgraph.AvailableTypes;

public class FieldFlows {
    private static void sink(Object obj) { }

    @AvailableTypes({"[Ljava/lang/String;"})
    public static void main(String[] args) {
        staticFieldFlowTest();
        staticArrayFieldFlowTest();
        shadowedFieldTest();
        genericFieldTest();
    }

    // === Type flow through fields ===
    // In this test, a value is written to a (static) field in one method
    // and read in another. Note that there is no direct flow between methods,
    // since all methods have no parameters and void return type. Instead,
    // the types A1 and A2 flow indirectly through the field.

    @AvailableTypes
    private static void staticFieldFlowTest() {
        // Note: Order of calls does not matter, result is the same.
        staticFieldFlowTest_Write1();
        staticFieldFlowTest_Read();
        staticFieldFlowTest_Write1();
        staticFieldFlowTest_Write2();
    }

    @AvailableTypes({
            "org/opalj/fpcf/fixtures/callgraph/xta/FieldFlows$A1",
            "org/opalj/fpcf/fixtures/callgraph/xta/FieldFlows$A2"})
    private static A field;

    @AvailableTypes("org/opalj/fpcf/fixtures/callgraph/xta/FieldFlows$A1")
    private static void staticFieldFlowTest_Write1() {
        A obj = new A1();
        field = obj;
    }

    @AvailableTypes("org/opalj/fpcf/fixtures/callgraph/xta/FieldFlows$A2")
    private static void staticFieldFlowTest_Write2() {
        A obj = new A2();
        field = obj;
    }

    @AvailableTypes({
            "org/opalj/fpcf/fixtures/callgraph/xta/FieldFlows$A1",
            "org/opalj/fpcf/fixtures/callgraph/xta/FieldFlows$A2"})
    private static void staticFieldFlowTest_Read() {
        A obj = field;
        sink(obj);
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
        sink(obj);
    }

    // === Shadowed field test ===

    private static class B {
        @AvailableTypes("org/opalj/fpcf/fixtures/callgraph/xta/FieldFlows$A1")
        A field;
    }
    private static class B1 extends B {
        @AvailableTypes // Empty!
        A field;
    }

    @AvailableTypes({
            "org/opalj/fpcf/fixtures/callgraph/xta/FieldFlows$A1",
            "org/opalj/fpcf/fixtures/callgraph/xta/FieldFlows$B1"})
    public static void shadowedFieldTest() {
        B obj = new B1();
        // Field stores are not virtual, therefore A1 is written to B.field, NOT to B1.field!
        obj.field = new A1();

        shadowedFieldTest_sink1(obj);
        shadowedFieldTest_sink2((B1)obj);
    }

    @AvailableTypes({
            "org/opalj/fpcf/fixtures/callgraph/xta/FieldFlows$A1",
            "org/opalj/fpcf/fixtures/callgraph/xta/FieldFlows$B1"})
    public static void shadowedFieldTest_sink1(B b) {
        Object obj = b.field;
        sink(obj);
    }

    @AvailableTypes({
            "org/opalj/fpcf/fixtures/callgraph/xta/FieldFlows$B1"})
    public static void shadowedFieldTest_sink2(B1 b1) {
        // Since no actual value is written to the B1.field, this read will return null.
        // Thus, no subtype of A is available in this method's type set!
        Object obj = b1.field;
        sink(obj);
    }

    // === Generic field ===
    private static class Box<T> {
        public T field;
    }

    @AvailableTypes({
            "org/opalj/fpcf/fixtures/callgraph/xta/FieldFlows$Box",
            "org/opalj/fpcf/fixtures/callgraph/xta/FieldFlows$A",
            "org/opalj/fpcf/fixtures/callgraph/xta/FieldFlows$B"})
    public static void genericFieldTest() {
        Box<A> box1 = new Box<>();
        box1.field = new A();

        Box<B> box2 = new Box<>();
        box2.field = new B();

        genericFieldTest_sink(box1);
        genericFieldTest_sink2(box1, box2);
    }

    @AvailableTypes({
            "org/opalj/fpcf/fixtures/callgraph/xta/FieldFlows$Box",
            "org/opalj/fpcf/fixtures/callgraph/xta/FieldFlows$A"})
    public static void genericFieldTest_sink(Box<A> box) {
        // Compiler should insert a checkcast here from which we can get the "actual" field type.
        A obj = box.field;
        sink(obj);
    }

    @AvailableTypes({
            "org/opalj/fpcf/fixtures/callgraph/xta/FieldFlows$Box",
            "org/opalj/fpcf/fixtures/callgraph/xta/FieldFlows$A",
            "org/opalj/fpcf/fixtures/callgraph/xta/FieldFlows$B"})
    public static void genericFieldTest_sink2(Box<A> box1, Box<B> box2) {
        A obj1 = box1.field;
        B obj2 = box2.field;
        sink(obj1);
        sink(obj2);
    }

    // === Common classes ===

    private static class A {}
    private static class A1 extends A {}
    private static class A2 extends A {}
}

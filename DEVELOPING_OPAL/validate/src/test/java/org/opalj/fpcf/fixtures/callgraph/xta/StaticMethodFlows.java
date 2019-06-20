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
    // TODO AB It's possible that this method is going to receive types through the args parameter.
    @AvailableTypes()
    public static void main(String[] args) {
        // Call the tests. No data-flow here that should influence the results.
        parameterFlow();
        returnValueFlow();
        twoWayFlow();
        arrayTest();
        arrayTest2();
        arrayTest3();
        recursionTest();
    }

    // === Test 1: ===
    // Data flow from caller to callee.
    // A2 and B2 are available because of the constructor calls.
    // The respective subsets should flow to the data sinks.
    @AvailableTypes({
            "org/opalj/fpcf/fixtures/callgraph/xta/StaticMethodFlows$A2",
            "org/opalj/fpcf/fixtures/callgraph/xta/StaticMethodFlows$B2"})
    public static void parameterFlow() {
        A1 obj1 = new A2();
        parameterFlow_sinkA(obj1);
        B1 obj2 = new B2();
        parameterFlow_sinkB(obj2);
    }

    @AvailableTypes("org/opalj/fpcf/fixtures/callgraph/xta/StaticMethodFlows$A2")
    public static void parameterFlow_sinkA(A1 obj) {
        // ...
    }

    @AvailableTypes("org/opalj/fpcf/fixtures/callgraph/xta/StaticMethodFlows$B2")
    public static void parameterFlow_sinkB(B1 obj) {
        // ...
    }

    // === Test 2: ===
    // Data flow from callee to caller, through return value.
    // The source method constructs objects of type A2 and B2, but
    // only A2 flows back to the caller since B2 is not a subtype of A1.
    @AvailableTypes("org/opalj/fpcf/fixtures/callgraph/xta/StaticMethodFlows$A2")
    public static void returnValueFlow() {
        A1 obj = returnValueFlow_Source();
    }

    @AvailableTypes({
            "org/opalj/fpcf/fixtures/callgraph/xta/StaticMethodFlows$A2",
            "org/opalj/fpcf/fixtures/callgraph/xta/StaticMethodFlows$B2"})
    public static A1 returnValueFlow_Source() {
        A1 obj1 = new A2();
        B1 obj2 = new B2();
        return obj1;
    }

    // === Test 3: ===
    // Data flows in both directions.
    @AvailableTypes({
            "org/opalj/fpcf/fixtures/callgraph/xta/StaticMethodFlows$A2",
            "org/opalj/fpcf/fixtures/callgraph/xta/StaticMethodFlows$B2"})
    public static void twoWayFlow() {
        A1 obj1 = new A2();
        B1 obj2 = twoWayFlow_SourceAndSink(obj1);
    }

    @AvailableTypes({
            "org/opalj/fpcf/fixtures/callgraph/xta/StaticMethodFlows$A2",
            "org/opalj/fpcf/fixtures/callgraph/xta/StaticMethodFlows$B2"})
    public static B1 twoWayFlow_SourceAndSink(A1 obj) {
        B1 obj1 = new B2();
        return obj1;
    }

    // === Test 4: ===
    // In this test, two different types are written to an array. The array is accessed in
    // in arrayTest_sink, which should make both types available in this method.
    @AvailableTypes("[Lorg/opalj/fpcf/fixtures/callgraph/xta/StaticMethodFlows$A1;")
    public static void arrayTest() {
        // Array is allocated in another method in order to test backward flow of the array type as well.
        A1[] arr = arrayTest_alloc();
        arrayTest_source1(arr);
        arrayTest_source2(arr);
        arrayTest_sink(arr);
    }

    @AvailableTypes("[Lorg/opalj/fpcf/fixtures/callgraph/xta/StaticMethodFlows$A1;")
    public static A1[] arrayTest_alloc() {
        // These are not arrays of object types, therefore they should not be tracked.
        int[] foo = new int[10];
        int[][] foo2 = new int[10][10];

        return new A1[2];
    }

    @AvailableTypes({
            "org/opalj/fpcf/fixtures/callgraph/xta/StaticMethodFlows$A1",
            "[Lorg/opalj/fpcf/fixtures/callgraph/xta/StaticMethodFlows$A1;"})
    public static void arrayTest_source1(A1[] arr) {
        arr[0] = new A1();
    }

    @AvailableTypes({
            "org/opalj/fpcf/fixtures/callgraph/xta/StaticMethodFlows$A2",
            "[Lorg/opalj/fpcf/fixtures/callgraph/xta/StaticMethodFlows$A1;"})
    public static void arrayTest_source2(A1[] arr) {
        arr[1] = new A2();
    }

    @AvailableTypes({
            "org/opalj/fpcf/fixtures/callgraph/xta/StaticMethodFlows$A1",
            "org/opalj/fpcf/fixtures/callgraph/xta/StaticMethodFlows$A2",
            "[Lorg/opalj/fpcf/fixtures/callgraph/xta/StaticMethodFlows$A1;"})
    public static void arrayTest_sink(A1[] arr) {
        A1 obj = arr[0];
    }

    // === Test 5: ===
    // Here, two differently typed arrays flow into the sink separately. The result should be the same as
    // above.
    @AvailableTypes({
            "[Lorg/opalj/fpcf/fixtures/callgraph/xta/StaticMethodFlows$A1;",
            "[Lorg/opalj/fpcf/fixtures/callgraph/xta/StaticMethodFlows$A1;"})
    public static void arrayTest2() {
        A1[] arr = new A1[2];
        A1[] arr2 = new A1[2];
        arrayTest2_source1(arr);
        arrayTest2_sink(arr);
        arrayTest2_source2(arr2);
        arrayTest2_sink(arr2);
    }

    @AvailableTypes({
            "org/opalj/fpcf/fixtures/callgraph/xta/StaticMethodFlows$A1",
            "[Lorg/opalj/fpcf/fixtures/callgraph/xta/StaticMethodFlows$A1;"})
    public static void arrayTest2_source1(A1[] arr) {
        arr[0] = new A1();
    }

    @AvailableTypes({
            "org/opalj/fpcf/fixtures/callgraph/xta/StaticMethodFlows$A2",
            "[Lorg/opalj/fpcf/fixtures/callgraph/xta/StaticMethodFlows$A1;"})
    public static void arrayTest2_source2(A1[] arr) {
        arr[1] = new A2();
    }

    @AvailableTypes({
            "Lorg/opalj/fpcf/fixtures/callgraph/xta/StaticMethodFlows$A1;",
            "Lorg/opalj/fpcf/fixtures/callgraph/xta/StaticMethodFlows$A2;",
            "[Lorg/opalj/fpcf/fixtures/callgraph/xta/StaticMethodFlows$A1;"})
    public static void arrayTest2_sink(A1[] arr) {
        A1 obj = arr[0];
    }

    // === Test 6: ===
    // Here, two differently typed arrays flow into the sink.
    @AvailableTypes({
            "[Lorg/opalj/fpcf/fixtures/callgraph/xta/StaticMethodFlows$A1;",
            "[Lorg/opalj/fpcf/fixtures/callgraph/xta/StaticMethodFlows$B1;"})
    public static void arrayTest3() {
        A1[] arr = new A1[2];
        B1[] arr2 = new B1[2];
        arrayTest3_source1(arr);
        arrayTest3_sink(arr);
        arrayTest3_source2(arr2);
        arrayTest3_sink(arr2);
    }

    @AvailableTypes({
            "org/opalj/fpcf/fixtures/callgraph/xta/StaticMethodFlows$A1",
            "[Lorg/opalj/fpcf/fixtures/callgraph/xta/StaticMethodFlows$A1;"})
    public static void arrayTest3_source1(A1[] arr) {
        arr[0] = new A1();
    }

    @AvailableTypes({
            "org/opalj/fpcf/fixtures/callgraph/xta/StaticMethodFlows$B1",
            "[Lorg/opalj/fpcf/fixtures/callgraph/xta/StaticMethodFlows$B1;"})
    public static void arrayTest3_source2(B1[] arr) {
        arr[0] = new B1();
    }

    @AvailableTypes({
            "org/opalj/fpcf/fixtures/callgraph/xta/StaticMethodFlows$A1",
            "org/opalj/fpcf/fixtures/callgraph/xta/StaticMethodFlows$B1",
            "[Lorg/opalj/fpcf/fixtures/callgraph/xta/StaticMethodFlows$A1;",
            "[Lorg/opalj/fpcf/fixtures/callgraph/xta/StaticMethodFlows$A1;"})
    public static void arrayTest3_sink(Object[] arr) {
        Object obj = arr[0];
    }


    // === Recursive methods ===

    @AvailableTypes("org/opalj/fpcf/fixtures/callgraph/xta/StaticMethodFlows$A1")
    public static void recursionTest() {
        recursiveMethod(new A1());
    }

    @AvailableTypes("org/opalj/fpcf/fixtures/callgraph/xta/StaticMethodFlows$A1")
    public static void recursiveMethod(A1 a) {
        // This will not terminate obviously, but that shouldn't matter for the static analysis.
        recursiveMethod(a);
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
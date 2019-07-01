/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.callgraph.xta;

import org.opalj.fpcf.properties.callgraph.AvailableTypes;

import java.util.ArrayList;
import java.util.List;

/**
 * Testing type data flow between methods for XTA.
 *
 * @author Andreas Bauer
 */
public class StaticMethodFlows {
    // The main enty point. The annotation ensures that no values flow back from the called methods
    // since the return type is void. Since it is a main method, string and string array are available
    // by default.
    @AvailableTypes({"java/lang/String", "[Ljava/lang/String;"})
    public static void main(String[] args) {
        // Call the tests. No data-flow here that should influence the results.
        parameterFlow();
        returnValueFlow();
        twoWayFlow();
        arrayTest();
        arrayTest2();
        arrayTest3();
        multiDimensionalArrayTest();
        recursionTest();
        externalWorld();
    }

    // === Parameter flow ===
    // Data flow from caller to callee.
    // A1 and B1 are available because of the constructor calls.
    // The respective subsets should flow to the data sinks.
    @AvailableTypes({
            "org/opalj/fpcf/fixtures/callgraph/xta/StaticMethodFlows$A1",
            "org/opalj/fpcf/fixtures/callgraph/xta/StaticMethodFlows$B1"})
    public static void parameterFlow() {
        A obj1 = new A1();
        parameterFlow_sinkA(obj1);
        B obj2 = new B1();
        parameterFlow_sinkB(obj2);
    }

    @AvailableTypes("org/opalj/fpcf/fixtures/callgraph/xta/StaticMethodFlows$A1")
    public static void parameterFlow_sinkA(A obj) {
        // ...
    }

    @AvailableTypes("org/opalj/fpcf/fixtures/callgraph/xta/StaticMethodFlows$B1")
    public static void parameterFlow_sinkB(B obj) {
        // ...
    }

    // === Return value flow ===
    // Data flow from callee to caller, through return value.
    // The source method constructs objects of type A1 and B1, but
    // only A1 flows back to the caller since B1 is not a subtype of A.
    @AvailableTypes("org/opalj/fpcf/fixtures/callgraph/xta/StaticMethodFlows$A1")
    public static void returnValueFlow() {
        A obj = returnValueFlow_Source();
    }

    @AvailableTypes({
            "org/opalj/fpcf/fixtures/callgraph/xta/StaticMethodFlows$A1",
            "org/opalj/fpcf/fixtures/callgraph/xta/StaticMethodFlows$B1"})
    public static A returnValueFlow_Source() {
        A obj1 = new A1();
        B obj2 = new B1();
        return obj1;
    }

    // === Combined forward/backward flow ===
    // Data flows in both directions.
    @AvailableTypes({
            "org/opalj/fpcf/fixtures/callgraph/xta/StaticMethodFlows$A1",
            "org/opalj/fpcf/fixtures/callgraph/xta/StaticMethodFlows$B1"})
    public static void twoWayFlow() {
        A obj1 = new A1();
        B obj2 = twoWayFlow_SourceAndSink(obj1);
    }

    @AvailableTypes({
            "org/opalj/fpcf/fixtures/callgraph/xta/StaticMethodFlows$A1",
            "org/opalj/fpcf/fixtures/callgraph/xta/StaticMethodFlows$B1"})
    public static B twoWayFlow_SourceAndSink(A obj) {
        B obj1 = new B1();
        return obj1;
    }

    // === Array test ===
    // Note: All array tests use different types for isolation, since values written to ArrayTypes
    // are available globally, across test boundaries.

    // In this test, two different types are written to an array. The array is accessed in
    // in arrayTest_sink, which should make both types available in this method.
    @AvailableTypes("[Lorg/opalj/fpcf/fixtures/callgraph/xta/StaticMethodFlows$A;")
    public static void arrayTest() {
        // Array is allocated in another method in order to test backward flow of the array type as well.
        A[] arr = arrayTest_alloc();
        arrayTest_source1(arr);
        arrayTest_source2(arr);
        arrayTest_sink(arr);
    }

    @AvailableTypes("[Lorg/opalj/fpcf/fixtures/callgraph/xta/StaticMethodFlows$A;")
    public static A[] arrayTest_alloc() {
        // These are not arrays of object types, therefore they should not be tracked.
        int[] foo = new int[10];
        int[][] foo2 = new int[10][10];

        return new A[2];
    }

    @AvailableTypes({
            "org/opalj/fpcf/fixtures/callgraph/xta/StaticMethodFlows$A",
            "[Lorg/opalj/fpcf/fixtures/callgraph/xta/StaticMethodFlows$A;"})
    public static void arrayTest_source1(A[] arr) {
        arr[0] = new A();
    }

    @AvailableTypes({
            "org/opalj/fpcf/fixtures/callgraph/xta/StaticMethodFlows$A1",
            "[Lorg/opalj/fpcf/fixtures/callgraph/xta/StaticMethodFlows$A;"})
    public static void arrayTest_source2(A[] arr) {
        arr[1] = new A1();
    }

    @AvailableTypes({
            "org/opalj/fpcf/fixtures/callgraph/xta/StaticMethodFlows$A",
            "org/opalj/fpcf/fixtures/callgraph/xta/StaticMethodFlows$A1",
            "[Lorg/opalj/fpcf/fixtures/callgraph/xta/StaticMethodFlows$A;"})
    public static void arrayTest_sink(A[] arr) {
        A obj = arr[0];
    }

    // === Array test 2 ===
    // Here, two arrays of the same type flow into the sink separately. The result should be the same as
    // above.
    @AvailableTypes({
            "[Lorg/opalj/fpcf/fixtures/callgraph/xta/StaticMethodFlows$B;",
            "[Lorg/opalj/fpcf/fixtures/callgraph/xta/StaticMethodFlows$B;"})
    public static void arrayTest2() {
        B[] arr = new B[2];
        B[] arr2 = new B[2];
        arrayTest2_source1(arr);
        arrayTest2_sink(arr);
        arrayTest2_source2(arr2);
        arrayTest2_sink(arr2);
    }

    @AvailableTypes({
            "org/opalj/fpcf/fixtures/callgraph/xta/StaticMethodFlows$B",
            "[Lorg/opalj/fpcf/fixtures/callgraph/xta/StaticMethodFlows$B;"})
    public static void arrayTest2_source1(B[] arr) {
        arr[0] = new B();
    }

    @AvailableTypes({
            "org/opalj/fpcf/fixtures/callgraph/xta/StaticMethodFlows$B1",
            "[Lorg/opalj/fpcf/fixtures/callgraph/xta/StaticMethodFlows$B;"})
    public static void arrayTest2_source2(B[] arr) {
        arr[1] = new B1();
    }

    @AvailableTypes({
            "org/opalj/fpcf/fixtures/callgraph/xta/StaticMethodFlows$B",
            "org/opalj/fpcf/fixtures/callgraph/xta/StaticMethodFlows$B1",
            "[Lorg/opalj/fpcf/fixtures/callgraph/xta/StaticMethodFlows$B;"})
    public static void arrayTest2_sink(B[] arr) {
        B obj = arr[0];
    }

    // === Array test 3 ===
    // Here, two differently typed arrays flow into the sink.
    @AvailableTypes({
            "[Lorg/opalj/fpcf/fixtures/callgraph/xta/StaticMethodFlows$C;",
            "[Lorg/opalj/fpcf/fixtures/callgraph/xta/StaticMethodFlows$D;"})
    public static void arrayTest3() {
        C[] arr = new C[2];
        D[] arr2 = new D[2];
        arrayTest3_source1(arr);
        arrayTest3_sink(arr);
        arrayTest3_source2(arr2);
        arrayTest3_sink(arr2);
    }

    @AvailableTypes({
            "org/opalj/fpcf/fixtures/callgraph/xta/StaticMethodFlows$C",
            "[Lorg/opalj/fpcf/fixtures/callgraph/xta/StaticMethodFlows$C;"})
    public static void arrayTest3_source1(C[] arr) {
        arr[0] = new C();
    }

    @AvailableTypes({
            "org/opalj/fpcf/fixtures/callgraph/xta/StaticMethodFlows$D",
            "[Lorg/opalj/fpcf/fixtures/callgraph/xta/StaticMethodFlows$D;"})
    public static void arrayTest3_source2(D[] arr) {
        arr[0] = new D();
    }

    @AvailableTypes({
            "org/opalj/fpcf/fixtures/callgraph/xta/StaticMethodFlows$C",
            "org/opalj/fpcf/fixtures/callgraph/xta/StaticMethodFlows$D",
            "[Lorg/opalj/fpcf/fixtures/callgraph/xta/StaticMethodFlows$C;",
            "[Lorg/opalj/fpcf/fixtures/callgraph/xta/StaticMethodFlows$D;"})
    public static void arrayTest3_sink(Object[] arr) {
        Object obj = arr[0];
    }

    // === Multidimensional array test ===
    // In the main method, an object E is written to the array. In the source method,
    // an object E1 is written to the sub-array. In the sink, both E and E1 should
    // appear.
    @AvailableTypes({
            "org/opalj/fpcf/fixtures/callgraph/xta/StaticMethodFlows$E",
            // This is a slight inaccuracy: E1 is added to [E1 in the *_source method.
            // Since [E1 is available here and the method reads from arrays, E1
            // is added to this method as well.
            "org/opalj/fpcf/fixtures/callgraph/xta/StaticMethodFlows$E1",
            "[Lorg/opalj/fpcf/fixtures/callgraph/xta/StaticMethodFlows$E;",
            "[[Lorg/opalj/fpcf/fixtures/callgraph/xta/StaticMethodFlows$E;",
            "[[[Lorg/opalj/fpcf/fixtures/callgraph/xta/StaticMethodFlows$E;"})
    public static void multiDimensionalArrayTest() {
        E obj = new E();
        E[][][] arr = new E[1][1][1];
        multiDimensionalArrayTest_source(arr[0][0]);
        arr[0][0][0] = obj;
        multiDimensionalArrayTest_sink(arr[0]);
    }

    @AvailableTypes({
            "org/opalj/fpcf/fixtures/callgraph/xta/StaticMethodFlows$E1",
            "[Lorg/opalj/fpcf/fixtures/callgraph/xta/StaticMethodFlows$E;"})
    public static void multiDimensionalArrayTest_source(E[] arr) {
        arr[0] = new E1();
    }

    @AvailableTypes({
            "org/opalj/fpcf/fixtures/callgraph/xta/StaticMethodFlows$E",
            "org/opalj/fpcf/fixtures/callgraph/xta/StaticMethodFlows$E1",
            "[Lorg/opalj/fpcf/fixtures/callgraph/xta/StaticMethodFlows$E;",
            "[[Lorg/opalj/fpcf/fixtures/callgraph/xta/StaticMethodFlows$E;"})
    public static void multiDimensionalArrayTest_sink(E[][] arr) {
        E obj = arr[0][0];
    }

    // === Recursive methods ===

    @AvailableTypes("org/opalj/fpcf/fixtures/callgraph/xta/StaticMethodFlows$A")
    public static void recursionTest() {
        recursiveMethod(new A());
    }

    @AvailableTypes("org/opalj/fpcf/fixtures/callgraph/xta/StaticMethodFlows$A")
    public static void recursiveMethod(A a) {
        // This will not terminate obviously, but that shouldn't matter for the static analysis.
        recursiveMethod(a);
    }

    // === External world ===
    // This test handles cases where type flows to/from methods/fields which are not available in
    // the current analysis context. ArrayList is standard library code, which is not available
    // for property tests (analysis of incomplete projects).

    @AvailableTypes({
            "java/util/ArrayList",
            "org/opalj/fpcf/fixtures/callgraph/xta/StaticMethodFlows$A"})
    public static void externalWorld() {
        ArrayList<A> list = new ArrayList<>();
        list.add(new A());
        // TODO AB If the formal parameter has type List<A> instead, ArrayList does NOT flow to the sink
        // method, since OPAL does not know List is a supertype of ArrayList. Can this be fixed somehow?
        // Even if withRT = true, it does not work.
        externalWorld_sink(list);
    }

    @AvailableTypes({
            "java/util/ArrayList",
            "org/opalj/fpcf/fixtures/callgraph/xta/StaticMethodFlows$A"})
    public static void externalWorld_sink(ArrayList<A> list) {
        A obj = list.get(0);
    }

    // === Test Class Hierarchies ===

    private static class A {}
    private static class A1 extends A {}

    private static class B {}
    private static class B1 extends B {}

    private static class C {}
    private static class C1 extends C {}

    private static class D {}
    private static class D1 extends D {}

    private static class E {}
    private static class E1 extends E {}
}
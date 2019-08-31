/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.callgraph.xta;

import org.opalj.fpcf.properties.callgraph.AvailableTypes;

import java.util.*;

/**
 * Testing type data flow between methods for XTA.
 *
 * @author Andreas Bauer
 */
public class StaticMethodFlows {

    // Any variable passed to this will not be understood as dead code during TAC creation, and will
    // thus remain intact in the TAC.
    public static void sink(Object obj) {}

    // The main enty point. The annotation ensures that no values flow back from the called methods
    // since the return type is void. Since it is a main method, string array is an available type
    // by default.
    @AvailableTypes({"[Ljava/lang/String;"})
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
        externalWorldField();
        externalTypeFilter();
        returnOptimization();
        genericContainer();
        throwableInstantiation();
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
        sink(obj);
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
        sink(obj2);
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
        sink(obj);
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
        sink(obj);
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
        sink(obj);
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
        sink(obj);
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
            "org/opalj/fpcf/fixtures/callgraph/xta/StaticMethodFlows$D"})
    public static void externalWorld() {
        ArrayList<D> list = new ArrayList<>();
        list.add(new D());
        externalWorld_sink(list);
    }

    @AvailableTypes({
            "java/util/ArrayList",
            "org/opalj/fpcf/fixtures/callgraph/xta/StaticMethodFlows$D"})
    public static void externalWorld_sink(ArrayList<D> list) {
        // No call to sink required here, TAC does not optimize away the unused variable.
        D obj = list.get(0);
    }

    // === Field in external world ===

    @AvailableTypes({
            "java/awt/Event",
            "org/opalj/fpcf/fixtures/callgraph/xta/StaticMethodFlows$C"
    })
    public static void externalWorldField() {
        java.awt.Event e = new java.awt.Event(null, 0, null);
        e.arg = new C();
        externalWorldField_sink(e);
    }

    @AvailableTypes({
            "java/awt/Event",
            "org/opalj/fpcf/fixtures/callgraph/xta/StaticMethodFlows$C"
    })
    public static void externalWorldField_sink(java.awt.Event e) {
        C obj = (C)e.arg;
        sink(obj);
    }

    // === External type filter ===
    // For external types (i.e., types which are not part of the analysis project), we only have limited amount of
    // information regarding their type hierarchy. In this test, we consider two sinks with parameters of external
    // types ArrayList and Map.

    static class MyList extends ArrayList { }

    @AvailableTypes({
            "org/opalj/fpcf/fixtures/callgraph/xta/StaticMethodFlows$A",
            "org/opalj/fpcf/fixtures/callgraph/xta/StaticMethodFlows$MyList",
            "java/util/LinkedList"})
    private static void externalTypeFilter() {
        // A is an internal type for which we know it does not extend Map or ArrayList. A should not be propagated
        // to either sink.
        sink(new A());
        // MyList is an internal type for which we know it extends ArrayList, but it is unknown whether or not it
        // extends Map (since we do not know the relationship between ArrayList and Map). It should propagate to
        // both sinks.
        sink(new MyList());
        // LinkedList is an external type for which it is unknwon whether it extends Map or ArrayList.
        // It should propagate to both sinks.
        sink(new LinkedList());

        externalTypeFilter_sink(null);
        externalTypeFilter_sink2(null);
    }

    @AvailableTypes({
            "org/opalj/fpcf/fixtures/callgraph/xta/StaticMethodFlows$MyList",
            "java/util/LinkedList"})
    private static void externalTypeFilter_sink(ArrayList list) { }

    @AvailableTypes({
            "org/opalj/fpcf/fixtures/callgraph/xta/StaticMethodFlows$MyList",
            "java/util/LinkedList"})
    private static void externalTypeFilter_sink2(Map map) { }

    // === Return optimization ===
    // Even though the source method here has a non-void return type, the analysis should not consider the call
    // for backward flows since the caller discards the returned value.
    // "returnOptimization2" is a version which contains one call which discards the value and one call which doesn't,
    // making sure that the backward flow still applies in these cases.

    @AvailableTypes
    public static void returnOptimization() {
        returnOptimization1();
        returnOptimization2();
    }

    @AvailableTypes()
    public static void returnOptimization1() {
        returnOptimization_source();
    }

    @AvailableTypes("org/opalj/fpcf/fixtures/callgraph/xta/StaticMethodFlows$A")
    public static void returnOptimization2() {
        returnOptimization_source();
        A obj = returnOptimization_source();
        sink(obj);
    }

    @AvailableTypes("org/opalj/fpcf/fixtures/callgraph/xta/StaticMethodFlows$A")
    public static A returnOptimization_source() {
        return new A();
    }

    // === Generic container ===
    // This test makes sure that flows back from generic container data structures are handled
    // precisely. Due to type erasure, the return type of the Container's "get" method is Object,
    // which means that any value written to a Container of any generic type will flow back through
    // a "get" call on a Container, which is not precise.
    // However, the compiler inserts a "checkcast" instruction at each call site of such generic
    // methods. The analysis can make use of this to improve precision.

    private static class Container<T> {
        private T value;
        Container(T val) { value = val; }
        public T get() { return value; }
    }

    @AvailableTypes({
            "org/opalj/fpcf/fixtures/callgraph/xta/StaticMethodFlows$A",
            "org/opalj/fpcf/fixtures/callgraph/xta/StaticMethodFlows$B",
            "org/opalj/fpcf/fixtures/callgraph/xta/StaticMethodFlows$C",
            "org/opalj/fpcf/fixtures/callgraph/xta/StaticMethodFlows$Container"})
    public static void genericContainer() {
        Container<A> a = new Container<>(new A());
        Container<B> b = new Container<>(new B());
        Container<C> c = new Container<>(new C());
        genericContainer_sink(a);
        genericContainer_sink2(a, b);
        genericContainer_sink3(c);
    }

    @AvailableTypes({
            "org/opalj/fpcf/fixtures/callgraph/xta/StaticMethodFlows$A",
            "org/opalj/fpcf/fixtures/callgraph/xta/StaticMethodFlows$Container"})
    public static void genericContainer_sink(Container<A> a) {
        A obj = a.get();
        sink(obj);
    }

    @AvailableTypes({
            "org/opalj/fpcf/fixtures/callgraph/xta/StaticMethodFlows$A",
            "org/opalj/fpcf/fixtures/callgraph/xta/StaticMethodFlows$B",
            "org/opalj/fpcf/fixtures/callgraph/xta/StaticMethodFlows$Container"})
    public static void genericContainer_sink2(Container<A> a, Container<B> b) {
        A obj1 = a.get();
        B obj2 = b.get();
        sink(obj1);
        sink(obj2);
    }

    @AvailableTypes({
            "org/opalj/fpcf/fixtures/callgraph/xta/StaticMethodFlows$A",
            "org/opalj/fpcf/fixtures/callgraph/xta/StaticMethodFlows$B",
            "org/opalj/fpcf/fixtures/callgraph/xta/StaticMethodFlows$C",
            "org/opalj/fpcf/fixtures/callgraph/xta/StaticMethodFlows$Container"})
    public static void genericContainer_sink3(Container c) {
        // This method's parameter is a raw type.
        Object obj = c.get();
        sink(obj);
    }

    // === Throwable instantiation ===
    // Subtypes of Throwable are tracked globally. Thus, even though they are instantiated
    // locally, the type is added to the global type set instead of the method's type set.
    // This is due to the fact that exception handling in Java causes non-standard,
    // hard to track flows of data.

    @AvailableTypes // Empty set. "Exception" is added to the global type set instead.
    private static void throwableInstantiation() {
        Exception e = new Exception();
        sink(e);
    }

    // === Test Class Hierarchies ===

    private static class A {}
    private static class A1 extends A {}

    private static class B {}
    private static class B1 extends B {}

    final private static class C {}

    private static class D {}

    private static class E {}
    private static class E1 extends E {}
}
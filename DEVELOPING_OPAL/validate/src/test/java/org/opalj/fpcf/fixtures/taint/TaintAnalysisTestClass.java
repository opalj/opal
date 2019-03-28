/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.taint;

import org.opalj.fpcf.properties.taint.FlowPath;

/**
 * @author Mario Trageser
 */
public class TaintAnalysisTestClass {

    public static void main(String[] args) {
        new TaintAnalysisTestClass().run();
    }

    public void run() {
        callChain();
        instanceSource();
        privateSource();
        twoPaths();
        callRecursiveFunction();
        passToCatch();
        binaryExpression();
        unaryExpression();
        arrayLength();
        taintedArrayIndex();
        wholeArrayTainted();
        passTaintedArrayElement();
        taintArrayAfterCall();
        taintArrayElementAfterCall();
        untaintedArrayIndex();
        reassignArrayElement();
        passUntaintedArrayElement();
        doNotTaintAllArrayElementsAfterCall();
    }

    @FlowPath({"callChain", "passToSink"})
    public void callChain() {
        int i = source();
        passToSink(i);
    }
    @FlowPath({"instanceSource"})
    public void instanceSource() {
        int i = callSourceNonStatic();
        sink(i);
    }
    @FlowPath({"privateSource"})
    public void privateSource() {
        int i = callSourcePrivate();
        sink(i);
    }

    @FlowPath({"twoPaths", "indirectPassToSink", "passToSink"})
    public void twoPaths() {
        int i = source();
        passToSink(i);
        indirectPassToSink(i);
    }

    @FlowPath({"callRecursiveFunction"})
    public void callRecursiveFunction() {
        sink(recursion(0));
    }

    public int recursion(int i) {
        return i == 0 ? recursion(source()) : i;
    }

    @FlowPath({"passToCatch"})
    public void passToCatch() {
        int i = source();
        try {
            throw new RuntimeException();
        } catch(RuntimeException e) {
            sink(i);
        }
    }

    @FlowPath({"unaryExpression"})
    public void unaryExpression() {
        int i = source();
        int j = -i;
        sink(j);
    }

    @FlowPath({"binaryExpression"})
    public void binaryExpression() {
        int i = source();
        int j = i + 1;
        sink(j);
    }

    @FlowPath({"arrayLength"})
    public void arrayLength() {
        int i = source();
        Object[] arr = new Object[i];
        int j = arr.length;
        sink(j);
    }

    @FlowPath({"taintedArrayIndex"})
    public void taintedArrayIndex() {
        int[] arr = new int[2];
        arr[0] = source();
        sink(arr[0]);
    }

    @FlowPath({"wholeArrayTainted"})
    public void wholeArrayTainted() {
        int[] arr = new int[2];
        arr[(int) Math.random() * 2] = source();
        sink(arr[0]);
    }

    @FlowPath({"passTaintedArrayElement", "passFirstArrayElementToSink"})
    public void passTaintedArrayElement() {
        int[] arr = new int[2];
        arr[0] = source();
        passFirstArrayElementToSink(arr);
    }

    @FlowPath({"taintArrayAfterCall", "passFirstArrayElementToSink"})
    public void taintArrayAfterCall() {
        int[] arr = new int[2];
        taintRandomElement(arr);
        passFirstArrayElementToSink(arr);
    }

    @FlowPath({"taintArrayElementAfterCall", "passFirstArrayElementToSink"})
    public void taintArrayElementAfterCall() {
        int[] arr = new int[2];
        taintRandomElement(arr);
        passFirstArrayElementToSink(arr);
    }

    @FlowPath({})
    public void untaintedArrayIndex() {
        int[] arr = new int[2];
        arr[0] = source();
        sink(arr[1]);
    }

    @FlowPath({})
    public void reassignArrayElement() {
        int[] arr = new int[2];
        arr[0] = source();
        arr[0] = 0;
        sink(arr[0]);
    }

    @FlowPath({})
    public void passUntaintedArrayElement() {
        int[] arr = new int[2];
        arr[1] = source();
        passFirstArrayElementToSink(arr);
    }

    @FlowPath({})
    public void doNotTaintAllArrayElementsAfterCall() {
        int[] arr = new int[2];
        taintFirstElement(arr);
        sink(arr[1]);
    }

    public int callSourceNonStatic() {
        return source();
    }

    private int callSourcePrivate() {
        return source();
    }

    public void passToSink(int i) {
        sink(i);
    }

    public void indirectPassToSink(int i) {
        passToSink(i);
    }

    public void passFirstArrayElementToSink(int[] arr) {
        sink(arr[0]);
    }

    public void taintRandomElement(int[] arr) {
        arr[Math.random() < .5 ? 0 : 1] = source();
    }

    public void taintFirstElement(int[] arr) {
        arr[0] = source();
    }

    public static int source() {
        return 1;
    }

    public static void sink(int i) {
        System.out.println(i);
    }

}

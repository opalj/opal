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
        callChainsAreConsidered();
        returnEdgesFromInstanceMethodsArePresent();
        returnEdgesFromPrivateMethodsArePresent();
        multiplePathsAreConsidered_1();
        multiplePathsAreConsidered_2();
        summaryEdgesOfRecursiveFunctionsAreComputedCorrectly();
        codeInCatchNodesIsConsidered();
        binaryExpressionsPropagateTaints();
        unaryExpressionsPropagateTaints();
        arrayLengthPropagatesTaints();
        singleArrayIndicesAreTainted_1();
        wholeArrayTaintedIfIndexUnknown();
        arrayElementTaintsArePropagatedToCallee_1();
        arrayElementTaintsArePropagatedBack_1();
        callerParameterIsTaintedIfCalleeTaintsFormalParameter();
        singleArrayIndicesAreTainted_2();
        taintDisappearsWhenReassigning();
        arrayElementTaintsArePropagatedToCallee_2();
        arrayElementTaintsArePropagatedBack_2();
    }

    @FlowPath({"callChainsAreConsidered", "passToSink"})
    public void callChainsAreConsidered() {
        int i = source();
        passToSink(i);
    }
    @FlowPath({"returnEdgesFromInstanceMethodsArePresent"})
    public void returnEdgesFromInstanceMethodsArePresent() {
        int i = callSourceNonStatic();
        sink(i);
    }
    @FlowPath({"returnEdgesFromPrivateMethodsArePresent"})
    public void returnEdgesFromPrivateMethodsArePresent() {
        int i = callSourcePrivate();
        sink(i);
    }

    @FlowPath({"multiplePathsAreConsidered_1", "indirectPassToSink", "passToSink"})
    public void multiplePathsAreConsidered_1() {
        int i = source();
        passToSink(i);
        indirectPassToSink(i);
    }

    @FlowPath({"multiplePathsAreConsidered_2", "passToSink"})
    public void multiplePathsAreConsidered_2() {
        int i = source();
        passToSink(i);
        indirectPassToSink(i);
    }

    @FlowPath({"summaryEdgesOfRecursiveFunctionsAreComputedCorrectly"})
    public void summaryEdgesOfRecursiveFunctionsAreComputedCorrectly() {
        sink(recursion(0));
    }

    public int recursion(int i) {
        return i == 0 ? recursion(source()) : i;
    }

    @FlowPath({"codeInCatchNodesIsConsidered"})
    public void codeInCatchNodesIsConsidered() {
        int i = source();
        try {
            throw new RuntimeException();
        } catch(RuntimeException e) {
            sink(i);
        }
    }

    @FlowPath({"unaryExpressionsPropagateTaints"})
    public void unaryExpressionsPropagateTaints() {
        int i = source();
        int j = -i;
        sink(j);
    }

    @FlowPath({"binaryExpressionsPropagateTaints"})
    public void binaryExpressionsPropagateTaints() {
        int i = source();
        int j = i + 1;
        sink(j);
    }

    @FlowPath({"arrayLengthPropagatesTaints"})
    public void arrayLengthPropagatesTaints() {
        int i = source();
        Object[] arr = new Object[i];
        int j = arr.length;
        sink(j);
    }

    @FlowPath({"singleArrayIndicesAreTainted_1"})
    public void singleArrayIndicesAreTainted_1() {
        int[] arr = new int[2];
        arr[0] = source();
        sink(arr[0]);
    }

    @FlowPath({})
    public void singleArrayIndicesAreTainted_2() {
        int[] arr = new int[2];
        arr[0] = source();
        sink(arr[1]);
    }

    @FlowPath({"wholeArrayTaintedIfIndexUnknown"})
    public void wholeArrayTaintedIfIndexUnknown() {
        int[] arr = new int[2];
        arr[(int) Math.random() * 2] = source();
        sink(arr[0]);
    }

    @FlowPath({"arrayElementTaintsArePropagatedToCallee_1", "passFirstArrayElementToSink"})
    public void arrayElementTaintsArePropagatedToCallee_1() {
        int[] arr = new int[2];
        arr[0] = source();
        passFirstArrayElementToSink(arr);
    }

    @FlowPath({})
    public void arrayElementTaintsArePropagatedToCallee_2() {
        int[] arr = new int[2];
        arr[1] = source();
        passFirstArrayElementToSink(arr);
    }

    @FlowPath({"arrayElementTaintsArePropagatedBack_1", "passFirstArrayElementToSink"})
    public void arrayElementTaintsArePropagatedBack_1() {
        int[] arr = new int[2];
        taintRandomElement(arr);
        passFirstArrayElementToSink(arr);
    }

    @FlowPath({})
    public void arrayElementTaintsArePropagatedBack_2() {
        int[] arr = new int[2];
        taintFirstElement(arr);
        sink(arr[1]);
    }

    @FlowPath({"callerParameterIsTaintedIfCalleeTaintsFormalParameter", "passFirstArrayElementToSink"})
    public void callerParameterIsTaintedIfCalleeTaintsFormalParameter() {
        int[] arr = new int[2];
        taintRandomElement(arr);
        passFirstArrayElementToSink(arr);
    }

    @FlowPath({})
    public void taintDisappearsWhenReassigning() {
        int[] arr = new int[2];
        arr[0] = source();
        arr[0] = 0;
        sink(arr[0]);
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

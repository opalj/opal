/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.taint;

import org.opalj.fpcf.properties.taint.FlowPath;

/**
 * @author Mario Trageser
 */
public class TaintAnalysisTestClass {

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

    @FlowPath({"ifEdgesAreConsidered"})
    public void ifEdgesAreConsidered() {
        int i;
        if(Math.random() < .5) {
            i = source();
        } else {
            i = 0;
        }
        sink(i);
    }

    @FlowPath({"elseEdgesAreConsidered"})
    public void elseEdgesAreConsidered() {
        int i;
        if(Math.random() < .5) {
            i = 0;
        } else {
            i = source();
        }
        sink(i);
    }

    @FlowPath({"forLoopsAreConsidered"})
    public void forLoopsAreConsidered() {
        int[] arr = new int[2];
        for(int i = 0; i < arr.length; i++) {
            sink(arr[0]);
            arr[i] = source();
        }
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

    @FlowPath({"codeInFinallyNodesIsConsidered"})
    public void codeInFinallyNodesIsConsidered() {
        int i = 1;
        try {
            throw new RuntimeException();
        } catch(RuntimeException e) {
            i = source();
        } finally {
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

    //Does not work, because the TAC's exit statements do not have any predecessors in this case.
    /*@FlowPath({"doesNotReturn", "callerParameterIsTaintedIfCalleeTaintsFormalParameter", "passFirstArrayElementToSink"})
    public void doesNotReturn() {
        while(true) {
            try {
                callerParameterIsTaintedIfCalleeTaintsFormalParameter();
            }catch (Throwable t) {
                //does nothing by purpose
            }
        }
    }*/

    //Does not work, because we do not know which exceptions cannot be thrown.
    /*@FlowPath({})
    public void onlyThrowableExceptionsAreConsidered() {
        int i = 0;
        try {
            divide(1, i);
        } catch(IllegalArgumentException e) {
            i = source();
        }
        sink(i);
    }*/

    public int callSourceNonStatic() {
        return source();
    }

    private int callSourcePrivate() {
        return source();
    }

    public static void passToSink(int i) {
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

    //If it throws an exception, it is only an arithmetic exception.
    public static int divide(int i, int j) {
        return i / j;
    }

}

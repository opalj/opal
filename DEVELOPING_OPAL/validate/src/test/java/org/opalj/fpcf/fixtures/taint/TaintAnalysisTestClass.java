/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.taint;

import org.opalj.fpcf.properties.taint.BackwardFlowPath;
import org.opalj.fpcf.properties.taint.ForwardFlowPath;

/**
 * @author Mario Trageser
 */
public class TaintAnalysisTestClass {

    private static int staticField;

    private int instanceField;

    @ForwardFlowPath({"callChainsAreConsidered", "passToSink"})
    @BackwardFlowPath({"callChainsAreConsidered", "passToSink", "sink"})
    public void callChainsAreConsidered() {
        passToSink(source());
    }

    @ForwardFlowPath({"returnEdgesFromInstanceMethodsArePresent"})
    @BackwardFlowPath({"returnEdgesFromInstanceMethodsArePresent", "sink"})
    public void returnEdgesFromInstanceMethodsArePresent() {
        sink(callSourcePublic());
    }

    @ForwardFlowPath({"returnEdgesFromPrivateMethodsArePresent"})
    @BackwardFlowPath({"returnEdgesFromPrivateMethodsArePresent", "sink"})
    public void returnEdgesFromPrivateMethodsArePresent() {
        sink(callSourceNonStatic());
    }

    @ForwardFlowPath({"multiplePathsAreConsidered_1", "indirectPassToSink", "passToSink"})
    @BackwardFlowPath({"multiplePathsAreConsidered_1", "indirectPassToSink", "passToSink", "sink"})
    public void multiplePathsAreConsidered_1() {
        int i = source();
        passToSink(i);
        indirectPassToSink(i);
    }

    @ForwardFlowPath({"multiplePathsAreConsidered_2", "passToSink"})
    @BackwardFlowPath({"multiplePathsAreConsidered_2", "passToSink", "sink"})
    public void multiplePathsAreConsidered_2() {
        int i = source();
        passToSink(i);
        indirectPassToSink(i);
    }

    @ForwardFlowPath({"ifEdgesAreConsidered"})
    @BackwardFlowPath({"ifEdgesAreConsidered", "sink"})
    public void ifEdgesAreConsidered() {
        int i;
        if(Math.random() < .5) {
            i = source();
        } else {
            i = 0;
        }
        sink(i);
    }

    @ForwardFlowPath({"elseEdgesAreConsidered"})
    @BackwardFlowPath({"elseEdgesAreConsidered", "sink"})
    public void elseEdgesAreConsidered() {
        int i;
        if(Math.random() < .5) {
            i = 0;
        } else {
            i = source();
        }
        sink(i);
    }

    @ForwardFlowPath({"forLoopsAreConsidered"})
    @BackwardFlowPath({"forLoopsAreConsidered", "sink"})
    public void forLoopsAreConsidered() {
        int[] arr = new int[2];
        for(int i = 0; i < arr.length; i++) {
            sink(arr[0]);
            arr[i] = source();
        }
    }

    @ForwardFlowPath("returnOfIdentityFunctionIsConsidered")
    @BackwardFlowPath({"returnOfIdentityFunctionIsConsidered", "sink"})
    public void returnOfIdentityFunctionIsConsidered() {
        sink(identity(source()));
    }

    @ForwardFlowPath({"summaryEdgesOfRecursiveFunctionsAreComputedCorrectly"})
    @BackwardFlowPath({"summaryEdgesOfRecursiveFunctionsAreComputedCorrectly", "sink"})
    public void summaryEdgesOfRecursiveFunctionsAreComputedCorrectly() {
        sink(recursion(0));
    }

    public int recursion(int i) {
        return i == 0 ? recursion(source()) : i;
    }

    @ForwardFlowPath({"codeInCatchNodesIsConsidered"})
    @BackwardFlowPath({"codeInCatchNodesIsConsidered", "sink"})
    public void codeInCatchNodesIsConsidered() {
        int i = source();
        try {
            throw new RuntimeException();
        } catch(RuntimeException e) {
            sink(i);
        }
    }

    @ForwardFlowPath({"codeInFinallyNodesIsConsidered"})
    @BackwardFlowPath({"codeInFinallyNodesIsConsidered", "sink"})
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

    @ForwardFlowPath({"unaryExpressionsPropagateTaints"})
    @BackwardFlowPath({"unaryExpressionsPropagateTaints", "sink"})
    public void unaryExpressionsPropagateTaints() {
        sink(-source());
    }

    @ForwardFlowPath({"binaryExpressionsPropagateTaints"})
    @BackwardFlowPath({"binaryExpressionsPropagateTaints", "sink"})
    public void binaryExpressionsPropagateTaints() {
        sink(source() + 1);
    }

    @ForwardFlowPath({"arrayLengthPropagatesTaints"})
    @BackwardFlowPath({"arrayLengthPropagatesTaints", "sink"})
    public void arrayLengthPropagatesTaints() {
        sink(new Object[source()].length);
    }

    @ForwardFlowPath({"singleArrayIndicesAreTainted_1"})
    @BackwardFlowPath({"singleArrayIndicesAreTainted_1", "sink"})
    public void singleArrayIndicesAreTainted_1() {
        int[] arr = new int[2];
        arr[0] = source();
        sink(arr[0]);
    }

    @ForwardFlowPath({})
    @BackwardFlowPath({})
    public void singleArrayIndicesAreTainted_2() {
        int[] arr = new int[2];
        arr[0] = source();
        sink(arr[1]);
    }

    @ForwardFlowPath({"wholeArrayTaintedIfIndexUnknown"})
    @BackwardFlowPath({"wholeArrayTaintedIfIndexUnknown", "sink"})
    public void wholeArrayTaintedIfIndexUnknown() {
        int[] arr = new int[2];
        arr[(int) (Math.random() * 2)] = source();
        sink(arr[0]);
    }

    @ForwardFlowPath({"arrayElementTaintsArePropagatedToCallee_1", "passFirstArrayElementToSink"})
    @BackwardFlowPath({"arrayElementTaintsArePropagatedToCallee_1", "passFirstArrayElementToSink",
            "sink"})
    public void arrayElementTaintsArePropagatedToCallee_1() {
        int[] arr = new int[2];
        arr[0] = source();
        passFirstArrayElementToSink(arr);
    }

    @ForwardFlowPath({})
    @BackwardFlowPath({})
    public void arrayElementTaintsArePropagatedToCallee_2() {
        int[] arr = new int[2];
        arr[1] = source();
        passFirstArrayElementToSink(arr);
    }

    @ForwardFlowPath({"arrayElementTaintsArePropagatedBack_1", "passFirstArrayElementToSink"})
    @BackwardFlowPath({"arrayElementTaintsArePropagatedBack_1", "passFirstArrayElementToSink",
            "sink"})
    public void arrayElementTaintsArePropagatedBack_1() {
        int[] arr = new int[2];
        taintRandomElement(arr);
        passFirstArrayElementToSink(arr);
    }

    @ForwardFlowPath({})
    @BackwardFlowPath({})
    public void arrayElementTaintsArePropagatedBack_2() {
        int[] arr = new int[2];
        taintFirstElement(arr);
        sink(arr[1]);
    }

    @ForwardFlowPath({"callerParameterIsTaintedIfCalleeTaintsFormalParameter",
            "passFirstArrayElementToSink"})
    @BackwardFlowPath({"callerParameterIsTaintedIfCalleeTaintsFormalParameter",
            "passFirstArrayElementToSink", "sink"})
    public void callerParameterIsTaintedIfCalleeTaintsFormalParameter() {
        int[] arr = new int[2];
        taintRandomElement(arr);
        passFirstArrayElementToSink(arr);
    }

    @ForwardFlowPath({})
    @BackwardFlowPath({})
    public void taintDisappearsWhenReassigning() {
        int[] arr = new int[2];
        arr[0] = source();
        arr[0] = 0;
        sink(arr[0]);
    }

    @ForwardFlowPath({})
    @BackwardFlowPath({})
    public void nativeMethodsCanBeHandeled() {
        int j = nativeMethod(0);
        sink(j);
    }

    @ForwardFlowPath({"returnValueOfNativeMethodIsTainted"})
    @BackwardFlowPath({"returnValueOfNativeMethodIsTainted", "sink"})
    public void returnValueOfNativeMethodIsTainted() {
        sink(nativeMethod(source()));
    }

    @ForwardFlowPath({"analysisUsesCallGraph_1"})
    @BackwardFlowPath({"analysisUsesCallGraph_1", "sink"})
    public void analysisUsesCallGraph_1() {
        A a = new B();
        sink(a.get());
    }

    @ForwardFlowPath({})
    @BackwardFlowPath({})
    public void analysisUsesCallGraph_2() {
        A a = new C();
        sink(a.get());
    }

    @ForwardFlowPath({"analysisUsesCallGraph_3"})
    @BackwardFlowPath({"analysisUsesCallGraph_3", "sink"})
    public void analysisUsesCallGraph_3() {
        A a;
        if(Math.random() < .5)
            a = new B();
        else
            a = new C();
        sink(a.get());
    }

    @ForwardFlowPath({})
    @BackwardFlowPath({})
    public void sanitizeRemovesTaint() {
        sink(sanitize(source()));
    }

    @ForwardFlowPath({"instanceFieldsAreTainted"})
    @BackwardFlowPath({"instanceFieldsAreTainted", "sink"})
    public void instanceFieldsAreTainted() {
        instanceField = source();
        sink(instanceField);
    }

    @ForwardFlowPath({"staticFieldsAreTainted"})
    @BackwardFlowPath({"staticFieldsAreTainted", "sink"})
    public void staticFieldsAreTainted() {
        staticField = source();
        sink(staticField);
    }

    @ForwardFlowPath({"fieldTaintsArePassed", "passWrappedValueToSink"})
    @BackwardFlowPath({"fieldTaintsArePassed", "passWrappedValueToSink", "sink"})
    public void fieldTaintsArePassed() {
        passWrappedValueToSink(new Wrapper(source()));
    }

    @ForwardFlowPath({"fieldTaintsAreAppliedInReturnFlow"})
    @BackwardFlowPath({"fieldTaintsAreAppliedInReturnFlow", "sink"})
    public void fieldTaintsAreAppliedInReturnFlow() {
        sink(createTaintedWrapper().field);
    }

    @ForwardFlowPath({"fieldTaintsOfParametersAreAppliedInReturnFlow"})
    @BackwardFlowPath({"fieldTaintsOfParametersAreAppliedInReturnFlow", "sink"})
    public void fieldTaintsOfParametersAreAppliedInReturnFlow() {
        Wrapper wrapper = new Wrapper();
        taintWrappedValue(wrapper);
        sink(wrapper.field);
    }

    @ForwardFlowPath({"fieldTaintsAreConsideredInComputations"})
    @BackwardFlowPath({"fieldTaintsAreConsideredInComputations", "sink"})
    public void fieldTaintsAreConsideredInComputations() {
        Wrapper wrapper = new Wrapper(source());
        sink(wrapper.field + 1);
    }

    //TODO Tests für statische Felder über Methodengrenzen

    //Does not work, because we do not know which exceptions cannot be thrown.
    /*@ForwardFlowPath({})
    public void onlyThrowableExceptionsAreConsidered() {
        int i = 0;
        try {
            divide(1, i);
        } catch(IllegalArgumentException e) {
            i = source();
        }
        sink(i);
    }*/

    //Does not work, because the analysis does not know that there is only one iteration.
    /*@ForwardFlowPath({})
    public void iterationCountIsConsidered() {
        int[] arr = new int[2];
        for(int i = 0; i < 1; i++) {
            sink(arr[0]);
            arr[i] = source();
        }
    }*/

    public int callSourcePublic() {
        return source();
    }

    private int callSourceNonStatic() {
        return source();
    }

    private static void passToSink(int i) {
        sink(i);
    }

    private void indirectPassToSink(int i) {
        passToSink(i);
    }

    private void passFirstArrayElementToSink(int[] arr) {
        sink(arr[0]);
    }

    private void taintRandomElement(int[] arr) {
        arr[Math.random() < .5 ? 0 : 1] = source();
    }

    private void taintFirstElement(int[] arr) {
        arr[0] = source();
    }

    private native int nativeMethod(int i);

    private int identity(int i) {return i;}

    private void passWrappedValueToSink(Wrapper wrapper) {
        sink(wrapper.field);
    }

    private Wrapper createTaintedWrapper() {
        return new Wrapper(source());
    }

    private void taintWrappedValue(Wrapper wrapper) {
        wrapper.field = source();
    }

    //If it throws an exception, it is only an arithmetic exception.
    private static int divide(int i, int j) {
        return i / j;
    }

    public static int source() {
        return 1;
    }

    private static int sanitize(int i) {return i;}

    private static void sink(int i) {
        System.out.println(i);
    }

}

abstract class A {
    abstract int get();
}

class B extends A {
    @Override
    int get() {
        return TaintAnalysisTestClass.source();
    }
}

class C extends A {
    @Override
    int get() {
        return 0;
    }
}

class Wrapper {
    public int field;

    Wrapper() {

    }

    Wrapper(int field) {
        this.field = field;
    }
}
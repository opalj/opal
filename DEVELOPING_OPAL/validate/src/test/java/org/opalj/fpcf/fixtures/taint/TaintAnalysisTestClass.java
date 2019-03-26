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
        twoPaths();
        passToCatch();
        binaryExpression();
        unaryExpression();
        arrayLength();
    }

    @FlowPath({"callChain", "passToSink"})
    public void callChain() {
        int i = source();
        passToSink(i);
    }

    @FlowPath({"twoPaths", "indirectPassToSink", "passToSink"})
    public void twoPaths() {
        int i = source();
        passToSink(i);
        indirectPassToSink(i);
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

    public void passToSink(int i) {
        sink(i);
    }

    public void indirectPassToSink(int i) {
        passToSink(i);
    }

    public int source() {
        return 1;
    }

    public void sink(int i) {
        System.out.println(i);
    }

    public void sink(boolean b) {
        System.out.println(b);
    }

}

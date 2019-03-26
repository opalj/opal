/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.taint;

import org.opalj.fpcf.properties.taint.FlowPath;

public class TaintAnalysisTestClass {

    public static void main(String[] args) {
        new TaintAnalysisTestClass().run();
    }

    public void run() {
        callChain();
        twoPaths();
        passToCatch();
    }

    @FlowPath({"callChain", "passToSink"})
    public void callChain() {
        String s = source();
        passToSink(s);
    }

    @FlowPath({"twoPaths", "indirectPassToSink", "passToSink"})
    public void twoPaths() {
        String s = source();
        passToSink(s);
        indirectPassToSink(s);
    }

    @FlowPath({"passToCatch"})
    public void passToCatch() {
        String s = source();
        try {
            throw new RuntimeException();
        } catch(RuntimeException e) {
            sink(s);
        }
    }

    public void passToSink(String s) {
        sink(s);
    }

    public void indirectPassToSink(String s) {
        passToSink(s);
    }

    public String source() {
        return "source";
    }

    public void sink(String data) {
        System.out.println(data);
    }

}

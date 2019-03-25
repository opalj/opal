/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.taint;

import org.opalj.fpcf.properties.taint.FlowPath;

public class TaintAnalysisTestClass {

    public static void main(String[] args) {
        new TaintAnalysisTestClass().run();
    }

    public void run() {
        callChain();
        passInCatch();
    }

    @FlowPath({"callChain", "passToSink"})
    public void callChain() {
        String s = source();
        passToSink(s);
    }

    @FlowPath({"passInCatch"})
    public void passInCatch() {
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

    public String source() {
        return "source";
    }

    public void sink(String data) {
        System.out.println(data);
    }

}

/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.callgraph;

import org.opalj.fpcf.properties.callgraph.DirectCall;
import org.opalj.tac.fpcf.analyses.cg.CHATypeProvider;
import org.opalj.tac.fpcf.analyses.cg.RTATypeProvider;

/**
 * Testing call graph construction with a very simple hierarchy.
 *
 * @author Andreas Bauer
 */
public class SimpleHierarchy {

    @DirectCall(
            name = "<init>",
            line = 21,
            resolvedTargets = "Lorg/opalj/fpcf/fixtures/callgraph/B;"
    )
    public static void main(String[] args) { // The entry point...
        A obj = new B();
        callSite(obj);
    }

    @DirectCall(
            name = "foo",
            line = 38,
            resolvedTargets = {
                    "Lorg/opalj/fpcf/fixtures/callgraph/A;",
                    "Lorg/opalj/fpcf/fixtures/callgraph/B;"},
            analyses = { CHATypeProvider.class})
    @DirectCall(
            name = "foo",
            line = 38,
            resolvedTargets = {"Lorg/opalj/fpcf/fixtures/callgraph/B;"},
            analyses = { RTATypeProvider.class})
    public static void callSite(A obj) {
        obj.foo();
        // if this call site was inside main, TAC would know that the
        // object can only be B, so the full RTA/CHA resolution would
        // not apply
    }
}

class A {
    public void foo() {
        // ...
    }
}

class B extends A {
    public void foo() {
        // ...
    }
}
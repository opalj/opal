package org.opalj.fpcf.fixtures.escape.virtual_calls;

import org.opalj.fpcf.analyses.escape.InterProceduralEscapeAnalysis;
import org.opalj.fpcf.analyses.escape.SimpleEscapeAnalysis;
import org.opalj.fpcf.fixtures.escape.Circle;
import org.opalj.fpcf.properties.escape.EscapeInCallee;
import org.opalj.fpcf.properties.escape.AtMostEscapeInCallee;

public class VirtualCalls {

    public void foo1(Interface i) {
        Circle c = new
                @AtMostEscapeInCallee("the type is extensible and worst escape is via return")
                        Circle();
        i.copyCircle(c);
    }


    public Circle foo2(FinalClassExtendsC x) {
        Circle c = new
                @EscapeInCallee(
                        value = "the type is final and worst escape is via return",
                        analyses = InterProceduralEscapeAnalysis.class)
                @AtMostEscapeInCallee(
                        value = "intra-procedural analyses don't handle this",
                        analyses = SimpleEscapeAnalysis.class)
                        Circle();
        return x.copyCircle(c);
    }
}

package org.opalj.fpcf.fixtures.escape.virtual_calls;

import org.opalj.fpcf.analyses.escape.InterproceduralEscapeAnalysis;
import org.opalj.fpcf.analyses.escape.SimpleEscapeAnalysis;
import org.opalj.fpcf.fixtures.escape.Circle;
import org.opalj.fpcf.properties.escape.MaybeNoEscape;
import org.opalj.fpcf.properties.escape.NoEscape;

public final class FinalClassExtendsC extends ClassCExtendsA {

    @Override
    public Circle cyclicFunction(
            @NoEscape(value = "not used at all", analyses = InterproceduralEscapeAnalysis.class)
            @MaybeNoEscape(value = "Formal parameters are not going to be analyzed",
                    analyses = SimpleEscapeAnalysis.class) Circle aCircle, int count
    ) {
        return new Circle(count);
    }
}

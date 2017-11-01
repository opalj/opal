package org.opalj.fpcf.fixtures.escape.virtual_calls;

import org.opalj.fpcf.analyses.escape.InterproceduralEscapeAnalysis;
import org.opalj.fpcf.analyses.escape.SimpleEscapeAnalysis;
import org.opalj.fpcf.fixtures.escape.Circle;
import org.opalj.fpcf.properties.escape.EscapeInCallee;
import org.opalj.fpcf.properties.escape.MaybeEscapeInCallee;
import org.opalj.fpcf.properties.escape.MaybeEscapeViaReturn;
import org.opalj.fpcf.properties.escape.MaybeNoEscape;

public class ClassA extends AbstractClass {

    @Override
    public Circle copyCircle(
            @EscapeInCallee(value = "escapes in super call",
                    analyses = InterproceduralEscapeAnalysis.class)
            @MaybeNoEscape(value = "Formal parameters are not going to be analyzed",
                    analyses = SimpleEscapeAnalysis.class)
                    Circle aCircle
    ) {
        System.out.println("Copying");
        return super.copyCircle(aCircle);
    }

    @Override
    public Circle cyclicFunction(
            @MaybeEscapeInCallee(value = "the type is extensible",
                    analyses = InterproceduralEscapeAnalysis.class)
            @MaybeNoEscape(value = "Formal parameters are not going to be analyzed",
                    analyses = SimpleEscapeAnalysis.class)
                    Circle aCircle,
            int count
    ) {
        if (count > 0)
            return super.cyclicFunction(aCircle, count - 1);
        else
            return cyclicFunction(aCircle, count - 1);

    }
}

package org.opalj.fpcf.fixtures.escape.virtual_calls;

import org.opalj.fpcf.analyses.escape.InterproceduralEscapeAnalysis;
import org.opalj.fpcf.analyses.escape.SimpleEscapeAnalysis;
import org.opalj.fpcf.fixtures.escape.Circle;
import org.opalj.fpcf.properties.escape.EscapeViaReturn;
import org.opalj.fpcf.properties.escape.MaybeNoEscape;

public class EffectivelyFinalClassExtendsA extends ClassA {

    @Override
    public final Circle copyCircle(
            @EscapeViaReturn(value = "is directly returned",
                    analyses = InterproceduralEscapeAnalysis.class)
            @MaybeNoEscape(value = "Formal parameters are not going to be analyzed",
                    analyses = SimpleEscapeAnalysis.class)
                    Circle aCircle
    ) {
        return aCircle;
    }

}

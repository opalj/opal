/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.escape.virtual_calls;

import org.opalj.fpcf.fixtures.escape.Circle;
import org.opalj.fpcf.properties.escape.AtMostEscapeInCallee;
import org.opalj.fpcf.properties.escape.AtMostNoEscape;
import org.opalj.fpcf.properties.escape.EscapeInCallee;
import org.opalj.tac.fpcf.analyses.escape.InterProceduralEscapeAnalysis;
import org.opalj.tac.fpcf.analyses.escape.SimpleEscapeAnalysis;

public class ClassA extends AbstractClass {

    @Override
    public Circle copyCircle(
            @EscapeInCallee(value = "escapes in super call",
                    analyses = InterProceduralEscapeAnalysis.class)
            @AtMostNoEscape(value = "Formal parameters are not going to be analyzed",
                    analyses = SimpleEscapeAnalysis.class)
                    Circle aCircle
    ) {
        System.out.println("Copying");
        return super.copyCircle(aCircle);
    }

    @Override
    public Circle cyclicFunction(
            @AtMostEscapeInCallee(value = "the type is extensible",
                    analyses = InterProceduralEscapeAnalysis.class)
            @AtMostNoEscape(value = "Formal parameters are not going to be analyzed",
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

/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.escape.virtual_calls;

import org.opalj.fpcf.fixtures.escape.Circle;
import org.opalj.fpcf.properties.escape.AtMostNoEscape;
import org.opalj.fpcf.properties.escape.NoEscape;
import org.opalj.tac.fpcf.analyses.escape.InterProceduralEscapeAnalysis;
import org.opalj.tac.fpcf.analyses.escape.SimpleEscapeAnalysis;

public final class FinalClassExtendsC extends ClassCExtendsA {

    @Override
    public Circle cyclicFunction(
            @NoEscape(value = "not used at all", analyses = InterProceduralEscapeAnalysis.class)
            @AtMostNoEscape(value = "Formal parameters are not going to be analyzed",
                    analyses = SimpleEscapeAnalysis.class) Circle aCircle, int count
    ) {
        return new Circle(count);
    }
}

/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.escape.virtual_calls;

import org.opalj.fpcf.fixtures.escape.Circle;
import org.opalj.fpcf.properties.escape.AtMostNoEscape;
import org.opalj.fpcf.properties.escape.EscapeViaReturn;
import org.opalj.tac.fpcf.analyses.escape.InterProceduralEscapeAnalysis;
import org.opalj.tac.fpcf.analyses.escape.SimpleEscapeAnalysis;

public class EffectivelyFinalClassExtendsA extends ClassA {

    @Override
    public final Circle copyCircle(
            @EscapeViaReturn(value = "is directly returned",
                    analyses = InterProceduralEscapeAnalysis.class)
            @AtMostNoEscape(value = "Formal parameters are not going to be analyzed",
                    analyses = SimpleEscapeAnalysis.class)
                    Circle aCircle
    ) {
        return aCircle;
    }

}

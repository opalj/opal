/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.escape.virtual_calls;

import org.opalj.tac.fpcf.analyses.escape.InterProceduralEscapeAnalysis;
import org.opalj.tac.fpcf.analyses.escape.SimpleEscapeAnalysis;
import org.opalj.fpcf.fixtures.escape.Circle;
import org.opalj.fpcf.properties.escape.*;

public abstract class AbstractClass implements Interface {

    @Override
    public Circle copyCircle(
            @NoEscape(value = "a new circle is created",
                    analyses = InterProceduralEscapeAnalysis.class)
            @AtMostNoEscape(value = "Formal parameters are not going to be analyzed",
                    analyses = SimpleEscapeAnalysis.class)
                    Circle aCircle
    ) {
        return new Circle(aCircle.radius);
    }

    @Override
    public Circle cyclicFunction(
            @AtMostEscapeInCallee(value = "it is passed to an extensible function",
                    analyses = InterProceduralEscapeAnalysis.class)
            @AtMostNoEscape(value = "Formal parameters are not going to be analyzed",
                    analyses = SimpleEscapeAnalysis.class) Circle aCircle,
            int count
    ) {
        if (count == 0) {
            return new @EscapeViaReturn("returned") Circle(aCircle.radius);
        } else if (count < 0) {
            return cyclicFunction(aCircle, -1);
        } else {
            return cyclicFunction(new
                    @AtMostEscapeInCallee("the function is extensible")
                            Circle(), 0);
        }
    }
}

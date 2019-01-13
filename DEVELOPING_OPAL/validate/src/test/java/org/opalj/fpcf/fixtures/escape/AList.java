/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.escape;

import org.opalj.fpcf.properties.escape.AtMostEscapeViaParameter;
import org.opalj.fpcf.properties.escape.AtMostNoEscape;
import org.opalj.fpcf.properties.escape.EscapeViaParameter;
import org.opalj.tac.fpcf.analyses.escape.InterProceduralEscapeAnalysis;
import org.opalj.tac.fpcf.analyses.escape.SimpleEscapeAnalysis;

public class AList {

    private Object[] elements;
    private int numberOfElements = 0;

    public AList(int capacity) {
        elements = new @EscapeViaParameter("the array escapes via the this local") Object[capacity];
    }

    public Object get(int index) {
        return elements[index];
    }

    public void add(
            @AtMostEscapeViaParameter(value = "the parameter escapes via the array",
                    analyses = InterProceduralEscapeAnalysis.class)
            @AtMostNoEscape(value = "SimpleEscapeAnalyis does not track formal parameters",
                    analyses = SimpleEscapeAnalysis.class)
                    Object o
    ) {
        if (numberOfElements >= elements.length) {
            Object[] tmp = elements;
            elements = new
                    @AtMostEscapeViaParameter("the array escapes via the this local")
                            Object[elements.length * 2];
            for (int i = 0; i < numberOfElements; i++) {
                elements[i] = tmp[i];
            }
            elements[numberOfElements] = o;
            numberOfElements++;
        }
        elements[numberOfElements] = o;
        numberOfElements++;

    }

}

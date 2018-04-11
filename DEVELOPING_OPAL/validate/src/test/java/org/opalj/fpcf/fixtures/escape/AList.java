/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2017
 * Software Technology Group
 * Department of Computer Science
 * Technische Universität Darmstadt
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  - Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package org.opalj.fpcf.fixtures.escape;

import org.opalj.fpcf.analyses.escape.InterProceduralEscapeAnalysis;
import org.opalj.fpcf.analyses.escape.SimpleEscapeAnalysis;
import org.opalj.fpcf.properties.escape.EscapeViaParameter;
import org.opalj.fpcf.properties.escape.AtMostEscapeViaParameter;
import org.opalj.fpcf.properties.escape.AtMostNoEscape;

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

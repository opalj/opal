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
package org.opalj.fpcf.fixtures.escape.virtual_calls;

import org.opalj.fpcf.analyses.escape.InterProceduralEscapeAnalysis;
import org.opalj.fpcf.analyses.escape.SimpleEscapeAnalysis;
import org.opalj.fpcf.fixtures.escape.Circle;
import org.opalj.fpcf.properties.escape.AtMostNoEscape;
import org.opalj.fpcf.properties.escape.EscapeInCallee;
import org.opalj.fpcf.properties.escape.AtMostEscapeInCallee;
import org.opalj.fpcf.properties.escape.NoEscape;

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

    public void preciseTypeKnown() {
        Circle c = new
                @EscapeInCallee(
                        value = "the type is precise and the parameter is thrown away",
                        analyses = {InterProceduralEscapeAnalysis.class})
                @AtMostEscapeInCallee(
                        value = "intra-procedural analyses don't handle this",
                        analyses = SimpleEscapeAnalysis.class)

                        Circle();
        Interface x = new FinalClassExtendsC();
        x.cyclicFunction(c, 12);
    }

    public void preciseAndImpreciseType(Interface i) {
        Circle c = new
                @AtMostEscapeInCallee("the type is extensible")
                        Circle();
        Interface x = new FinalClassExtendsC();
        x.cyclicFunction(c, 12);
        i.cyclicFunction(c, 123);
    }
}

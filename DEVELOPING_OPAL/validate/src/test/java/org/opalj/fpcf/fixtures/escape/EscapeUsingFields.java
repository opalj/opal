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
import org.opalj.fpcf.properties.escape.*;

/**
 * Some example escapes using fields
 */
public class EscapeUsingFields {

    public Object f;
    public EscapeUsingFields g;
    public static Object global;

    public static void instanceFieldLoop() {
        EscapeUsingFields x =
                new @EscapeViaStaticField("written into static fields") EscapeUsingFields();
        x.g = x;
        while (true) {
            if (System.currentTimeMillis() == 12345678)
                break;
            x = x.g;
        }
        global = x;
    }

    public static void instanceFieldLoop2() {
        EscapeUsingFields x = new @NoEscape("local") EscapeUsingFields();
        EscapeUsingFields y = x;
        while (true) {
            if (System.currentTimeMillis() == 123456789)
                break;
            y.g = new
                    @EscapeViaStaticField(value = "escapes via field into global", analyses = {})
                    @AtMostNoEscape("we do not track fields") EscapeUsingFields();
            y = y.g;
        }
        global = x.g;
    }

    public static void instanceFieldFlowNoEscape() {
        EscapeUsingFields x = new
                @EscapeInCallee(value = "parameter does not escape",
                        analyses = InterProceduralEscapeAnalysis.class)
                @AtMostEscapeInCallee(value = "intra-procedural",
                        analyses = SimpleEscapeAnalysis.class)
                        EscapeUsingFields();
        formalParamNoEscape(x);
        x.f = new
                @AtMostNoEscape("we do not track fields")
                        Object(); //a2
    }

    public static void instanceFieldAlias() {
        EscapeUsingFields x = new @NoEscape("local") EscapeUsingFields();
        EscapeUsingFields y = x;
        y.f = new
                @EscapeViaStaticField(value = "escapes via field into global", analyses = {})
                @AtMostNoEscape("we do not track fields") EscapeUsingFields();
        global = x.f;
    }

    public static void instanceFieldNoEscape() {
        EscapeUsingFields x = new @NoEscape("local") EscapeUsingFields();
        x.f = new
                @NoEscape(value = "local", analyses = {})
                @AtMostNoEscape("we do not track fields") EscapeUsingFields();
    }

    public static void formalParamNoEscape(
            @NoEscape(value = "not used", analyses = InterProceduralEscapeAnalysis.class)
            @AtMostNoEscape(value = "simple analyses does not track params",
                    analyses = SimpleEscapeAnalysis.class)
                    Object param
    ) {
        if (param == null) {
            System.out.println("null");
        }
    }
}

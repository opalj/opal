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

public class AllEscapeStates {

    public Object f;

    public static Circle global;

    public static Object globalObject;

    public static int noEscape() {
        Circle c = new @NoEscape("the object stays local") Circle();
        return c.area;
    }

    public static void escapeInCallee() {
        Circle c = new
                @EscapeInCallee(value = "the this local escapes in toString",
                        analyses = InterProceduralEscapeAnalysis.class)
                @AtMostEscapeInCallee(value = "the simple analysis is intra-procedural",
                        analyses = SimpleEscapeAnalysis.class)
                        Circle();
        System.out.println(c.toString());
    }

    public static Circle escapeViaReturnOrException(int radius) {
        if (radius > 0) {
            return new @EscapeViaReturn("the object is returned") Circle(radius);
        }
        throw new @EscapeViaAbnormalReturn("the exception is thrown") IllegalArgumentException();
    }

    public static RuntimeException escapeViaReturnAndException(int i) {
        RuntimeException e = new @EscapeViaNormalAndAbnormalReturn(
                "the exception is thrown and returned") RuntimeException();
        if (i != 0) {
            return e;
        } else {
            throw e;
        }
    }

    public static void escapeViaStaticField() {
        Circle c = new @EscapeViaStaticField("assigned to global") Circle();
        AllEscapeStates.global = c;
    }

    public static void escapeViaHeapObjectAfterCast() {
        Object o = globalObject;
        if (o instanceof AllEscapeStates) {
            ((AllEscapeStates) o).f = new @EscapeViaHeapObject("assigned to global f") Object();
        }
    }

    public static void escapeViaParameter(AllEscapeStates param) {
        param.f =
                new @EscapeViaParameter(value = "passed to parameter", analyses = {})
                @AtMostEscapeViaParameter("passed to parameter, but analyses do not track further")
                        Object();
    }

    public static void parameterFieldGlobalEscape(AllEscapeStates param) {
        param.f = new
                @EscapeViaStaticField(value = "passed to parameter", analyses = {})
                @AtMostEscapeViaParameter(
                        "passed to parameter, but analyses do not track further") Object();
        globalObject = param;
    }

    public static Object multipleEscapes(AllEscapeStates param) {
        global = new @EscapeViaStaticField("assigned to global") Circle();

        if (param == null) {
            throw new @EscapeViaAbnormalReturn("thrown exception")
                    RuntimeException();
        }
        param.f =
                new @EscapeViaParameter(value = "passed to parameter", analyses = {})
                @AtMostEscapeViaParameter("passed to parameter, but analyses do not track further")
                        Object();
        Object local = new @NoEscape("local object") Object();
        Circle noLocal =
                new
                        @EscapeViaStaticField(value = "parameter escapes",
                                analyses = { InterProceduralEscapeAnalysis.class })
                        @AtMostEscapeInCallee(value = "intra-procedural",
                                analyses = SimpleEscapeAnalysis.class)
                                Circle();
        if (local != null) {
            formalParamEscape(noLocal);
        }
        return new @EscapeViaReturn("returned") Object();
    }

    private static void formalParamEscape(
            @EscapeViaStaticField(value = "assigned to static field",
                    analyses = InterProceduralEscapeAnalysis.class)
            @AtMostNoEscape(value = "intra-procedural",
                    analyses = SimpleEscapeAnalysis.class) Circle o
    ) {
        global = o;
    }

}

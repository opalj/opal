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

public class EscapesViaReturn {

    public static Object global;

    public Object simpleEscapeViaReturn() {
        return new @EscapeViaReturn("the object is returned") Object();
    }

    public static Object identity(
            @EscapeViaReturn(value = "the object is returned",
                    analyses = InterProceduralEscapeAnalysis.class)
                    Object o
    ) {
        return o;
    }

    public Object escapeAfterCallToIdentity() {
        Object o = new
                @EscapeViaReturn(
                        value = "the object is passed to an identity function and then returned",
                        analyses = InterProceduralEscapeAnalysis.class)
                @AtMostEscapeViaReturn(
                        value = "intra-procedural analyses don't track the call but the domain does",
                        analyses = SimpleEscapeAnalysis.class)
                @AtMostEscapeInCallee(value = "the domain does not recognize the identity", performInvokationsDomain = false)
                        Object();
        Object x = identity(o);
        return x;
    }

    public static void handlingReturnGlobalEscape() {
        Object o = new
                @EscapeViaStaticField(
                        value = "the object is passed to an identity function and escapes")
                @AtMostEscapeInCallee(value = "the domain does not recognize the identity", performInvokationsDomain = false)
                        Object();
        Object x = identity(o);
        if (x != null) {
            global = x;
        }
    }

    public void noEscapeAfterCallToIdentiy() {
        Object o = new
                @EscapeInCallee(
                        value = "the object is passed to an identity function and not returned",
                        analyses = InterProceduralEscapeAnalysis.class)
                @AtMostEscapeInCallee(
                        value = "intra-procedural analyses don't track the call",
                        analyses = SimpleEscapeAnalysis.class)
                        Object();
        identity(o);
    }

    public static Object sometimesIdentity(boolean b,
            @EscapeViaReturn(value = "the object is returned",
                    analyses = InterProceduralEscapeAnalysis.class)
                    Object o
    ) {
        if (b) {
            return o;
        } else {
            return null;
        }
    }

    public Object escapeAfterCallToSometimesIdentity(boolean b) {
        Object o = new
                @AtMostEscapeInCallee("the object is passed to an identity like function")
                        Object();
        return sometimesIdentity(b, o);
    }

    public Object globalEscapeIsWorseThanReturn(boolean b) {
        Object o = new @EscapeViaStaticField("the object is assigned to a static field") Object();
        if (b) {
            global = o;
            return null;
        } else {
            return o;
        }
    }
}

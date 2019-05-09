/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.escape;

import org.opalj.fpcf.properties.escape.*;
import org.opalj.tac.fpcf.analyses.escape.InterProceduralEscapeAnalysis;
import org.opalj.tac.fpcf.analyses.escape.SimpleEscapeAnalysis;

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
                @AtMostEscapeInCallee(value = "the object is passed to an identity like function", performInvokationsDomain = false)
                @AtMostEscapeViaReturn(value = "the object is passed to an identity like function", performInvokationsDomain = true)
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

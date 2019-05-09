/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.escape;

import org.opalj.fpcf.properties.escape.*;
import org.opalj.tac.fpcf.analyses.escape.InterProceduralEscapeAnalysis;
import org.opalj.tac.fpcf.analyses.escape.SimpleEscapeAnalysis;

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

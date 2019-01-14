/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.escape.coding;

import org.opalj.fpcf.properties.escape.AtMostEscapeViaParameter;
import org.opalj.tac.fpcf.analyses.escape.InterProceduralEscapeAnalysis;

/**
 * Example code without functionally taken from:
 * https://github.com/JetBrains/jdk8u_jdk/blob/master/src/share/classes/com/sun/java/util/jar/pack/BandStructure.java
 *
 * @author Florian Kübler
 */
public abstract class BandStructure {

    public static final int _meta_default = 0;

    final private static Coding[] basicCodings = {
            // Table of "Canonical BHSD Codings" from Pack200 spec.
            null,  // _meta_default

            // Fixed-length codings:
            Coding.of(1,256,0),
            Coding.of(1,256,1),
            Coding.of(2,256,0),
            Coding.of(2,256,1),
            Coding.of(3,256,0),
            Coding.of(3,256,1),
            Coding.of(4,256,0),
            Coding.of(4,256,1)
    };

    public static int parseMetaCoding(byte[] bytes, int pos, @AtMostEscapeViaParameter(value = "", analyses = InterProceduralEscapeAnalysis.class) Coding dflt, CodingMethod[] res) {
        if ((bytes[pos] & 0xFF) == _meta_default) {
            res[0] = dflt;
            return pos+1;
        }
        int pos2;
        pos2 = Coding.parseMetaCoding(bytes, pos, dflt, res);
        if (pos2 > pos)  return pos2;
        pos2 = PopulationCoding.parseMetaCoding12345(bytes, pos, dflt, res);
        if (pos2 > pos)  return pos2;
        pos2 = AdaptiveCoding.parseMetaCoding(bytes, pos, dflt, res);
        if (pos2 > pos)  return pos2;
        throw new RuntimeException("Bad meta-coding op "+(bytes[pos]&0xFF));
    }


    public static Coding codingForIndex(int i) {
        return i < basicCodings.length ? basicCodings[i] : null;
    }

}

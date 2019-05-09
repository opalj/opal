/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.escape.coding;

import org.opalj.fpcf.properties.escape.AtMostEscapeInCallee;
import org.opalj.tac.fpcf.analyses.escape.InterProceduralEscapeAnalysis;

/**
 * Example code without functionally taken from:
 * https://github.com/JetBrains/jdk8u_jdk/blob/master/src/share/classes/com/sun/java/util/jar/pack/AdaptiveCoding.java
 *
 * @author Florian Kübler
 */
public class AdaptiveCoding implements CodingMethod {

    CodingMethod tailCoding;
    CodingMethod headCoding;
    int headLength;

    public AdaptiveCoding(int headLength, CodingMethod headCoding, CodingMethod tailCoding) {
        assert(isCodableLength(headLength));
        this.headLength = headLength;
        this.headCoding = headCoding;
        this.tailCoding = tailCoding;
    }

    public static boolean isCodableLength(int K) {
        int KX = getKXOf(K);
        if (KX < 0)  return false;
        int unit = 1      << (KX * KX_LG2BASE);
        int mask = KB_MAX << (KX * KX_LG2BASE);
        return ((K - unit) & ~mask) == 0;
    }

    public static final int _meta_run = 117;
    public static final int _meta_pop = 141;
    public static final int KX_MIN = 0;
    public static final int KX_MAX = 3;
    public static final int KX_LG2BASE = 4;

    public static final int KB_MIN = 0x00;
    public static final int KB_MAX = 0xFF;
    public static final int KB_OFFSET = 1;
    public static final int KB_DEFAULT = 3;


    private static int decodeK(int KX, int KB) {
        assert(KX_MIN <= KX && KX <= KX_MAX);
        assert(KB_MIN <= KB && KB <= KB_MAX);
        return (KB+KB_OFFSET) << (KX * KX_LG2BASE);
    }

    public static int parseMetaCoding(byte[] bytes, int pos, @AtMostEscapeInCallee(value = "", analyses = InterProceduralEscapeAnalysis.class) Coding dflt, CodingMethod res[]) {
        int op = bytes[pos++] & 0xFF;
        if (op < _meta_run || op >= _meta_pop)  return pos-1; // backup
        AdaptiveCoding prevc = null;
        for (boolean keepGoing = true; keepGoing; ) {
            keepGoing = false;
            op -= _meta_run;
            int KX = op % 4;
            int KBFlag = (op / 4) % 2;
            int ABDef = (op / 8);
            assert(ABDef < 3);
            int ADef = (ABDef & 1);
            int BDef = (ABDef & 2);
            CodingMethod[] ACode = {dflt}, BCode = {dflt};
            int KB = KB_DEFAULT;
            if (KBFlag != 0)
                KB = bytes[pos++] & 0xFF;
            if (ADef == 0) {
                pos = BandStructure.parseMetaCoding(bytes, pos, dflt, ACode);
            }
            if (BDef == 0 &&
                    ((op = bytes[pos] & 0xFF) >= _meta_run) && op < _meta_pop) {
                pos++;
                keepGoing = true;
            } else if (BDef == 0) {
                pos = BandStructure.parseMetaCoding(bytes, pos, dflt, BCode);
            }
            AdaptiveCoding newc = new AdaptiveCoding(decodeK(KX, KB),
                    ACode[0], BCode[0]);
            if (prevc == null) {
                res[0] = newc;
            } else {
                prevc.tailCoding = newc;
            }
            prevc = newc;
        }
        return pos;
    }

    static int getKXOf(int K) {
        for (int KX = KX_MIN; KX <= KX_MAX; KX++) {
            if (((K - KB_OFFSET) & ~KB_MAX) == 0)
                return KX;
            K >>>= KX_LG2BASE;
        }
        return -1;
    }
}

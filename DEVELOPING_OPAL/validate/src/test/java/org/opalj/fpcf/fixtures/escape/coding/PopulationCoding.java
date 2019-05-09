/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.escape.coding;

import org.opalj.fpcf.properties.escape.AtMostEscapeInCallee;
import org.opalj.tac.fpcf.analyses.escape.InterProceduralEscapeAnalysis;

/**
 * Example code without functionally taken from:
 * https://github.com/JetBrains/jdk8u_jdk/blob/master/src/share/classes/com/sun/java/util/jar/pack/PopulationCoding.java
 *
 * @author Florian Kübler
 */
class PopulationCoding implements CodingMethod {
    int[]     fValues; // list of favored values
    int       fVlen;   // inclusive max index
    long[]    symtab;  // int map of favored value -> token [1..#fValues]

    CodingMethod favoredCoding;
    CodingMethod tokenCoding;
    CodingMethod unfavoredCoding;

    int L = -1; //preferred L value for tokenCoding

    public static final int _meta_pop = 141;
    public static final int _meta_limit = 189;
    static final int[] LValuesCoded
            = { -1, 4, 8, 16, 32, 64, 128, 192, 224, 240, 248, 252 };

    public static int parseMetaCoding12345(byte[] bytes, int pos, @AtMostEscapeInCallee(value = "", analyses = InterProceduralEscapeAnalysis.class) Coding dflt, CodingMethod res[]) {
        int op = bytes[pos++] & 0xFF;
        if (op < _meta_pop || op >= _meta_limit)  return pos-1; // backup
        op -= _meta_pop;
        int FDef = op % 2;
        int UDef = (op / 2) % 2;
        int TDefL = (op / 4);
        int TDef = (TDefL > 0)?1:0;
        int L = LValuesCoded[TDefL];
        CodingMethod[] FCode = {dflt}, TCode = {null}, UCode = {dflt};
        if (FDef == 0)
            pos = BandStructure.parseMetaCoding(bytes, pos, dflt, FCode);
        if (TDef == 0)
            pos = BandStructure.parseMetaCoding(bytes, pos, dflt, TCode);
        if (UDef == 0)
            pos = BandStructure.parseMetaCoding(bytes, pos, dflt, UCode);
        PopulationCoding pop = new PopulationCoding();
        pop.L = L;  // might be -1
        pop.favoredCoding   = FCode[0];
        pop.tokenCoding     = TCode[0];  // might be null!
        pop.unfavoredCoding = UCode[0];
        res[0] = pop;
        return pos;
    }

}


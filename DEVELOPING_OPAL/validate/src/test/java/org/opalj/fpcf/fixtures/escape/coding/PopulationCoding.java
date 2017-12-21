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
package org.opalj.fpcf.fixtures.escape.coding;

import org.opalj.fpcf.analyses.escape.InterProceduralEscapeAnalysis;
import org.opalj.fpcf.properties.escape.AtMostEscapeInCallee;

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


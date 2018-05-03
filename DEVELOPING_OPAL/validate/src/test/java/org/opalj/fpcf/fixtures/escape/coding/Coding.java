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
import org.opalj.fpcf.properties.escape.NoEscape;

import java.util.HashMap;
import java.util.Map;

/**
 * Example code without functionally taken from:
 * https://github.com/JetBrains/jdk8u_jdk/blob/master/src/share/classes/com/sun/java/util/jar/pack/Coding.java
 *
 * @author Florian Kübler
 */
public class Coding implements CodingMethod{
    public static final int _meta_canon_min = 1;
    public static final int _meta_canon_max = 115;
    public static final int _meta_arb = 116;

    public static final int B_MAX = 5;    /* B: [1,5] */
    public static final int H_MAX = 256;  /* H: [1,256] */
    public static final int S_MAX = 2; /* S: [0,2] */

    private static Map<Coding, Coding> codeMap;
    public static Coding of(int B, int H, int S) {
        return of(B, H, S, 0);
    }
    private static synchronized Coding of(int B, int H, int S, int del) {
        if (codeMap == null)  codeMap = new HashMap<>();
        Coding x0 = new Coding(B, H, S, del);
        Coding x1 = codeMap.get(x0);
        if (x1 == null)  codeMap.put(x0, x1 = x0);
        return x1;
    }

    private Coding(int B, int H, int S, int del) {

    }

    public static int parseMetaCoding(byte[] bytes, int pos, @NoEscape(value = "", analyses = InterProceduralEscapeAnalysis.class) Coding dflt, CodingMethod res[]) {
        int op = bytes[pos++] & 0xFF;
        if (_meta_canon_min <= op && op <= _meta_canon_max) {
            Coding c = BandStructure.codingForIndex(op);
            assert(c != null);
            res[0] = c;
            return pos;
        }
        if (op == _meta_arb) {
            int dsb = bytes[pos++] & 0xFF;
            int H_1 = bytes[pos++] & 0xFF;
            int del = dsb % 2;
            int S = (dsb / 2) % 4;
            int B = (dsb / 8)+1;
            int H = H_1+1;
            if (!((1 <= B && B <= B_MAX) &&
                    (0 <= S && S <= S_MAX) &&
                    (1 <= H && H <= H_MAX) &&
                    (0 <= del && del <= 1))
                    || (B == 1 && H != 256)
                    || (B == 5 && H == 256)) {
                throw new RuntimeException("Bad arb. coding: ("+B+","+H+","+S+","+del);
            }
            res[0] = Coding.of(B, H, S, del);
            return pos;
        }
        return pos-1;  // backup
    }
}

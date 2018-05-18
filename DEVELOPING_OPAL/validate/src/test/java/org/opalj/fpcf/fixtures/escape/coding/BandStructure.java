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
import org.opalj.fpcf.properties.escape.AtMostEscapeViaParameter;

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

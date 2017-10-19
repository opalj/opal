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
package org.opalj
package fpcf
package properties

import org.junit.runner.RunWith
import org.scalatest.Matchers
import org.scalatest.FlatSpec
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class EscapePropertyTest extends FlatSpec with Matchers {
    val allProperties = List(
        NoEscape, EscapeInCallee, EscapeViaParameter, EscapeViaReturn, EscapeViaAbnormalReturn,
        EscapeViaParameterAndReturn, EscapeViaParameterAndAbnormalReturn, EscapeViaNormalAndAbnormalReturn,
        EscapeViaParameterAndNormalAndAbnormalReturn, GlobalEscape, MaybeNoEscape, MaybeEscapeInCallee,
        MaybeEscapeViaParameter, MaybeEscapeViaReturn, MaybeEscapeViaAbnormalReturn,
        MaybeEscapeViaParameterAndReturn, MaybeEscapeViaParameterAndAbnormalReturn, MaybeEscapeViaNormalAndAbnormalReturn,
        MaybeEscapeViaParameterAndNormalAndAbnormalReturn
    )

    behavior of "the meet operator"

    for (prop1 <- allProperties) {
        for (prop2 <- allProperties){
            val meet12 = prop1 meet prop2
            val meet21 = prop2 meet prop1
            it should s"be symmetric for $prop1 and $prop2" in {
                meet12 should be(meet21)
            }
            it should s"less or equal restrictive with respect to $prop1 and $prop2" in {
                meet12 lessOrEqualRestrictive prop1 should be(true)
                meet12 lessOrEqualRestrictive prop2 should be(true)
            }
        }
        it should s"evaluate to $prop1 for $prop1 meet $prop1" in {
            prop1 meet prop1 should be(prop1)
        }
    }

    s"$NoEscape meet $EscapeInCallee" should s"be $EscapeInCallee" in {
        NoEscape meet EscapeInCallee should be(EscapeInCallee)
    }
    s"$NoEscape meet $EscapeViaReturn" should s"be $EscapeViaReturn" in {
        NoEscape meet EscapeViaReturn should be(EscapeViaReturn)
    }
    s"$NoEscape meet $EscapeViaParameter" should s"be $EscapeViaParameter" in {
        NoEscape meet EscapeViaParameter should be(EscapeViaParameter)
    }
    s"$NoEscape meet $EscapeViaAbnormalReturn" should s"be $EscapeViaAbnormalReturn" in {
        NoEscape meet EscapeViaAbnormalReturn should be(EscapeViaAbnormalReturn)
    }
    s"$NoEscape meet $EscapeViaParameterAndReturn" should s"be $EscapeViaParameterAndReturn" in {
        NoEscape meet EscapeViaParameterAndReturn should be(EscapeViaParameterAndReturn)
    }
    s"$NoEscape meet $EscapeViaParameterAndAbnormalReturn" should s"be $EscapeViaParameterAndAbnormalReturn" in {
        NoEscape meet EscapeViaParameterAndAbnormalReturn should be(EscapeViaParameterAndAbnormalReturn)
    }
    s"$NoEscape meet $EscapeViaNormalAndAbnormalReturn" should s"be $EscapeViaNormalAndAbnormalReturn" in {
        NoEscape meet EscapeViaNormalAndAbnormalReturn should be(EscapeViaNormalAndAbnormalReturn)
    }
    s"$NoEscape meet $EscapeViaParameterAndNormalAndAbnormalReturn" should s"be $EscapeViaParameterAndNormalAndAbnormalReturn" in {
        NoEscape meet EscapeViaParameterAndNormalAndAbnormalReturn should be(EscapeViaParameterAndNormalAndAbnormalReturn)
    }
    s"$NoEscape meet $MaybeEscapeInCallee" should s"be $MaybeEscapeInCallee" in {
        NoEscape meet MaybeEscapeInCallee should be(MaybeEscapeInCallee)
    }
    s"$NoEscape meet $MaybeEscapeViaReturn" should s"be $MaybeEscapeViaReturn" in {
        NoEscape meet MaybeEscapeViaReturn should be(MaybeEscapeViaReturn)
    }
    s"$NoEscape meet $MaybeEscapeViaParameter" should s"be $MaybeEscapeViaParameter" in {
        NoEscape meet MaybeEscapeViaParameter should be(MaybeEscapeViaParameter)
    }
    s"$NoEscape meet $MaybeEscapeViaParameterAndReturn" should s"be $MaybeEscapeViaParameterAndReturn" in {
        NoEscape meet MaybeEscapeViaParameterAndReturn should be(MaybeEscapeViaParameterAndReturn)
    }
    s"$NoEscape meet $MaybeEscapeViaParameterAndAbnormalReturn" should s"be $MaybeEscapeViaParameterAndAbnormalReturn" in {
        NoEscape meet MaybeEscapeViaParameterAndAbnormalReturn should be(MaybeEscapeViaParameterAndAbnormalReturn)
    }
    s"$NoEscape meet $MaybeEscapeViaNormalAndAbnormalReturn" should s"be $MaybeEscapeViaNormalAndAbnormalReturn" in {
        NoEscape meet MaybeEscapeViaNormalAndAbnormalReturn should be(MaybeEscapeViaNormalAndAbnormalReturn)
    }
    s"$NoEscape meet $MaybeEscapeViaParameterAndNormalAndAbnormalReturn" should s"be $MaybeEscapeViaParameterAndNormalAndAbnormalReturn" in {
        NoEscape meet MaybeEscapeViaParameterAndNormalAndAbnormalReturn should be(MaybeEscapeViaParameterAndNormalAndAbnormalReturn)
    }
    s"$NoEscape meet $GlobalEscape" should s"be $GlobalEscape" in {
        NoEscape meet GlobalEscape should be(GlobalEscape)
    }



    s"$EscapeInCallee meet $EscapeViaReturn" should s"be $EscapeViaReturn" in {
        EscapeInCallee meet EscapeViaReturn should be(EscapeViaReturn)
    }
    s"$EscapeInCallee meet $EscapeViaParameter" should s"be $EscapeViaParameter" in {
        EscapeInCallee meet EscapeViaParameter should be(EscapeViaParameter)
    }
    s"$EscapeInCallee meet $EscapeViaAbnormalReturn" should s"be $EscapeViaAbnormalReturn" in {
        EscapeInCallee meet EscapeViaAbnormalReturn should be(EscapeViaAbnormalReturn)
    }
    s"$EscapeInCallee meet $EscapeViaParameterAndReturn" should s"be $EscapeViaParameterAndReturn" in {
        EscapeInCallee meet EscapeViaParameterAndReturn should be(EscapeViaParameterAndReturn)
    }
    s"$EscapeInCallee meet $EscapeViaParameterAndAbnormalReturn" should s"be $EscapeViaParameterAndAbnormalReturn" in {
        EscapeInCallee meet EscapeViaParameterAndAbnormalReturn should be(EscapeViaParameterAndAbnormalReturn)
    }
    s"$EscapeInCallee meet $EscapeViaNormalAndAbnormalReturn" should s"be $EscapeViaNormalAndAbnormalReturn" in {
        EscapeInCallee meet EscapeViaNormalAndAbnormalReturn should be(EscapeViaNormalAndAbnormalReturn)
    }
    s"$EscapeInCallee meet $EscapeViaParameterAndNormalAndAbnormalReturn" should s"be $EscapeViaParameterAndNormalAndAbnormalReturn" in {
        EscapeInCallee meet EscapeViaParameterAndNormalAndAbnormalReturn should be(EscapeViaParameterAndNormalAndAbnormalReturn)
    }
    s"$EscapeInCallee meet $MaybeEscapeInCallee" should s"be $MaybeEscapeInCallee" in {
        EscapeInCallee meet MaybeEscapeInCallee should be(MaybeEscapeInCallee)
    }
    s"$EscapeInCallee meet $MaybeEscapeViaReturn" should s"be $MaybeEscapeViaReturn" in {
        EscapeInCallee meet MaybeEscapeViaReturn should be(MaybeEscapeViaReturn)
    }
    s"$EscapeInCallee meet $MaybeEscapeViaParameter" should s"be $MaybeEscapeViaParameter" in {
        EscapeInCallee meet MaybeEscapeViaParameter should be(MaybeEscapeViaParameter)
    }
    s"$EscapeInCallee meet $MaybeEscapeViaParameterAndReturn" should s"be $MaybeEscapeViaParameterAndReturn" in {
        EscapeInCallee meet MaybeEscapeViaParameterAndReturn should be(MaybeEscapeViaParameterAndReturn)
    }
    s"$EscapeInCallee meet $MaybeEscapeViaParameterAndAbnormalReturn" should s"be $MaybeEscapeViaParameterAndAbnormalReturn" in {
        EscapeInCallee meet MaybeEscapeViaParameterAndAbnormalReturn should be(MaybeEscapeViaParameterAndAbnormalReturn)
    }
    s"$EscapeInCallee meet $MaybeEscapeViaNormalAndAbnormalReturn" should s"be $MaybeEscapeViaNormalAndAbnormalReturn" in {
        EscapeInCallee meet MaybeEscapeViaNormalAndAbnormalReturn should be(MaybeEscapeViaNormalAndAbnormalReturn)
    }
    s"$EscapeInCallee meet $MaybeEscapeViaParameterAndNormalAndAbnormalReturn" should s"be $MaybeEscapeViaParameterAndNormalAndAbnormalReturn" in {
        EscapeInCallee meet MaybeEscapeViaParameterAndNormalAndAbnormalReturn should be(MaybeEscapeViaParameterAndNormalAndAbnormalReturn)
    }
    s"$EscapeInCallee meet $GlobalEscape" should s"be $GlobalEscape" in {
        EscapeInCallee meet GlobalEscape should be(GlobalEscape)
    }


    s"$EscapeViaReturn meet $EscapeViaParameter" should s"be $EscapeViaParameterAndReturn" in {
        EscapeViaReturn meet EscapeViaParameter should be(EscapeViaParameterAndReturn)
    }
    s"$EscapeViaReturn meet $EscapeViaAbnormalReturn" should s"be $EscapeViaNormalAndAbnormalReturn" in {
        EscapeViaReturn meet EscapeViaAbnormalReturn should be(EscapeViaNormalAndAbnormalReturn)
    }
    s"$EscapeViaReturn meet $EscapeViaParameterAndReturn" should s"be $EscapeViaParameterAndReturn" in {
        EscapeViaReturn meet EscapeViaParameterAndReturn should be(EscapeViaParameterAndReturn)
    }
    s"$EscapeViaReturn meet $EscapeViaParameterAndAbnormalReturn" should s"be $EscapeViaParameterAndNormalAndAbnormalReturn" in {
        EscapeViaReturn meet EscapeViaParameterAndAbnormalReturn should be(EscapeViaParameterAndNormalAndAbnormalReturn)
    }
    s"$EscapeViaReturn meet $EscapeViaNormalAndAbnormalReturn" should s"be $EscapeViaNormalAndAbnormalReturn" in {
        EscapeViaReturn meet EscapeViaNormalAndAbnormalReturn should be(EscapeViaNormalAndAbnormalReturn)
    }
    s"$EscapeViaReturn meet $EscapeViaParameterAndNormalAndAbnormalReturn" should s"be $EscapeViaParameterAndNormalAndAbnormalReturn" in {
        EscapeViaReturn meet EscapeViaParameterAndNormalAndAbnormalReturn should be(EscapeViaParameterAndNormalAndAbnormalReturn)
    }
    s"$EscapeViaReturn meet $MaybeEscapeInCallee" should s"be $MaybeEscapeViaReturn" in {
        EscapeViaReturn meet MaybeEscapeInCallee should be(MaybeEscapeViaReturn)
    }
    s"$EscapeViaReturn meet $MaybeEscapeViaReturn" should s"be $MaybeEscapeViaReturn" in {
        EscapeViaReturn meet MaybeEscapeViaReturn should be(MaybeEscapeViaReturn)
    }
    s"$EscapeViaReturn meet $MaybeEscapeViaParameter" should s"be $MaybeEscapeViaParameterAndReturn" in {
        EscapeViaReturn meet MaybeEscapeViaParameter should be(MaybeEscapeViaParameterAndReturn)
    }
    s"$EscapeViaReturn meet $MaybeEscapeViaParameterAndReturn" should s"be $MaybeEscapeViaParameterAndReturn" in {
        EscapeViaReturn meet MaybeEscapeViaParameterAndReturn should be(MaybeEscapeViaParameterAndReturn)
    }
    s"$EscapeViaReturn meet $MaybeEscapeViaParameterAndAbnormalReturn" should s"be $MaybeEscapeViaParameterAndNormalAndAbnormalReturn" in {
        EscapeViaReturn meet MaybeEscapeViaParameterAndAbnormalReturn should be(MaybeEscapeViaParameterAndNormalAndAbnormalReturn)
    }
    s"$EscapeViaReturn meet $MaybeEscapeViaNormalAndAbnormalReturn" should s"be $MaybeEscapeViaNormalAndAbnormalReturn" in {
        EscapeViaReturn meet MaybeEscapeViaNormalAndAbnormalReturn should be(MaybeEscapeViaNormalAndAbnormalReturn)
    }
    s"$EscapeViaReturn meet $MaybeEscapeViaParameterAndNormalAndAbnormalReturn" should s"be $MaybeEscapeViaParameterAndNormalAndAbnormalReturn" in {
        EscapeViaReturn meet MaybeEscapeViaParameterAndNormalAndAbnormalReturn should be(MaybeEscapeViaParameterAndNormalAndAbnormalReturn)
    }
    s"$EscapeViaReturn meet $GlobalEscape" should s"be $GlobalEscape" in {
        EscapeViaReturn meet GlobalEscape should be(GlobalEscape)
    }


    s"$EscapeViaParameter meet $EscapeViaAbnormalReturn" should s"be $EscapeViaParameterAndAbnormalReturn" in {
        EscapeViaParameter meet EscapeViaAbnormalReturn should be(EscapeViaParameterAndAbnormalReturn)
    }
    s"$EscapeViaParameter meet $EscapeViaParameterAndReturn" should s"be $EscapeViaParameterAndReturn" in {
        EscapeViaParameter meet EscapeViaParameterAndReturn should be(EscapeViaParameterAndReturn)
    }
    s"$EscapeViaParameter meet $EscapeViaParameterAndAbnormalReturn" should s"be $EscapeViaParameterAndAbnormalReturn" in {
        EscapeViaParameter meet EscapeViaParameterAndAbnormalReturn should be(EscapeViaParameterAndAbnormalReturn)
    }
    s"$EscapeViaParameter meet $EscapeViaNormalAndAbnormalReturn" should s"be $EscapeViaParameterAndNormalAndAbnormalReturn" in {
        EscapeViaParameter meet EscapeViaNormalAndAbnormalReturn should be(EscapeViaParameterAndNormalAndAbnormalReturn)
    }
    s"$EscapeViaParameter meet $EscapeViaParameterAndNormalAndAbnormalReturn" should s"be $EscapeViaParameterAndNormalAndAbnormalReturn" in {
        EscapeViaParameter meet EscapeViaParameterAndNormalAndAbnormalReturn should be(EscapeViaParameterAndNormalAndAbnormalReturn)
    }
    s"$EscapeViaParameter meet $MaybeEscapeInCallee" should s"be $MaybeEscapeViaParameter" in {
        EscapeViaParameter meet MaybeEscapeInCallee should be(MaybeEscapeViaParameter)
    }
    s"$EscapeViaParameter meet $MaybeEscapeViaReturn" should s"be $MaybeEscapeViaParameterAndReturn" in {
        EscapeViaParameter meet MaybeEscapeViaReturn should be(MaybeEscapeViaParameterAndReturn)
    }
    s"$EscapeViaParameter meet $MaybeEscapeViaParameter" should s"be $MaybeEscapeViaParameter" in {
        EscapeViaParameter meet MaybeEscapeViaParameter should be(MaybeEscapeViaParameter)
    }
    s"$EscapeViaParameter meet $MaybeEscapeViaParameterAndReturn" should s"be $MaybeEscapeViaParameterAndReturn" in {
        EscapeViaParameter meet MaybeEscapeViaParameterAndReturn should be(MaybeEscapeViaParameterAndReturn)
    }
    s"$EscapeViaParameter meet $MaybeEscapeViaParameterAndAbnormalReturn" should s"be $MaybeEscapeViaParameterAndAbnormalReturn" in {
        EscapeViaParameter meet MaybeEscapeViaParameterAndAbnormalReturn should be(MaybeEscapeViaParameterAndAbnormalReturn)
    }
    s"$EscapeViaParameter meet $MaybeEscapeViaNormalAndAbnormalReturn" should s"be $MaybeEscapeViaParameterAndNormalAndAbnormalReturn" in {
        EscapeViaParameter meet MaybeEscapeViaNormalAndAbnormalReturn should be(MaybeEscapeViaParameterAndNormalAndAbnormalReturn)
    }
    s"$EscapeViaParameter meet $MaybeEscapeViaParameterAndNormalAndAbnormalReturn" should s"be $MaybeEscapeViaParameterAndNormalAndAbnormalReturn" in {
        EscapeViaParameter meet MaybeEscapeViaParameterAndNormalAndAbnormalReturn should be(MaybeEscapeViaParameterAndNormalAndAbnormalReturn)
    }
    s"$EscapeViaParameter meet $GlobalEscape" should s"be $GlobalEscape" in {
        EscapeViaParameter meet GlobalEscape should be(GlobalEscape)
    }


    s"$EscapeViaAbnormalReturn meet $EscapeViaParameterAndReturn" should s"be $EscapeViaParameterAndNormalAndAbnormalReturn" in {
        EscapeViaAbnormalReturn meet EscapeViaParameterAndReturn should be(EscapeViaParameterAndNormalAndAbnormalReturn)
    }
    s"$EscapeViaAbnormalReturn meet $EscapeViaParameterAndAbnormalReturn" should s"be $EscapeViaParameterAndAbnormalReturn" in {
        EscapeViaAbnormalReturn meet EscapeViaParameterAndAbnormalReturn should be(EscapeViaParameterAndAbnormalReturn)
    }
    s"$EscapeViaAbnormalReturn meet $EscapeViaNormalAndAbnormalReturn" should s"be $EscapeViaNormalAndAbnormalReturn" in {
        EscapeViaAbnormalReturn meet EscapeViaNormalAndAbnormalReturn should be(EscapeViaNormalAndAbnormalReturn)
    }
    s"$EscapeViaAbnormalReturn meet $EscapeViaParameterAndNormalAndAbnormalReturn" should s"be $EscapeViaParameterAndNormalAndAbnormalReturn" in {
        EscapeViaAbnormalReturn meet EscapeViaParameterAndNormalAndAbnormalReturn should be(EscapeViaParameterAndNormalAndAbnormalReturn)
    }
    s"$EscapeViaAbnormalReturn meet $MaybeEscapeInCallee" should s"be $MaybeEscapeViaAbnormalReturn" in {
        EscapeViaAbnormalReturn meet MaybeEscapeInCallee should be(MaybeEscapeViaAbnormalReturn)
    }
    s"$EscapeViaAbnormalReturn meet $MaybeEscapeViaReturn" should s"be $MaybeEscapeViaNormalAndAbnormalReturn" in {
        EscapeViaAbnormalReturn meet MaybeEscapeViaReturn should be(MaybeEscapeViaNormalAndAbnormalReturn)
    }
    s"$EscapeViaAbnormalReturn meet $MaybeEscapeViaParameter" should s"be $MaybeEscapeViaParameterAndAbnormalReturn" in {
        EscapeViaAbnormalReturn meet MaybeEscapeViaParameter should be(MaybeEscapeViaParameterAndAbnormalReturn)
    }
    s"$EscapeViaAbnormalReturn meet $MaybeEscapeViaParameterAndReturn" should s"be $MaybeEscapeViaParameterAndNormalAndAbnormalReturn" in {
        EscapeViaAbnormalReturn meet MaybeEscapeViaParameterAndReturn should be(MaybeEscapeViaParameterAndNormalAndAbnormalReturn)
    }
    s"$EscapeViaAbnormalReturn meet $MaybeEscapeViaParameterAndAbnormalReturn" should s"be $MaybeEscapeViaParameterAndAbnormalReturn" in {
        EscapeViaAbnormalReturn meet MaybeEscapeViaParameterAndAbnormalReturn should be(MaybeEscapeViaParameterAndAbnormalReturn)
    }
    s"$EscapeViaAbnormalReturn meet $MaybeEscapeViaNormalAndAbnormalReturn" should s"be $MaybeEscapeViaNormalAndAbnormalReturn" in {
        EscapeViaAbnormalReturn meet MaybeEscapeViaNormalAndAbnormalReturn should be(MaybeEscapeViaNormalAndAbnormalReturn)
    }
    s"$EscapeViaAbnormalReturn meet $MaybeEscapeViaParameterAndNormalAndAbnormalReturn" should s"be $MaybeEscapeViaParameterAndNormalAndAbnormalReturn" in {
        EscapeViaAbnormalReturn meet MaybeEscapeViaParameterAndNormalAndAbnormalReturn should be(MaybeEscapeViaParameterAndNormalAndAbnormalReturn)
    }
    s"$EscapeViaAbnormalReturn meet $GlobalEscape" should s"be $GlobalEscape" in {
        EscapeViaAbnormalReturn meet GlobalEscape should be(GlobalEscape)
    }


    s"$EscapeViaParameterAndReturn meet $EscapeViaParameterAndAbnormalReturn" should s"be $EscapeViaParameterAndNormalAndAbnormalReturn" in {
        EscapeViaParameterAndReturn meet EscapeViaParameterAndAbnormalReturn should be(EscapeViaParameterAndNormalAndAbnormalReturn)
    }
    s"$EscapeViaParameterAndReturn meet $EscapeViaNormalAndAbnormalReturn" should s"be $EscapeViaParameterAndNormalAndAbnormalReturn" in {
        EscapeViaParameterAndReturn meet EscapeViaNormalAndAbnormalReturn should be(EscapeViaParameterAndNormalAndAbnormalReturn)
    }
    s"$EscapeViaParameterAndReturn meet $EscapeViaParameterAndNormalAndAbnormalReturn" should s"be $EscapeViaParameterAndNormalAndAbnormalReturn" in {
        EscapeViaParameterAndReturn meet EscapeViaParameterAndNormalAndAbnormalReturn should be(EscapeViaParameterAndNormalAndAbnormalReturn)
    }
    s"$EscapeViaParameterAndReturn meet $MaybeEscapeInCallee" should s"be $MaybeEscapeViaParameterAndReturn" in {
        EscapeViaParameterAndReturn meet MaybeEscapeInCallee should be(MaybeEscapeViaParameterAndReturn)
    }
    s"$EscapeViaParameterAndReturn meet $MaybeEscapeViaReturn" should s"be $MaybeEscapeViaParameterAndReturn" in {
        EscapeViaParameterAndReturn meet MaybeEscapeViaReturn should be(MaybeEscapeViaParameterAndReturn)
    }
    s"$EscapeViaParameterAndReturn meet $MaybeEscapeViaParameter" should s"be $MaybeEscapeViaParameterAndReturn" in {
        EscapeViaParameterAndReturn meet MaybeEscapeViaParameter should be(MaybeEscapeViaParameterAndReturn)
    }
    s"$EscapeViaParameterAndReturn meet $MaybeEscapeViaParameterAndReturn" should s"be $MaybeEscapeViaParameterAndReturn" in {
        EscapeViaParameterAndReturn meet MaybeEscapeViaParameterAndReturn should be(MaybeEscapeViaParameterAndReturn)
    }
    s"$EscapeViaParameterAndReturn meet $MaybeEscapeViaParameterAndAbnormalReturn" should s"be $MaybeEscapeViaParameterAndNormalAndAbnormalReturn" in {
        EscapeViaParameterAndReturn meet MaybeEscapeViaParameterAndAbnormalReturn should be(MaybeEscapeViaParameterAndNormalAndAbnormalReturn)
    }
    s"$EscapeViaParameterAndReturn meet $MaybeEscapeViaNormalAndAbnormalReturn" should s"be $MaybeEscapeViaParameterAndNormalAndAbnormalReturn" in {
        EscapeViaParameterAndReturn meet MaybeEscapeViaNormalAndAbnormalReturn should be(MaybeEscapeViaParameterAndNormalAndAbnormalReturn)
    }
    s"$EscapeViaParameterAndReturn meet $MaybeEscapeViaParameterAndNormalAndAbnormalReturn" should s"be $MaybeEscapeViaParameterAndNormalAndAbnormalReturn" in {
        EscapeViaParameterAndReturn meet MaybeEscapeViaParameterAndNormalAndAbnormalReturn should be(MaybeEscapeViaParameterAndNormalAndAbnormalReturn)
    }
    s"$EscapeViaParameterAndReturn meet $GlobalEscape" should s"be $GlobalEscape" in {
        EscapeViaParameterAndReturn meet GlobalEscape should be(GlobalEscape)
    }


    s"$EscapeViaParameterAndAbnormalReturn meet $EscapeViaNormalAndAbnormalReturn" should s"be $EscapeViaParameterAndNormalAndAbnormalReturn" in {
        EscapeViaParameterAndAbnormalReturn meet EscapeViaNormalAndAbnormalReturn should be(EscapeViaParameterAndNormalAndAbnormalReturn)
    }
    s"$EscapeViaParameterAndAbnormalReturn meet $EscapeViaParameterAndNormalAndAbnormalReturn" should s"be $EscapeViaParameterAndNormalAndAbnormalReturn" in {
        EscapeViaParameterAndAbnormalReturn meet EscapeViaParameterAndNormalAndAbnormalReturn should be(EscapeViaParameterAndNormalAndAbnormalReturn)
    }
    s"$EscapeViaParameterAndAbnormalReturn meet $MaybeEscapeInCallee" should s"be $MaybeEscapeViaParameterAndAbnormalReturn" in {
        EscapeViaParameterAndAbnormalReturn meet MaybeEscapeInCallee should be(MaybeEscapeViaParameterAndAbnormalReturn)
    }
    s"$EscapeViaParameterAndAbnormalReturn meet $MaybeEscapeViaReturn" should s"be $MaybeEscapeViaParameterAndNormalAndAbnormalReturn" in {
        EscapeViaParameterAndAbnormalReturn meet MaybeEscapeViaReturn should be(MaybeEscapeViaParameterAndNormalAndAbnormalReturn)
    }
    s"$EscapeViaParameterAndAbnormalReturn meet $MaybeEscapeViaParameter" should s"be $MaybeEscapeViaParameterAndAbnormalReturn" in {
        EscapeViaParameterAndAbnormalReturn meet MaybeEscapeViaParameter should be(MaybeEscapeViaParameterAndAbnormalReturn)
    }
    s"$EscapeViaParameterAndAbnormalReturn meet $MaybeEscapeViaParameterAndReturn" should s"be $MaybeEscapeViaParameterAndNormalAndAbnormalReturn" in {
        EscapeViaParameterAndAbnormalReturn meet MaybeEscapeViaParameterAndReturn should be(MaybeEscapeViaParameterAndNormalAndAbnormalReturn)
    }
    s"$EscapeViaParameterAndAbnormalReturn meet $MaybeEscapeViaParameterAndAbnormalReturn" should s"be $MaybeEscapeViaParameterAndAbnormalReturn" in {
        EscapeViaParameterAndAbnormalReturn meet MaybeEscapeViaParameterAndAbnormalReturn should be(MaybeEscapeViaParameterAndAbnormalReturn)
    }
    s"$EscapeViaParameterAndAbnormalReturn meet $MaybeEscapeViaNormalAndAbnormalReturn" should s"be $MaybeEscapeViaParameterAndNormalAndAbnormalReturn" in {
        EscapeViaParameterAndAbnormalReturn meet MaybeEscapeViaNormalAndAbnormalReturn should be(MaybeEscapeViaParameterAndNormalAndAbnormalReturn)
    }
    s"$EscapeViaParameterAndAbnormalReturn meet $MaybeEscapeViaParameterAndNormalAndAbnormalReturn" should s"be $MaybeEscapeViaParameterAndNormalAndAbnormalReturn" in {
        EscapeViaParameterAndAbnormalReturn meet MaybeEscapeViaParameterAndNormalAndAbnormalReturn should be(MaybeEscapeViaParameterAndNormalAndAbnormalReturn)
    }
    s"$EscapeViaParameterAndAbnormalReturn meet $GlobalEscape" should s"be $GlobalEscape" in {
        EscapeViaParameterAndAbnormalReturn meet GlobalEscape should be(GlobalEscape)
    }


    s"$EscapeViaNormalAndAbnormalReturn meet $EscapeViaParameterAndNormalAndAbnormalReturn" should s"be $EscapeViaParameterAndNormalAndAbnormalReturn" in {
        EscapeViaNormalAndAbnormalReturn meet EscapeViaParameterAndNormalAndAbnormalReturn should be(EscapeViaParameterAndNormalAndAbnormalReturn)
    }
    s"$EscapeViaNormalAndAbnormalReturn meet $MaybeEscapeInCallee" should s"be $MaybeEscapeViaNormalAndAbnormalReturn" in {
        EscapeViaNormalAndAbnormalReturn meet MaybeEscapeInCallee should be(MaybeEscapeViaNormalAndAbnormalReturn)
    }
    s"$EscapeViaNormalAndAbnormalReturn meet $MaybeEscapeViaReturn" should s"be $MaybeEscapeViaNormalAndAbnormalReturn" in {
        EscapeViaNormalAndAbnormalReturn meet MaybeEscapeViaReturn should be(MaybeEscapeViaNormalAndAbnormalReturn)
    }
    s"$EscapeViaNormalAndAbnormalReturn meet $MaybeEscapeViaParameter" should s"be $MaybeEscapeViaParameterAndNormalAndAbnormalReturn" in {
        EscapeViaNormalAndAbnormalReturn meet MaybeEscapeViaParameter should be(MaybeEscapeViaParameterAndNormalAndAbnormalReturn)
    }
    s"$EscapeViaNormalAndAbnormalReturn meet $MaybeEscapeViaParameterAndReturn" should s"be $MaybeEscapeViaParameterAndNormalAndAbnormalReturn" in {
        EscapeViaNormalAndAbnormalReturn meet MaybeEscapeViaParameterAndReturn should be(MaybeEscapeViaParameterAndNormalAndAbnormalReturn)
    }
    s"$EscapeViaNormalAndAbnormalReturn meet $MaybeEscapeViaParameterAndAbnormalReturn" should s"be $MaybeEscapeViaParameterAndNormalAndAbnormalReturn" in {
        EscapeViaNormalAndAbnormalReturn meet MaybeEscapeViaParameterAndAbnormalReturn should be(MaybeEscapeViaParameterAndNormalAndAbnormalReturn)
    }
    s"$EscapeViaNormalAndAbnormalReturn meet $MaybeEscapeViaNormalAndAbnormalReturn" should s"be $MaybeEscapeViaNormalAndAbnormalReturn" in {
        EscapeViaNormalAndAbnormalReturn meet MaybeEscapeViaNormalAndAbnormalReturn should be(MaybeEscapeViaNormalAndAbnormalReturn)
    }
    s"$EscapeViaNormalAndAbnormalReturn meet $MaybeEscapeViaParameterAndNormalAndAbnormalReturn" should s"be $MaybeEscapeViaParameterAndNormalAndAbnormalReturn" in {
        EscapeViaNormalAndAbnormalReturn meet MaybeEscapeViaParameterAndNormalAndAbnormalReturn should be(MaybeEscapeViaParameterAndNormalAndAbnormalReturn)
    }
    s"$EscapeViaNormalAndAbnormalReturn meet $GlobalEscape" should s"be $GlobalEscape" in {
        EscapeViaNormalAndAbnormalReturn meet GlobalEscape should be(GlobalEscape)
    }


    s"$EscapeViaParameterAndNormalAndAbnormalReturn meet $MaybeEscapeInCallee" should s"be $MaybeEscapeViaParameterAndNormalAndAbnormalReturn" in {
        EscapeViaParameterAndNormalAndAbnormalReturn meet MaybeEscapeInCallee should be(MaybeEscapeViaParameterAndNormalAndAbnormalReturn)
    }
    s"$EscapeViaParameterAndNormalAndAbnormalReturn meet $MaybeEscapeViaReturn" should s"be $MaybeEscapeViaParameterAndNormalAndAbnormalReturn" in {
        EscapeViaParameterAndNormalAndAbnormalReturn meet MaybeEscapeViaReturn should be(MaybeEscapeViaParameterAndNormalAndAbnormalReturn)
    }
    s"$EscapeViaParameterAndNormalAndAbnormalReturn meet $MaybeEscapeViaParameter" should s"be $MaybeEscapeViaParameterAndNormalAndAbnormalReturn" in {
        EscapeViaParameterAndNormalAndAbnormalReturn meet MaybeEscapeViaParameter should be(MaybeEscapeViaParameterAndNormalAndAbnormalReturn)
    }
    s"$EscapeViaParameterAndNormalAndAbnormalReturn meet $MaybeEscapeViaParameterAndReturn" should s"be $MaybeEscapeViaParameterAndNormalAndAbnormalReturn" in {
        EscapeViaParameterAndNormalAndAbnormalReturn meet MaybeEscapeViaParameterAndReturn should be(MaybeEscapeViaParameterAndNormalAndAbnormalReturn)
    }
    s"$EscapeViaParameterAndNormalAndAbnormalReturn meet $MaybeEscapeViaParameterAndAbnormalReturn" should s"be $MaybeEscapeViaParameterAndNormalAndAbnormalReturn" in {
        EscapeViaParameterAndNormalAndAbnormalReturn meet MaybeEscapeViaParameterAndAbnormalReturn should be(MaybeEscapeViaParameterAndNormalAndAbnormalReturn)
    }
    s"$EscapeViaParameterAndNormalAndAbnormalReturn meet $MaybeEscapeViaNormalAndAbnormalReturn" should s"be $MaybeEscapeViaParameterAndNormalAndAbnormalReturn" in {
        EscapeViaParameterAndNormalAndAbnormalReturn meet MaybeEscapeViaNormalAndAbnormalReturn should be(MaybeEscapeViaParameterAndNormalAndAbnormalReturn)
    }
    s"$EscapeViaParameterAndNormalAndAbnormalReturn meet $MaybeEscapeViaParameterAndNormalAndAbnormalReturn" should s"be $MaybeEscapeViaParameterAndNormalAndAbnormalReturn" in {
        EscapeViaParameterAndNormalAndAbnormalReturn meet MaybeEscapeViaParameterAndNormalAndAbnormalReturn should be(MaybeEscapeViaParameterAndNormalAndAbnormalReturn)
    }
    s"$EscapeViaParameterAndNormalAndAbnormalReturn meet $GlobalEscape" should s"be $GlobalEscape" in {
        EscapeViaParameterAndNormalAndAbnormalReturn meet GlobalEscape should be(GlobalEscape)
    }


    s"$MaybeEscapeInCallee meet $MaybeEscapeViaReturn" should s"be $MaybeEscapeViaReturn" in {
        MaybeEscapeInCallee meet MaybeEscapeViaReturn should be(MaybeEscapeViaReturn)
    }
    s"$MaybeEscapeInCallee meet $MaybeEscapeViaParameter" should s"be $MaybeEscapeViaParameter" in {
        MaybeEscapeInCallee meet MaybeEscapeViaParameter should be(MaybeEscapeViaParameter)
    }
    s"$MaybeEscapeInCallee meet $MaybeEscapeViaParameterAndReturn" should s"be $MaybeEscapeViaParameterAndReturn" in {
        MaybeEscapeInCallee meet MaybeEscapeViaParameterAndReturn should be(MaybeEscapeViaParameterAndReturn)
    }
    s"$MaybeEscapeInCallee meet $MaybeEscapeViaParameterAndAbnormalReturn" should s"be $MaybeEscapeViaParameterAndAbnormalReturn" in {
        MaybeEscapeInCallee meet MaybeEscapeViaParameterAndAbnormalReturn should be(MaybeEscapeViaParameterAndAbnormalReturn)
    }
    s"$MaybeEscapeInCallee meet $MaybeEscapeViaNormalAndAbnormalReturn" should s"be $MaybeEscapeViaNormalAndAbnormalReturn" in {
        MaybeEscapeInCallee meet MaybeEscapeViaNormalAndAbnormalReturn should be(MaybeEscapeViaNormalAndAbnormalReturn)
    }
    s"$MaybeEscapeInCallee meet $MaybeEscapeViaParameterAndNormalAndAbnormalReturn" should s"be $MaybeEscapeViaParameterAndNormalAndAbnormalReturn" in {
        MaybeEscapeInCallee meet MaybeEscapeViaParameterAndNormalAndAbnormalReturn should be(MaybeEscapeViaParameterAndNormalAndAbnormalReturn)
    }
    s"$MaybeEscapeInCallee meet $GlobalEscape" should s"be $GlobalEscape" in {
        MaybeEscapeInCallee meet GlobalEscape should be(GlobalEscape)
    }


    s"$MaybeEscapeViaReturn meet $MaybeEscapeViaParameter" should s"be $MaybeEscapeViaParameterAndReturn" in {
        MaybeEscapeViaReturn meet MaybeEscapeViaParameter should be(MaybeEscapeViaParameterAndReturn)
    }
    s"$MaybeEscapeViaReturn meet $MaybeEscapeViaParameterAndReturn" should s"be $MaybeEscapeViaParameterAndReturn" in {
        MaybeEscapeViaReturn meet MaybeEscapeViaParameterAndReturn should be(MaybeEscapeViaParameterAndReturn)
    }
    s"$MaybeEscapeViaReturn meet $MaybeEscapeViaParameterAndAbnormalReturn" should s"be $MaybeEscapeViaParameterAndNormalAndAbnormalReturn" in {
        MaybeEscapeViaReturn meet MaybeEscapeViaParameterAndAbnormalReturn should be(MaybeEscapeViaParameterAndNormalAndAbnormalReturn)
    }
    s"$MaybeEscapeViaReturn meet $MaybeEscapeViaNormalAndAbnormalReturn" should s"be $MaybeEscapeViaNormalAndAbnormalReturn" in {
        MaybeEscapeViaReturn meet MaybeEscapeViaNormalAndAbnormalReturn should be(MaybeEscapeViaNormalAndAbnormalReturn)
    }
    s"$MaybeEscapeViaReturn meet $MaybeEscapeViaParameterAndNormalAndAbnormalReturn" should s"be $MaybeEscapeViaParameterAndNormalAndAbnormalReturn" in {
        MaybeEscapeViaReturn meet MaybeEscapeViaParameterAndNormalAndAbnormalReturn should be(MaybeEscapeViaParameterAndNormalAndAbnormalReturn)
    }
    s"$MaybeEscapeViaReturn meet $GlobalEscape" should s"be $GlobalEscape" in {
        MaybeEscapeViaReturn meet GlobalEscape should be(GlobalEscape)
    }


    s"$MaybeEscapeViaParameter meet $MaybeEscapeViaParameterAndReturn" should s"be $MaybeEscapeViaParameterAndReturn" in {
        MaybeEscapeViaParameter meet MaybeEscapeViaParameterAndReturn should be(MaybeEscapeViaParameterAndReturn)
    }
    s"$MaybeEscapeViaParameter meet $MaybeEscapeViaParameterAndAbnormalReturn" should s"be $MaybeEscapeViaParameterAndAbnormalReturn" in {
        MaybeEscapeViaParameter meet MaybeEscapeViaParameterAndAbnormalReturn should be(MaybeEscapeViaParameterAndAbnormalReturn)
    }
    s"$MaybeEscapeViaParameter meet $MaybeEscapeViaNormalAndAbnormalReturn" should s"be $MaybeEscapeViaParameterAndNormalAndAbnormalReturn" in {
        MaybeEscapeViaParameter meet MaybeEscapeViaNormalAndAbnormalReturn should be(MaybeEscapeViaParameterAndNormalAndAbnormalReturn)
    }
    s"$MaybeEscapeViaParameter meet $MaybeEscapeViaParameterAndNormalAndAbnormalReturn" should s"be $MaybeEscapeViaParameterAndNormalAndAbnormalReturn" in {
        MaybeEscapeViaParameter meet MaybeEscapeViaParameterAndNormalAndAbnormalReturn should be(MaybeEscapeViaParameterAndNormalAndAbnormalReturn)
    }
    s"$MaybeEscapeViaParameter meet $GlobalEscape" should s"be $GlobalEscape" in {
        MaybeEscapeViaParameter meet GlobalEscape should be(GlobalEscape)
    }


    s"$MaybeEscapeViaParameterAndReturn meet $MaybeEscapeViaParameterAndAbnormalReturn" should s"be $MaybeEscapeViaParameterAndNormalAndAbnormalReturn" in {
        MaybeEscapeViaParameterAndReturn meet MaybeEscapeViaParameterAndAbnormalReturn should be(MaybeEscapeViaParameterAndNormalAndAbnormalReturn)
    }
    s"$MaybeEscapeViaParameterAndReturn meet $MaybeEscapeViaNormalAndAbnormalReturn" should s"be $MaybeEscapeViaParameterAndNormalAndAbnormalReturn" in {
        MaybeEscapeViaParameterAndReturn meet MaybeEscapeViaNormalAndAbnormalReturn should be(MaybeEscapeViaParameterAndNormalAndAbnormalReturn)
    }
    s"$MaybeEscapeViaParameterAndReturn meet $MaybeEscapeViaParameterAndNormalAndAbnormalReturn" should s"be $MaybeEscapeViaParameterAndNormalAndAbnormalReturn" in {
        MaybeEscapeViaParameterAndReturn meet MaybeEscapeViaParameterAndNormalAndAbnormalReturn should be(MaybeEscapeViaParameterAndNormalAndAbnormalReturn)
    }
    s"$MaybeEscapeViaParameterAndReturn meet $GlobalEscape" should s"be $GlobalEscape" in {
        MaybeEscapeViaParameterAndReturn meet GlobalEscape should be(GlobalEscape)
    }




    s"$MaybeEscapeViaParameterAndAbnormalReturn meet $MaybeEscapeViaNormalAndAbnormalReturn" should s"be $MaybeEscapeViaParameterAndNormalAndAbnormalReturn" in {
        MaybeEscapeViaParameterAndAbnormalReturn meet MaybeEscapeViaNormalAndAbnormalReturn should be(MaybeEscapeViaParameterAndNormalAndAbnormalReturn)
    }
    s"$MaybeEscapeViaParameterAndAbnormalReturn meet $MaybeEscapeViaParameterAndNormalAndAbnormalReturn" should s"be $MaybeEscapeViaParameterAndNormalAndAbnormalReturn" in {
        MaybeEscapeViaParameterAndAbnormalReturn meet MaybeEscapeViaParameterAndNormalAndAbnormalReturn should be(MaybeEscapeViaParameterAndNormalAndAbnormalReturn)
    }
    s"$MaybeEscapeViaParameterAndAbnormalReturn meet $GlobalEscape" should s"be $GlobalEscape" in {
        MaybeEscapeViaParameterAndAbnormalReturn meet GlobalEscape should be(GlobalEscape)
    }


    s"$MaybeEscapeViaNormalAndAbnormalReturn meet $MaybeEscapeViaParameterAndNormalAndAbnormalReturn" should s"be $MaybeEscapeViaParameterAndNormalAndAbnormalReturn" in {
        MaybeEscapeViaNormalAndAbnormalReturn meet MaybeEscapeViaParameterAndNormalAndAbnormalReturn should be(MaybeEscapeViaParameterAndNormalAndAbnormalReturn)
    }
    s"$MaybeEscapeViaNormalAndAbnormalReturn meet $GlobalEscape" should s"be $GlobalEscape" in {
        MaybeEscapeViaNormalAndAbnormalReturn meet GlobalEscape should be(GlobalEscape)
    }


    s"$MaybeEscapeViaParameterAndNormalAndAbnormalReturn meet $GlobalEscape" should s"be $GlobalEscape" in {
        MaybeEscapeViaParameterAndNormalAndAbnormalReturn meet GlobalEscape should be(GlobalEscape)
    }
}

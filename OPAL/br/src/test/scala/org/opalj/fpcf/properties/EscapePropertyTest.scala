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
        EscapeViaParameterAndNormalAndAbnormalReturn, GlobalEscape, EscapeViaStaticField, EscapeViaHeapObject, MaybeNoEscape, MaybeEscapeInCallee,
        MaybeEscapeViaParameter, MaybeEscapeViaReturn, MaybeEscapeViaAbnormalReturn,
        MaybeEscapeViaParameterAndReturn, MaybeEscapeViaParameterAndAbnormalReturn, MaybeEscapeViaNormalAndAbnormalReturn,
        MaybeEscapeViaParameterAndNormalAndAbnormalReturn
    )

    behavior of "the meet operator"

    it should "be symmetric in its arguments, the result should be less or equal" in {
        for (prop1 ← allProperties) {
            for (prop2 ← allProperties) {
                val meet12 = prop1 meet prop2
                val meet21 = prop2 meet prop1
                assert(meet12 eq meet21, s"meet should be symmetric for $prop1 and $prop2")
                assert(meet12 lessOrEqualRestrictive prop1, s"$prop1 meet $prop2 ($meet12) should be lessOrEqualRestrictive than $prop1")
                assert(meet12 lessOrEqualRestrictive prop2, s"$prop1 meet $prop2 ($meet12) should be lessOrEqualRestrictive than $prop2")
            }
        }
    }

    it should "be the identity if both arguments are the same" in {
        for (prop ← allProperties) {
            val m = prop meet prop
            assert(prop meet prop eq prop, s"$prop meet $prop should be $prop but was $m")
        }
    }

    it should "evaluate to the correct result" in {


        assert(NoEscape meet EscapeInCallee eq EscapeInCallee, s"$NoEscape meet $EscapeInCallee should be $EscapeInCallee")
        assert(NoEscape meet EscapeViaReturn eq EscapeViaReturn, s"$NoEscape meet $EscapeViaReturn should be$EscapeViaReturn")
        assert(NoEscape meet EscapeViaParameter eq EscapeViaParameter, s"$NoEscape meet $EscapeViaParameter should be$EscapeViaParameter")
        assert(NoEscape meet EscapeViaAbnormalReturn eq EscapeViaAbnormalReturn, s"$NoEscape meet $EscapeViaAbnormalReturn should be$EscapeViaAbnormalReturn")
        assert(NoEscape meet EscapeViaParameterAndReturn eq EscapeViaParameterAndReturn, s"$NoEscape meet $EscapeViaParameterAndReturn should be$EscapeViaParameterAndReturn")
        assert(NoEscape meet EscapeViaParameterAndAbnormalReturn eq EscapeViaParameterAndAbnormalReturn, s"$NoEscape meet $EscapeViaParameterAndAbnormalReturn should be$EscapeViaParameterAndAbnormalReturn")
        assert(NoEscape meet EscapeViaNormalAndAbnormalReturn eq EscapeViaNormalAndAbnormalReturn, s"$NoEscape meet $EscapeViaNormalAndAbnormalReturn should be$EscapeViaNormalAndAbnormalReturn")
        assert(NoEscape meet EscapeViaParameterAndNormalAndAbnormalReturn eq EscapeViaParameterAndNormalAndAbnormalReturn, s"$NoEscape meet $EscapeViaParameterAndNormalAndAbnormalReturn should be$EscapeViaParameterAndNormalAndAbnormalReturn")
        assert(NoEscape meet MaybeEscapeInCallee eq MaybeEscapeInCallee, s"$NoEscape meet $MaybeEscapeInCallee should be$MaybeEscapeInCallee")
        assert(NoEscape meet MaybeEscapeViaReturn eq MaybeEscapeViaReturn, s"$NoEscape meet $MaybeEscapeViaReturn should be$MaybeEscapeViaReturn")
        assert(NoEscape meet MaybeEscapeViaParameter eq MaybeEscapeViaParameter, s"$NoEscape meet $MaybeEscapeViaParameter should be$MaybeEscapeViaParameter")
        assert(NoEscape meet MaybeEscapeViaParameterAndReturn eq MaybeEscapeViaParameterAndReturn, s"$NoEscape meet $MaybeEscapeViaParameterAndReturn should be$MaybeEscapeViaParameterAndReturn")
        assert(NoEscape meet MaybeEscapeViaParameterAndAbnormalReturn eq MaybeEscapeViaParameterAndAbnormalReturn, s"$NoEscape meet $MaybeEscapeViaParameterAndAbnormalReturn should be$MaybeEscapeViaParameterAndAbnormalReturn")
        assert(NoEscape meet MaybeEscapeViaNormalAndAbnormalReturn eq MaybeEscapeViaNormalAndAbnormalReturn, s"$NoEscape meet $MaybeEscapeViaNormalAndAbnormalReturn should be$MaybeEscapeViaNormalAndAbnormalReturn")
        assert(NoEscape meet MaybeEscapeViaParameterAndNormalAndAbnormalReturn eq MaybeEscapeViaParameterAndNormalAndAbnormalReturn, s"$NoEscape meet $MaybeEscapeViaParameterAndNormalAndAbnormalReturn should be$MaybeEscapeViaParameterAndNormalAndAbnormalReturn")
        assert(NoEscape meet GlobalEscape eq GlobalEscape, s"$NoEscape meet $GlobalEscape should be$GlobalEscape")
        assert(NoEscape meet EscapeViaStaticField eq EscapeViaStaticField, s"$NoEscape meet $EscapeViaStaticField should be$EscapeViaStaticField")
        assert(NoEscape meet EscapeViaHeapObject eq EscapeViaHeapObject, s"$NoEscape meet $EscapeViaHeapObject should be$EscapeViaHeapObject")

        assert(EscapeInCallee meet EscapeViaReturn eq EscapeViaReturn, s"$EscapeInCallee meet $EscapeViaReturn should be$EscapeViaReturn")
        assert(EscapeInCallee meet EscapeViaParameter eq EscapeViaParameter, s"$EscapeInCallee meet $EscapeViaParameter should be$EscapeViaParameter")
        assert(EscapeInCallee meet EscapeViaAbnormalReturn eq EscapeViaAbnormalReturn, s"$EscapeInCallee meet $EscapeViaAbnormalReturn should be$EscapeViaAbnormalReturn")
        assert(EscapeInCallee meet EscapeViaParameterAndReturn eq EscapeViaParameterAndReturn, s"$EscapeInCallee meet $EscapeViaParameterAndReturn should be$EscapeViaParameterAndReturn")
        assert(EscapeInCallee meet EscapeViaParameterAndAbnormalReturn eq EscapeViaParameterAndAbnormalReturn, s"$EscapeInCallee meet $EscapeViaParameterAndAbnormalReturn should be$EscapeViaParameterAndAbnormalReturn")
        assert(EscapeInCallee meet EscapeViaNormalAndAbnormalReturn eq EscapeViaNormalAndAbnormalReturn, s"$EscapeInCallee meet $EscapeViaNormalAndAbnormalReturn should be$EscapeViaNormalAndAbnormalReturn")
        assert(EscapeInCallee meet EscapeViaParameterAndNormalAndAbnormalReturn eq EscapeViaParameterAndNormalAndAbnormalReturn, s"$EscapeInCallee meet $EscapeViaParameterAndNormalAndAbnormalReturn should be$EscapeViaParameterAndNormalAndAbnormalReturn")
        assert(EscapeInCallee meet MaybeEscapeInCallee eq MaybeEscapeInCallee, s"$EscapeInCallee meet $MaybeEscapeInCallee should be$MaybeEscapeInCallee")
        assert(EscapeInCallee meet MaybeEscapeViaReturn eq MaybeEscapeViaReturn, s"$EscapeInCallee meet $MaybeEscapeViaReturn should be$MaybeEscapeViaReturn")
        assert(EscapeInCallee meet MaybeEscapeViaParameter eq MaybeEscapeViaParameter, s"$EscapeInCallee meet $MaybeEscapeViaParameter should be$MaybeEscapeViaParameter")
        assert(EscapeInCallee meet MaybeEscapeViaParameterAndReturn eq MaybeEscapeViaParameterAndReturn, s"$EscapeInCallee meet $MaybeEscapeViaParameterAndReturn should be$MaybeEscapeViaParameterAndReturn")
        assert(EscapeInCallee meet MaybeEscapeViaParameterAndAbnormalReturn eq MaybeEscapeViaParameterAndAbnormalReturn, s"$EscapeInCallee meet $MaybeEscapeViaParameterAndAbnormalReturn should be$MaybeEscapeViaParameterAndAbnormalReturn")
        assert(EscapeInCallee meet MaybeEscapeViaNormalAndAbnormalReturn eq MaybeEscapeViaNormalAndAbnormalReturn, s"$EscapeInCallee meet $MaybeEscapeViaNormalAndAbnormalReturn should be$MaybeEscapeViaNormalAndAbnormalReturn")
        assert(EscapeInCallee meet MaybeEscapeViaParameterAndNormalAndAbnormalReturn eq MaybeEscapeViaParameterAndNormalAndAbnormalReturn, s"$EscapeInCallee meet $MaybeEscapeViaParameterAndNormalAndAbnormalReturn should be$MaybeEscapeViaParameterAndNormalAndAbnormalReturn")
        assert(EscapeInCallee meet GlobalEscape eq GlobalEscape, s"$EscapeInCallee meet $GlobalEscape should be$GlobalEscape")
        assert(EscapeInCallee meet EscapeViaStaticField eq EscapeViaStaticField, s"$EscapeInCallee meet $EscapeViaStaticField should be$EscapeViaStaticField")
        assert(EscapeInCallee meet EscapeViaHeapObject eq EscapeViaHeapObject, s"$EscapeInCallee meet $EscapeViaHeapObject should be$EscapeViaHeapObject")

        assert(EscapeViaReturn meet EscapeViaParameter eq EscapeViaParameterAndReturn, s"$EscapeViaReturn meet $EscapeViaParameter should be$EscapeViaParameterAndReturn")
        assert(EscapeViaReturn meet EscapeViaAbnormalReturn eq EscapeViaNormalAndAbnormalReturn, s"$EscapeViaReturn meet $EscapeViaAbnormalReturn should be$EscapeViaNormalAndAbnormalReturn")
        assert(EscapeViaReturn meet EscapeViaParameterAndReturn eq EscapeViaParameterAndReturn, s"$EscapeViaReturn meet $EscapeViaParameterAndReturn should be$EscapeViaParameterAndReturn")
        assert(EscapeViaReturn meet EscapeViaParameterAndAbnormalReturn eq EscapeViaParameterAndNormalAndAbnormalReturn, s"$EscapeViaReturn meet $EscapeViaParameterAndAbnormalReturn should be$EscapeViaParameterAndNormalAndAbnormalReturn")
        assert(EscapeViaReturn meet EscapeViaNormalAndAbnormalReturn eq EscapeViaNormalAndAbnormalReturn, s"$EscapeViaReturn meet $EscapeViaNormalAndAbnormalReturn should be$EscapeViaNormalAndAbnormalReturn")
        assert(EscapeViaReturn meet EscapeViaParameterAndNormalAndAbnormalReturn eq EscapeViaParameterAndNormalAndAbnormalReturn, s"$EscapeViaReturn meet $EscapeViaParameterAndNormalAndAbnormalReturn should be$EscapeViaParameterAndNormalAndAbnormalReturn")
        assert(EscapeViaReturn meet MaybeEscapeInCallee eq MaybeEscapeViaReturn, s"$EscapeViaReturn meet $MaybeEscapeInCallee should be$MaybeEscapeViaReturn")
        assert(EscapeViaReturn meet MaybeEscapeViaReturn eq MaybeEscapeViaReturn, s"$EscapeViaReturn meet $MaybeEscapeViaReturn should be$MaybeEscapeViaReturn")
        assert(EscapeViaReturn meet MaybeEscapeViaParameter eq MaybeEscapeViaParameterAndReturn, s"$EscapeViaReturn meet $MaybeEscapeViaParameter should be$MaybeEscapeViaParameterAndReturn")
        assert(EscapeViaReturn meet MaybeEscapeViaParameterAndReturn eq MaybeEscapeViaParameterAndReturn, s"$EscapeViaReturn meet $MaybeEscapeViaParameterAndReturn should be$MaybeEscapeViaParameterAndReturn")
        assert(EscapeViaReturn meet MaybeEscapeViaParameterAndAbnormalReturn eq MaybeEscapeViaParameterAndNormalAndAbnormalReturn, s"$EscapeViaReturn meet $MaybeEscapeViaParameterAndAbnormalReturn should be$MaybeEscapeViaParameterAndNormalAndAbnormalReturn")
        assert(EscapeViaReturn meet MaybeEscapeViaNormalAndAbnormalReturn eq MaybeEscapeViaNormalAndAbnormalReturn, s"$EscapeViaReturn meet $MaybeEscapeViaNormalAndAbnormalReturn should be$MaybeEscapeViaNormalAndAbnormalReturn")
        assert(EscapeViaReturn meet MaybeEscapeViaParameterAndNormalAndAbnormalReturn eq MaybeEscapeViaParameterAndNormalAndAbnormalReturn, s"$EscapeViaReturn meet $MaybeEscapeViaParameterAndNormalAndAbnormalReturn should be$MaybeEscapeViaParameterAndNormalAndAbnormalReturn")
        assert(EscapeViaReturn meet GlobalEscape eq GlobalEscape, s"$EscapeViaReturn meet $GlobalEscape should be$GlobalEscape")
        assert(EscapeViaReturn meet EscapeViaStaticField eq EscapeViaStaticField, s"$EscapeViaReturn meet $EscapeViaStaticField should be$EscapeViaStaticField")
        assert(EscapeViaReturn meet EscapeViaHeapObject eq EscapeViaHeapObject, s"$EscapeViaReturn meet $EscapeViaHeapObject should be$EscapeViaHeapObject")

        assert(EscapeViaParameter meet EscapeViaAbnormalReturn eq EscapeViaParameterAndAbnormalReturn, s"$EscapeViaParameter meet $EscapeViaAbnormalReturn should be$EscapeViaParameterAndAbnormalReturn")
        assert(EscapeViaParameter meet EscapeViaParameterAndReturn eq EscapeViaParameterAndReturn, s"$EscapeViaParameter meet $EscapeViaParameterAndReturn should be$EscapeViaParameterAndReturn")
        assert(EscapeViaParameter meet EscapeViaParameterAndAbnormalReturn eq EscapeViaParameterAndAbnormalReturn, s"$EscapeViaParameter meet $EscapeViaParameterAndAbnormalReturn should be$EscapeViaParameterAndAbnormalReturn")
        assert(EscapeViaParameter meet EscapeViaNormalAndAbnormalReturn eq EscapeViaParameterAndNormalAndAbnormalReturn, s"$EscapeViaParameter meet $EscapeViaNormalAndAbnormalReturn should be$EscapeViaParameterAndNormalAndAbnormalReturn")
        assert(EscapeViaParameter meet EscapeViaParameterAndNormalAndAbnormalReturn eq EscapeViaParameterAndNormalAndAbnormalReturn, s"$EscapeViaParameter meet $EscapeViaParameterAndNormalAndAbnormalReturn should be$EscapeViaParameterAndNormalAndAbnormalReturn")
        assert(EscapeViaParameter meet MaybeEscapeInCallee eq MaybeEscapeViaParameter, s"$EscapeViaParameter meet $MaybeEscapeInCallee should be$MaybeEscapeViaParameter")
        assert(EscapeViaParameter meet MaybeEscapeViaReturn eq MaybeEscapeViaParameterAndReturn, s"$EscapeViaParameter meet $MaybeEscapeViaReturn should be$MaybeEscapeViaParameterAndReturn")
        assert(EscapeViaParameter meet MaybeEscapeViaParameter eq MaybeEscapeViaParameter, s"$EscapeViaParameter meet $MaybeEscapeViaParameter should be$MaybeEscapeViaParameter")
        assert(EscapeViaParameter meet MaybeEscapeViaParameterAndReturn eq MaybeEscapeViaParameterAndReturn, s"$EscapeViaParameter meet $MaybeEscapeViaParameterAndReturn should be$MaybeEscapeViaParameterAndReturn")
        assert(EscapeViaParameter meet MaybeEscapeViaParameterAndAbnormalReturn eq MaybeEscapeViaParameterAndAbnormalReturn, s"$EscapeViaParameter meet $MaybeEscapeViaParameterAndAbnormalReturn should be$MaybeEscapeViaParameterAndAbnormalReturn")
        assert(EscapeViaParameter meet MaybeEscapeViaNormalAndAbnormalReturn eq MaybeEscapeViaParameterAndNormalAndAbnormalReturn, s"$EscapeViaParameter meet $MaybeEscapeViaNormalAndAbnormalReturn should be$MaybeEscapeViaParameterAndNormalAndAbnormalReturn")
        assert(EscapeViaParameter meet MaybeEscapeViaParameterAndNormalAndAbnormalReturn eq MaybeEscapeViaParameterAndNormalAndAbnormalReturn, s"$EscapeViaParameter meet $MaybeEscapeViaParameterAndNormalAndAbnormalReturn should be$MaybeEscapeViaParameterAndNormalAndAbnormalReturn")
        assert(EscapeViaParameter meet GlobalEscape eq GlobalEscape, s"$EscapeViaParameter meet $GlobalEscape should be$GlobalEscape")
        assert(EscapeViaParameter meet EscapeViaStaticField eq EscapeViaStaticField, s"$EscapeViaParameter meet $EscapeViaStaticField should be$EscapeViaStaticField")
        assert(EscapeViaParameter meet EscapeViaHeapObject eq EscapeViaHeapObject, s"$EscapeViaParameter meet $EscapeViaHeapObject should be$EscapeViaHeapObject")

        assert(EscapeViaAbnormalReturn meet EscapeViaParameterAndReturn eq EscapeViaParameterAndNormalAndAbnormalReturn, s"$EscapeViaAbnormalReturn meet $EscapeViaParameterAndReturn should be$EscapeViaParameterAndNormalAndAbnormalReturn")
        assert(EscapeViaAbnormalReturn meet EscapeViaParameterAndAbnormalReturn eq EscapeViaParameterAndAbnormalReturn, s"$EscapeViaAbnormalReturn meet $EscapeViaParameterAndAbnormalReturn should be$EscapeViaParameterAndAbnormalReturn")
        assert(EscapeViaAbnormalReturn meet EscapeViaNormalAndAbnormalReturn eq EscapeViaNormalAndAbnormalReturn, s"$EscapeViaAbnormalReturn meet $EscapeViaNormalAndAbnormalReturn should be$EscapeViaNormalAndAbnormalReturn")
        assert(EscapeViaAbnormalReturn meet EscapeViaParameterAndNormalAndAbnormalReturn eq EscapeViaParameterAndNormalAndAbnormalReturn, s"$EscapeViaAbnormalReturn meet $EscapeViaParameterAndNormalAndAbnormalReturn should be$EscapeViaParameterAndNormalAndAbnormalReturn")
        assert(EscapeViaAbnormalReturn meet MaybeEscapeInCallee eq MaybeEscapeViaAbnormalReturn, s"$EscapeViaAbnormalReturn meet $MaybeEscapeInCallee should be$MaybeEscapeViaAbnormalReturn")
        assert(EscapeViaAbnormalReturn meet MaybeEscapeViaReturn eq MaybeEscapeViaNormalAndAbnormalReturn, s"$EscapeViaAbnormalReturn meet $MaybeEscapeViaReturn should be$MaybeEscapeViaNormalAndAbnormalReturn")
        assert(EscapeViaAbnormalReturn meet MaybeEscapeViaParameter eq MaybeEscapeViaParameterAndAbnormalReturn, s"$EscapeViaAbnormalReturn meet $MaybeEscapeViaParameter should be$MaybeEscapeViaParameterAndAbnormalReturn")
        assert(EscapeViaAbnormalReturn meet MaybeEscapeViaParameterAndReturn eq MaybeEscapeViaParameterAndNormalAndAbnormalReturn, s"$EscapeViaAbnormalReturn meet $MaybeEscapeViaParameterAndReturn should be$MaybeEscapeViaParameterAndNormalAndAbnormalReturn")
        assert(EscapeViaAbnormalReturn meet MaybeEscapeViaParameterAndAbnormalReturn eq MaybeEscapeViaParameterAndAbnormalReturn, s"$EscapeViaAbnormalReturn meet $MaybeEscapeViaParameterAndAbnormalReturn should be$MaybeEscapeViaParameterAndAbnormalReturn")
        assert(EscapeViaAbnormalReturn meet MaybeEscapeViaNormalAndAbnormalReturn eq MaybeEscapeViaNormalAndAbnormalReturn, s"$EscapeViaAbnormalReturn meet $MaybeEscapeViaNormalAndAbnormalReturn should be$MaybeEscapeViaNormalAndAbnormalReturn")
        assert(EscapeViaAbnormalReturn meet MaybeEscapeViaParameterAndNormalAndAbnormalReturn eq MaybeEscapeViaParameterAndNormalAndAbnormalReturn, s"$EscapeViaAbnormalReturn meet $MaybeEscapeViaParameterAndNormalAndAbnormalReturn should be$MaybeEscapeViaParameterAndNormalAndAbnormalReturn")
        assert(EscapeViaAbnormalReturn meet GlobalEscape eq GlobalEscape, s"$EscapeViaAbnormalReturn meet $GlobalEscape should be$GlobalEscape")
        assert(EscapeViaAbnormalReturn meet EscapeViaStaticField eq EscapeViaStaticField, s"$EscapeViaAbnormalReturn meet $EscapeViaStaticField should be$EscapeViaStaticField")
        assert(EscapeViaAbnormalReturn meet EscapeViaHeapObject eq EscapeViaHeapObject, s"$EscapeViaAbnormalReturn meet $EscapeViaHeapObject should be$EscapeViaHeapObject")

        assert(EscapeViaParameterAndReturn meet EscapeViaParameterAndAbnormalReturn eq EscapeViaParameterAndNormalAndAbnormalReturn, s"$EscapeViaParameterAndReturn meet $EscapeViaParameterAndAbnormalReturn should be$EscapeViaParameterAndNormalAndAbnormalReturn")
        assert(EscapeViaParameterAndReturn meet EscapeViaNormalAndAbnormalReturn eq EscapeViaParameterAndNormalAndAbnormalReturn, s"$EscapeViaParameterAndReturn meet $EscapeViaNormalAndAbnormalReturn should be$EscapeViaParameterAndNormalAndAbnormalReturn")
        assert(EscapeViaParameterAndReturn meet EscapeViaParameterAndNormalAndAbnormalReturn eq EscapeViaParameterAndNormalAndAbnormalReturn, s"$EscapeViaParameterAndReturn meet $EscapeViaParameterAndNormalAndAbnormalReturn should be$EscapeViaParameterAndNormalAndAbnormalReturn")
        assert(EscapeViaParameterAndReturn meet MaybeEscapeInCallee eq MaybeEscapeViaParameterAndReturn, s"$EscapeViaParameterAndReturn meet $MaybeEscapeInCallee should be$MaybeEscapeViaParameterAndReturn")
        assert(EscapeViaParameterAndReturn meet MaybeEscapeViaReturn eq MaybeEscapeViaParameterAndReturn, s"$EscapeViaParameterAndReturn meet $MaybeEscapeViaReturn should be$MaybeEscapeViaParameterAndReturn")
        assert(EscapeViaParameterAndReturn meet MaybeEscapeViaParameter eq MaybeEscapeViaParameterAndReturn, s"$EscapeViaParameterAndReturn meet $MaybeEscapeViaParameter should be$MaybeEscapeViaParameterAndReturn")
        assert(EscapeViaParameterAndReturn meet MaybeEscapeViaParameterAndReturn eq MaybeEscapeViaParameterAndReturn, s"$EscapeViaParameterAndReturn meet $MaybeEscapeViaParameterAndReturn should be$MaybeEscapeViaParameterAndReturn")
        assert(EscapeViaParameterAndReturn meet MaybeEscapeViaParameterAndAbnormalReturn eq MaybeEscapeViaParameterAndNormalAndAbnormalReturn, s"$EscapeViaParameterAndReturn meet $MaybeEscapeViaParameterAndAbnormalReturn should be$MaybeEscapeViaParameterAndNormalAndAbnormalReturn")
        assert(EscapeViaParameterAndReturn meet MaybeEscapeViaNormalAndAbnormalReturn eq MaybeEscapeViaParameterAndNormalAndAbnormalReturn, s"$EscapeViaParameterAndReturn meet $MaybeEscapeViaNormalAndAbnormalReturn should be$MaybeEscapeViaParameterAndNormalAndAbnormalReturn")
        assert(EscapeViaParameterAndReturn meet MaybeEscapeViaParameterAndNormalAndAbnormalReturn eq MaybeEscapeViaParameterAndNormalAndAbnormalReturn, s"$EscapeViaParameterAndReturn meet $MaybeEscapeViaParameterAndNormalAndAbnormalReturn should be$MaybeEscapeViaParameterAndNormalAndAbnormalReturn")
        assert(EscapeViaParameterAndReturn meet GlobalEscape eq GlobalEscape, s"$EscapeViaParameterAndReturn meet $GlobalEscape should be$GlobalEscape")
        assert(EscapeViaParameterAndReturn meet EscapeViaStaticField eq EscapeViaStaticField, s"$EscapeViaParameterAndReturn meet $EscapeViaStaticField should be$EscapeViaStaticField")
        assert(EscapeViaParameterAndReturn meet EscapeViaHeapObject eq EscapeViaHeapObject, s"$EscapeViaParameterAndReturn meet $EscapeViaHeapObject should be$EscapeViaHeapObject")

        assert(EscapeViaParameterAndAbnormalReturn meet EscapeViaNormalAndAbnormalReturn eq EscapeViaParameterAndNormalAndAbnormalReturn, s"$EscapeViaParameterAndAbnormalReturn meet $EscapeViaNormalAndAbnormalReturn should be$EscapeViaParameterAndNormalAndAbnormalReturn")
        assert(EscapeViaParameterAndAbnormalReturn meet EscapeViaParameterAndNormalAndAbnormalReturn eq EscapeViaParameterAndNormalAndAbnormalReturn, s"$EscapeViaParameterAndAbnormalReturn meet $EscapeViaParameterAndNormalAndAbnormalReturn should be$EscapeViaParameterAndNormalAndAbnormalReturn")
        assert(EscapeViaParameterAndAbnormalReturn meet MaybeEscapeInCallee eq MaybeEscapeViaParameterAndAbnormalReturn, s"$EscapeViaParameterAndAbnormalReturn meet $MaybeEscapeInCallee should be$MaybeEscapeViaParameterAndAbnormalReturn")
        assert(EscapeViaParameterAndAbnormalReturn meet MaybeEscapeViaReturn eq MaybeEscapeViaParameterAndNormalAndAbnormalReturn, s"$EscapeViaParameterAndAbnormalReturn meet $MaybeEscapeViaReturn should be$MaybeEscapeViaParameterAndNormalAndAbnormalReturn")
        assert(EscapeViaParameterAndAbnormalReturn meet MaybeEscapeViaParameter eq MaybeEscapeViaParameterAndAbnormalReturn, s"$EscapeViaParameterAndAbnormalReturn meet $MaybeEscapeViaParameter should be$MaybeEscapeViaParameterAndAbnormalReturn")
        assert(EscapeViaParameterAndAbnormalReturn meet MaybeEscapeViaParameterAndReturn eq MaybeEscapeViaParameterAndNormalAndAbnormalReturn, s"$EscapeViaParameterAndAbnormalReturn meet $MaybeEscapeViaParameterAndReturn should be$MaybeEscapeViaParameterAndNormalAndAbnormalReturn")
        assert(EscapeViaParameterAndAbnormalReturn meet MaybeEscapeViaParameterAndAbnormalReturn eq MaybeEscapeViaParameterAndAbnormalReturn, s"$EscapeViaParameterAndAbnormalReturn meet $MaybeEscapeViaParameterAndAbnormalReturn should be$MaybeEscapeViaParameterAndAbnormalReturn")
        assert(EscapeViaParameterAndAbnormalReturn meet MaybeEscapeViaNormalAndAbnormalReturn eq MaybeEscapeViaParameterAndNormalAndAbnormalReturn, s"$EscapeViaParameterAndAbnormalReturn meet $MaybeEscapeViaNormalAndAbnormalReturn should be$MaybeEscapeViaParameterAndNormalAndAbnormalReturn")
        assert(EscapeViaParameterAndAbnormalReturn meet MaybeEscapeViaParameterAndNormalAndAbnormalReturn eq MaybeEscapeViaParameterAndNormalAndAbnormalReturn, s"$EscapeViaParameterAndAbnormalReturn meet $MaybeEscapeViaParameterAndNormalAndAbnormalReturn should be$MaybeEscapeViaParameterAndNormalAndAbnormalReturn")
        assert(EscapeViaParameterAndAbnormalReturn meet GlobalEscape eq GlobalEscape, s"$EscapeViaParameterAndAbnormalReturn meet $GlobalEscape should be$GlobalEscape")
        assert(EscapeViaParameterAndAbnormalReturn meet EscapeViaStaticField eq EscapeViaStaticField, s"$EscapeViaParameterAndAbnormalReturn meet $EscapeViaStaticField should be$EscapeViaStaticField")
        assert(EscapeViaParameterAndAbnormalReturn meet EscapeViaHeapObject eq EscapeViaHeapObject, s"$EscapeViaParameterAndAbnormalReturn meet $EscapeViaHeapObject should be$EscapeViaHeapObject")

        assert(EscapeViaNormalAndAbnormalReturn meet EscapeViaParameterAndNormalAndAbnormalReturn eq EscapeViaParameterAndNormalAndAbnormalReturn, s"$EscapeViaNormalAndAbnormalReturn meet $EscapeViaParameterAndNormalAndAbnormalReturn should be$EscapeViaParameterAndNormalAndAbnormalReturn")
        assert(EscapeViaNormalAndAbnormalReturn meet MaybeEscapeInCallee eq MaybeEscapeViaNormalAndAbnormalReturn, s"$EscapeViaNormalAndAbnormalReturn meet $MaybeEscapeInCallee should be$MaybeEscapeViaNormalAndAbnormalReturn")
        assert(EscapeViaNormalAndAbnormalReturn meet MaybeEscapeViaReturn eq MaybeEscapeViaNormalAndAbnormalReturn, s"$EscapeViaNormalAndAbnormalReturn meet $MaybeEscapeViaReturn should be$MaybeEscapeViaNormalAndAbnormalReturn")
        assert(EscapeViaNormalAndAbnormalReturn meet MaybeEscapeViaParameter eq MaybeEscapeViaParameterAndNormalAndAbnormalReturn, s"$EscapeViaNormalAndAbnormalReturn meet $MaybeEscapeViaParameter should be$MaybeEscapeViaParameterAndNormalAndAbnormalReturn")
        assert(EscapeViaNormalAndAbnormalReturn meet MaybeEscapeViaParameterAndReturn eq MaybeEscapeViaParameterAndNormalAndAbnormalReturn, s"$EscapeViaNormalAndAbnormalReturn meet $MaybeEscapeViaParameterAndReturn should be$MaybeEscapeViaParameterAndNormalAndAbnormalReturn")
        assert(EscapeViaNormalAndAbnormalReturn meet MaybeEscapeViaParameterAndAbnormalReturn eq MaybeEscapeViaParameterAndNormalAndAbnormalReturn, s"$EscapeViaNormalAndAbnormalReturn meet $MaybeEscapeViaParameterAndAbnormalReturn should be$MaybeEscapeViaParameterAndNormalAndAbnormalReturn")
        assert(EscapeViaNormalAndAbnormalReturn meet MaybeEscapeViaNormalAndAbnormalReturn eq MaybeEscapeViaNormalAndAbnormalReturn, s"$EscapeViaNormalAndAbnormalReturn meet $MaybeEscapeViaNormalAndAbnormalReturn should be$MaybeEscapeViaNormalAndAbnormalReturn")
        assert(EscapeViaNormalAndAbnormalReturn meet MaybeEscapeViaParameterAndNormalAndAbnormalReturn eq MaybeEscapeViaParameterAndNormalAndAbnormalReturn, s"$EscapeViaNormalAndAbnormalReturn meet $MaybeEscapeViaParameterAndNormalAndAbnormalReturn should be$MaybeEscapeViaParameterAndNormalAndAbnormalReturn")
        assert(EscapeViaNormalAndAbnormalReturn meet GlobalEscape eq GlobalEscape, s"$EscapeViaNormalAndAbnormalReturn meet $GlobalEscape should be$GlobalEscape")
        assert(EscapeViaNormalAndAbnormalReturn meet EscapeViaStaticField eq EscapeViaStaticField, s"$EscapeViaNormalAndAbnormalReturn meet $EscapeViaStaticField should be$EscapeViaStaticField")
        assert(EscapeViaNormalAndAbnormalReturn meet EscapeViaHeapObject eq EscapeViaHeapObject, s"$EscapeViaNormalAndAbnormalReturn meet $EscapeViaHeapObject should be$EscapeViaHeapObject")

        assert(EscapeViaParameterAndNormalAndAbnormalReturn meet MaybeEscapeInCallee eq MaybeEscapeViaParameterAndNormalAndAbnormalReturn, s"$EscapeViaParameterAndNormalAndAbnormalReturn meet $MaybeEscapeInCallee should be$MaybeEscapeViaParameterAndNormalAndAbnormalReturn")
        assert(EscapeViaParameterAndNormalAndAbnormalReturn meet MaybeEscapeViaReturn eq MaybeEscapeViaParameterAndNormalAndAbnormalReturn, s"$EscapeViaParameterAndNormalAndAbnormalReturn meet $MaybeEscapeViaReturn should be$MaybeEscapeViaParameterAndNormalAndAbnormalReturn")
        assert(EscapeViaParameterAndNormalAndAbnormalReturn meet MaybeEscapeViaParameter eq MaybeEscapeViaParameterAndNormalAndAbnormalReturn, s"$EscapeViaParameterAndNormalAndAbnormalReturn meet $MaybeEscapeViaParameter should be$MaybeEscapeViaParameterAndNormalAndAbnormalReturn")
        assert(EscapeViaParameterAndNormalAndAbnormalReturn meet MaybeEscapeViaParameterAndReturn eq MaybeEscapeViaParameterAndNormalAndAbnormalReturn, s"$EscapeViaParameterAndNormalAndAbnormalReturn meet $MaybeEscapeViaParameterAndReturn should be$MaybeEscapeViaParameterAndNormalAndAbnormalReturn")
        assert(EscapeViaParameterAndNormalAndAbnormalReturn meet MaybeEscapeViaParameterAndAbnormalReturn eq MaybeEscapeViaParameterAndNormalAndAbnormalReturn, s"$EscapeViaParameterAndNormalAndAbnormalReturn meet $MaybeEscapeViaParameterAndAbnormalReturn should be$MaybeEscapeViaParameterAndNormalAndAbnormalReturn")
        assert(EscapeViaParameterAndNormalAndAbnormalReturn meet MaybeEscapeViaNormalAndAbnormalReturn eq MaybeEscapeViaParameterAndNormalAndAbnormalReturn, s"$EscapeViaParameterAndNormalAndAbnormalReturn meet $MaybeEscapeViaNormalAndAbnormalReturn should be$MaybeEscapeViaParameterAndNormalAndAbnormalReturn")
        assert(EscapeViaParameterAndNormalAndAbnormalReturn meet MaybeEscapeViaParameterAndNormalAndAbnormalReturn eq MaybeEscapeViaParameterAndNormalAndAbnormalReturn, s"$EscapeViaParameterAndNormalAndAbnormalReturn meet $MaybeEscapeViaParameterAndNormalAndAbnormalReturn should be$MaybeEscapeViaParameterAndNormalAndAbnormalReturn")
        assert(EscapeViaParameterAndNormalAndAbnormalReturn meet GlobalEscape eq GlobalEscape, s"$EscapeViaParameterAndNormalAndAbnormalReturn meet $GlobalEscape should be$GlobalEscape")
        assert(EscapeViaParameterAndNormalAndAbnormalReturn meet EscapeViaStaticField eq EscapeViaStaticField, s"$EscapeViaParameterAndNormalAndAbnormalReturn meet $EscapeViaStaticField should be$EscapeViaStaticField")
        assert(EscapeViaParameterAndNormalAndAbnormalReturn meet EscapeViaHeapObject eq EscapeViaHeapObject, s"$EscapeViaParameterAndNormalAndAbnormalReturn meet $EscapeViaHeapObject should be$EscapeViaHeapObject")

        assert(MaybeEscapeInCallee meet MaybeEscapeViaReturn eq MaybeEscapeViaReturn, s"$MaybeEscapeInCallee meet $MaybeEscapeViaReturn should be$MaybeEscapeViaReturn")
        assert(MaybeEscapeInCallee meet MaybeEscapeViaParameter eq MaybeEscapeViaParameter, s"$MaybeEscapeInCallee meet $MaybeEscapeViaParameter should be$MaybeEscapeViaParameter")
        assert(MaybeEscapeInCallee meet MaybeEscapeViaParameterAndReturn eq MaybeEscapeViaParameterAndReturn, s"$MaybeEscapeInCallee meet $MaybeEscapeViaParameterAndReturn should be$MaybeEscapeViaParameterAndReturn")
        assert(MaybeEscapeInCallee meet MaybeEscapeViaParameterAndAbnormalReturn eq MaybeEscapeViaParameterAndAbnormalReturn, s"$MaybeEscapeInCallee meet $MaybeEscapeViaParameterAndAbnormalReturn should be$MaybeEscapeViaParameterAndAbnormalReturn")
        assert(MaybeEscapeInCallee meet MaybeEscapeViaNormalAndAbnormalReturn eq MaybeEscapeViaNormalAndAbnormalReturn, s"$MaybeEscapeInCallee meet $MaybeEscapeViaNormalAndAbnormalReturn should be$MaybeEscapeViaNormalAndAbnormalReturn")
        assert(MaybeEscapeInCallee meet MaybeEscapeViaParameterAndNormalAndAbnormalReturn eq MaybeEscapeViaParameterAndNormalAndAbnormalReturn, s"$MaybeEscapeInCallee meet $MaybeEscapeViaParameterAndNormalAndAbnormalReturn should be$MaybeEscapeViaParameterAndNormalAndAbnormalReturn")
        assert(MaybeEscapeInCallee meet GlobalEscape eq GlobalEscape, s"$MaybeEscapeInCallee meet $GlobalEscape should be$GlobalEscape")
        assert(MaybeEscapeInCallee meet EscapeViaStaticField eq EscapeViaStaticField, s"$MaybeEscapeInCallee meet $EscapeViaStaticField should be$EscapeViaStaticField")
        assert(MaybeEscapeInCallee meet EscapeViaHeapObject eq EscapeViaHeapObject, s"$MaybeEscapeInCallee meet $EscapeViaHeapObject should be$EscapeViaHeapObject")

        assert(MaybeEscapeViaReturn meet MaybeEscapeViaParameter eq MaybeEscapeViaParameterAndReturn, s"$MaybeEscapeViaReturn meet $MaybeEscapeViaParameter should be$MaybeEscapeViaParameterAndReturn")
        assert(MaybeEscapeViaReturn meet MaybeEscapeViaParameterAndReturn eq MaybeEscapeViaParameterAndReturn, s"$MaybeEscapeViaReturn meet $MaybeEscapeViaParameterAndReturn should be$MaybeEscapeViaParameterAndReturn")
        assert(MaybeEscapeViaReturn meet MaybeEscapeViaParameterAndAbnormalReturn eq MaybeEscapeViaParameterAndNormalAndAbnormalReturn, s"$MaybeEscapeViaReturn meet $MaybeEscapeViaParameterAndAbnormalReturn should be$MaybeEscapeViaParameterAndNormalAndAbnormalReturn")
        assert(MaybeEscapeViaReturn meet MaybeEscapeViaNormalAndAbnormalReturn eq MaybeEscapeViaNormalAndAbnormalReturn, s"$MaybeEscapeViaReturn meet $MaybeEscapeViaNormalAndAbnormalReturn should be$MaybeEscapeViaNormalAndAbnormalReturn")
        assert(MaybeEscapeViaReturn meet MaybeEscapeViaParameterAndNormalAndAbnormalReturn eq MaybeEscapeViaParameterAndNormalAndAbnormalReturn, s"$MaybeEscapeViaReturn meet $MaybeEscapeViaParameterAndNormalAndAbnormalReturn should be$MaybeEscapeViaParameterAndNormalAndAbnormalReturn")
        assert(MaybeEscapeViaReturn meet GlobalEscape eq GlobalEscape, s"$MaybeEscapeViaReturn meet $GlobalEscape should be$GlobalEscape")
        assert(MaybeEscapeViaReturn meet EscapeViaStaticField eq EscapeViaStaticField, s"$MaybeEscapeViaReturn meet $EscapeViaStaticField should be$EscapeViaStaticField")
        assert(MaybeEscapeViaReturn meet EscapeViaHeapObject eq EscapeViaHeapObject, s"$MaybeEscapeViaReturn meet $EscapeViaHeapObject should be$EscapeViaHeapObject")

        assert(MaybeEscapeViaParameter meet MaybeEscapeViaParameterAndReturn eq MaybeEscapeViaParameterAndReturn, s"$MaybeEscapeViaParameter meet $MaybeEscapeViaParameterAndReturn should be$MaybeEscapeViaParameterAndReturn")
        assert(MaybeEscapeViaParameter meet MaybeEscapeViaParameterAndAbnormalReturn eq MaybeEscapeViaParameterAndAbnormalReturn, s"$MaybeEscapeViaParameter meet $MaybeEscapeViaParameterAndAbnormalReturn should be$MaybeEscapeViaParameterAndAbnormalReturn")
        assert(MaybeEscapeViaParameter meet MaybeEscapeViaNormalAndAbnormalReturn eq MaybeEscapeViaParameterAndNormalAndAbnormalReturn, s"$MaybeEscapeViaParameter meet $MaybeEscapeViaNormalAndAbnormalReturn should be$MaybeEscapeViaParameterAndNormalAndAbnormalReturn")
        assert(MaybeEscapeViaParameter meet MaybeEscapeViaParameterAndNormalAndAbnormalReturn eq MaybeEscapeViaParameterAndNormalAndAbnormalReturn, s"$MaybeEscapeViaParameter meet $MaybeEscapeViaParameterAndNormalAndAbnormalReturn should be$MaybeEscapeViaParameterAndNormalAndAbnormalReturn")
        assert(MaybeEscapeViaParameter meet GlobalEscape eq GlobalEscape, s"$MaybeEscapeViaParameter meet $GlobalEscape should be$GlobalEscape")
        assert(MaybeEscapeViaParameter meet EscapeViaStaticField eq EscapeViaStaticField, s"$MaybeEscapeViaParameter meet $EscapeViaStaticField should be$EscapeViaStaticField")
        assert(MaybeEscapeViaParameter meet EscapeViaHeapObject eq EscapeViaHeapObject, s"$MaybeEscapeViaParameter meet $EscapeViaHeapObject should be$EscapeViaHeapObject")

        assert(MaybeEscapeViaParameterAndReturn meet MaybeEscapeViaParameterAndAbnormalReturn eq MaybeEscapeViaParameterAndNormalAndAbnormalReturn, s"$MaybeEscapeViaParameterAndReturn meet $MaybeEscapeViaParameterAndAbnormalReturn should be$MaybeEscapeViaParameterAndNormalAndAbnormalReturn")
        assert(MaybeEscapeViaParameterAndReturn meet MaybeEscapeViaNormalAndAbnormalReturn eq MaybeEscapeViaParameterAndNormalAndAbnormalReturn, s"$MaybeEscapeViaParameterAndReturn meet $MaybeEscapeViaNormalAndAbnormalReturn should be$MaybeEscapeViaParameterAndNormalAndAbnormalReturn")
        assert(MaybeEscapeViaParameterAndReturn meet MaybeEscapeViaParameterAndNormalAndAbnormalReturn eq MaybeEscapeViaParameterAndNormalAndAbnormalReturn, s"$MaybeEscapeViaParameterAndReturn meet $MaybeEscapeViaParameterAndNormalAndAbnormalReturn should be$MaybeEscapeViaParameterAndNormalAndAbnormalReturn")
        assert(MaybeEscapeViaParameterAndReturn meet GlobalEscape eq GlobalEscape, s"$MaybeEscapeViaParameterAndReturn meet $GlobalEscape should be$GlobalEscape")
        assert(MaybeEscapeViaParameterAndReturn meet EscapeViaStaticField eq EscapeViaStaticField, s"$MaybeEscapeViaParameterAndReturn meet $EscapeViaStaticField should be$EscapeViaStaticField")
        assert(MaybeEscapeViaParameterAndReturn meet EscapeViaHeapObject eq EscapeViaHeapObject, s"$MaybeEscapeViaParameterAndReturn meet $EscapeViaHeapObject should be$EscapeViaHeapObject")

        assert(MaybeEscapeViaParameterAndAbnormalReturn meet MaybeEscapeViaNormalAndAbnormalReturn eq MaybeEscapeViaParameterAndNormalAndAbnormalReturn, s"$MaybeEscapeViaParameterAndAbnormalReturn meet $MaybeEscapeViaNormalAndAbnormalReturn should be$MaybeEscapeViaParameterAndNormalAndAbnormalReturn")
        assert(MaybeEscapeViaParameterAndAbnormalReturn meet MaybeEscapeViaParameterAndNormalAndAbnormalReturn eq MaybeEscapeViaParameterAndNormalAndAbnormalReturn, s"$MaybeEscapeViaParameterAndAbnormalReturn meet $MaybeEscapeViaParameterAndNormalAndAbnormalReturn should be$MaybeEscapeViaParameterAndNormalAndAbnormalReturn")
        assert(MaybeEscapeViaParameterAndAbnormalReturn meet GlobalEscape eq GlobalEscape, s"$MaybeEscapeViaParameterAndAbnormalReturn meet $GlobalEscape should be$GlobalEscape")
        assert(MaybeEscapeViaParameterAndAbnormalReturn meet EscapeViaStaticField eq EscapeViaStaticField, s"$MaybeEscapeViaParameterAndAbnormalReturn meet $EscapeViaStaticField should be$EscapeViaStaticField")
        assert(MaybeEscapeViaParameterAndAbnormalReturn meet EscapeViaHeapObject eq EscapeViaHeapObject, s"$MaybeEscapeViaParameterAndAbnormalReturn meet $EscapeViaHeapObject should be$EscapeViaHeapObject")

        assert(MaybeEscapeViaNormalAndAbnormalReturn meet MaybeEscapeViaParameterAndNormalAndAbnormalReturn eq MaybeEscapeViaParameterAndNormalAndAbnormalReturn, s"$MaybeEscapeViaNormalAndAbnormalReturn meet $MaybeEscapeViaParameterAndNormalAndAbnormalReturn should be$MaybeEscapeViaParameterAndNormalAndAbnormalReturn")
        assert(MaybeEscapeViaNormalAndAbnormalReturn meet GlobalEscape eq GlobalEscape, s"$MaybeEscapeViaNormalAndAbnormalReturn meet $GlobalEscape should be$GlobalEscape")
        assert(MaybeEscapeViaNormalAndAbnormalReturn meet EscapeViaStaticField eq EscapeViaStaticField, s"$MaybeEscapeViaNormalAndAbnormalReturn meet $EscapeViaStaticField should be$EscapeViaStaticField")
        assert(MaybeEscapeViaNormalAndAbnormalReturn meet EscapeViaHeapObject eq EscapeViaHeapObject, s"$MaybeEscapeViaNormalAndAbnormalReturn meet $EscapeViaHeapObject should be$EscapeViaHeapObject")

        assert(MaybeEscapeViaParameterAndNormalAndAbnormalReturn meet GlobalEscape eq GlobalEscape, s"$MaybeEscapeViaParameterAndNormalAndAbnormalReturn meet $GlobalEscape should be$GlobalEscape")
        assert(MaybeEscapeViaParameterAndNormalAndAbnormalReturn meet EscapeViaStaticField eq EscapeViaStaticField, s"$MaybeEscapeViaParameterAndNormalAndAbnormalReturn meet $EscapeViaStaticField should be$EscapeViaStaticField")
        assert(MaybeEscapeViaParameterAndNormalAndAbnormalReturn meet EscapeViaHeapObject eq EscapeViaHeapObject, s"$MaybeEscapeViaParameterAndNormalAndAbnormalReturn meet $EscapeViaHeapObject should be$EscapeViaHeapObject")

        assert(GlobalEscape meet EscapeViaStaticField eq GlobalEscape, s"$GlobalEscape meet $EscapeViaStaticField should be$GlobalEscape")
        assert(GlobalEscape meet EscapeViaHeapObject eq GlobalEscape, s"$GlobalEscape meet $EscapeViaHeapObject should be$GlobalEscape")

        assert(EscapeViaStaticField meet EscapeViaHeapObject eq GlobalEscape, s"$EscapeViaStaticField meet $EscapeViaHeapObject should be$GlobalEscape")
    }

    behavior of "the less or equal restrictive relation"
    it should "be antisymmetric" in {
        for (prop1 ← allProperties) {
            for (prop2 ← allProperties) {
                if ((prop1 lessOrEqualRestrictive prop2) && (prop2 lessOrEqualRestrictive prop1)) {
                    if (prop1.isBottom)
                        assert(prop2.isBottom, s"$prop1 <= $prop2 and $prop2 <= $prop1 and $prop1 is bottom, so $prop2 should be bottom")
                    else
                        assert(prop1 eq prop2, s"$prop1 <= $prop2 and $prop2 <= $prop1, so they should be equal")
                }
            }
        }
    }
    it should "be transitive" in {
        for (prop1 ← allProperties) {
            for (prop2 ← allProperties) {
                for (prop3 ← allProperties) {
                    if ((prop1 lessOrEqualRestrictive prop2) && (prop2 lessOrEqualRestrictive prop3))
                        assert(prop1 lessOrEqualRestrictive prop3, s"$prop1 <= $prop2 and $prop2 <= $prop3, so $prop1 should be <= $prop3")
                }
            }
        }
    }
}

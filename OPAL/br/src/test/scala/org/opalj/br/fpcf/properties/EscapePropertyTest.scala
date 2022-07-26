/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package fpcf
package properties

import org.junit.runner.RunWith
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class EscapePropertyTest extends AnyFlatSpec with Matchers {
    val basicProperties = List(NoEscape, EscapeInCallee, EscapeViaParameter, EscapeViaReturn, EscapeViaAbnormalReturn,
        EscapeViaParameterAndReturn, EscapeViaParameterAndAbnormalReturn, EscapeViaNormalAndAbnormalReturn,
        EscapeViaParameterAndNormalAndAbnormalReturn)
    val atMostProperties = basicProperties map { AtMost(_) }
    val globalEscapes = List(GlobalEscape, EscapeViaStaticField, EscapeViaHeapObject)
    val allProperties = basicProperties ++ atMostProperties ++ globalEscapes

    behavior of "the meet operator"

    it should "be symmetric in its arguments, the result should be less or equal" in {
        for (prop1 <- allProperties) {
            for (prop2 <- allProperties) {
                val meet12 = prop1 meet prop2
                val meet21 = prop2 meet prop1
                assert(meet12 eq meet21, s"meet should be symmetric for $prop1 and $prop2")
                assert(meet12 lessOrEqualRestrictive prop1, s"$prop1 meet $prop2 ($meet12) should be lessOrEqualRestrictive than $prop1")
                assert(meet12 lessOrEqualRestrictive prop2, s"$prop1 meet $prop2 ($meet12) should be lessOrEqualRestrictive than $prop2")
            }
        }
    }

    it should "be the identity if both arguments are the same" in {
        for (prop <- allProperties) {
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
        assert(NoEscape meet AtMost(EscapeInCallee) eq AtMost(EscapeInCallee), s"$NoEscape meet $AtMost(EscapeInCallee) should be$AtMost(EscapeInCallee)")
        assert(NoEscape meet AtMost(EscapeViaReturn) eq AtMost(EscapeViaReturn), s"$NoEscape meet $AtMost(EscapeViaReturn) should be$AtMost(EscapeViaReturn)")
        assert(NoEscape meet AtMost(EscapeViaParameter) eq AtMost(EscapeViaParameter), s"$NoEscape meet $AtMost(EscapeViaParameter) should be$AtMost(EscapeViaParameter)")
        assert(NoEscape meet AtMost(EscapeViaParameterAndReturn) eq AtMost(EscapeViaParameterAndReturn), s"$NoEscape meet $AtMost(EscapeViaParameterAndReturn) should be$AtMost(EscapeViaParameterAndReturn)")
        assert(NoEscape meet AtMost(EscapeViaParameterAndAbnormalReturn) eq AtMost(EscapeViaParameterAndAbnormalReturn), s"$NoEscape meet $AtMost(EscapeViaParameterAndAbnormalReturn) should be$AtMost(EscapeViaParameterAndAbnormalReturn)")
        assert(NoEscape meet AtMost(EscapeViaNormalAndAbnormalReturn) eq AtMost(EscapeViaNormalAndAbnormalReturn), s"$NoEscape meet $AtMost(EscapeViaNormalAndAbnormalReturn) should be$AtMost(EscapeViaNormalAndAbnormalReturn)")
        assert(NoEscape meet AtMost(EscapeViaParameterAndNormalAndAbnormalReturn) eq AtMost(EscapeViaParameterAndNormalAndAbnormalReturn), s"$NoEscape meet $AtMost(EscapeViaParameterAndNormalAndAbnormalReturn) should be$AtMost(EscapeViaParameterAndNormalAndAbnormalReturn)")
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
        assert(EscapeInCallee meet AtMost(EscapeInCallee) eq AtMost(EscapeInCallee), s"$EscapeInCallee meet $AtMost(EscapeInCallee) should be$AtMost(EscapeInCallee)")
        assert(EscapeInCallee meet AtMost(EscapeViaReturn) eq AtMost(EscapeViaReturn), s"$EscapeInCallee meet $AtMost(EscapeViaReturn) should be$AtMost(EscapeViaReturn)")
        assert(EscapeInCallee meet AtMost(EscapeViaParameter) eq AtMost(EscapeViaParameter), s"$EscapeInCallee meet $AtMost(EscapeViaParameter) should be$AtMost(EscapeViaParameter)")
        assert(EscapeInCallee meet AtMost(EscapeViaParameterAndReturn) eq AtMost(EscapeViaParameterAndReturn), s"$EscapeInCallee meet $AtMost(EscapeViaParameterAndReturn) should be$AtMost(EscapeViaParameterAndReturn)")
        assert(EscapeInCallee meet AtMost(EscapeViaParameterAndAbnormalReturn) eq AtMost(EscapeViaParameterAndAbnormalReturn), s"$EscapeInCallee meet $AtMost(EscapeViaParameterAndAbnormalReturn) should be$AtMost(EscapeViaParameterAndAbnormalReturn)")
        assert(EscapeInCallee meet AtMost(EscapeViaNormalAndAbnormalReturn) eq AtMost(EscapeViaNormalAndAbnormalReturn), s"$EscapeInCallee meet $AtMost(EscapeViaNormalAndAbnormalReturn) should be$AtMost(EscapeViaNormalAndAbnormalReturn)")
        assert(EscapeInCallee meet AtMost(EscapeViaParameterAndNormalAndAbnormalReturn) eq AtMost(EscapeViaParameterAndNormalAndAbnormalReturn), s"$EscapeInCallee meet $AtMost(EscapeViaParameterAndNormalAndAbnormalReturn) should be$AtMost(EscapeViaParameterAndNormalAndAbnormalReturn)")
        assert(EscapeInCallee meet GlobalEscape eq GlobalEscape, s"$EscapeInCallee meet $GlobalEscape should be$GlobalEscape")
        assert(EscapeInCallee meet EscapeViaStaticField eq EscapeViaStaticField, s"$EscapeInCallee meet $EscapeViaStaticField should be$EscapeViaStaticField")
        assert(EscapeInCallee meet EscapeViaHeapObject eq EscapeViaHeapObject, s"$EscapeInCallee meet $EscapeViaHeapObject should be$EscapeViaHeapObject")

        assert(EscapeViaReturn meet EscapeViaParameter eq EscapeViaParameterAndReturn, s"$EscapeViaReturn meet $EscapeViaParameter should be$EscapeViaParameterAndReturn")
        assert(EscapeViaReturn meet EscapeViaAbnormalReturn eq EscapeViaNormalAndAbnormalReturn, s"$EscapeViaReturn meet $EscapeViaAbnormalReturn should be$EscapeViaNormalAndAbnormalReturn")
        assert(EscapeViaReturn meet EscapeViaParameterAndReturn eq EscapeViaParameterAndReturn, s"$EscapeViaReturn meet $EscapeViaParameterAndReturn should be$EscapeViaParameterAndReturn")
        assert(EscapeViaReturn meet EscapeViaParameterAndAbnormalReturn eq EscapeViaParameterAndNormalAndAbnormalReturn, s"$EscapeViaReturn meet $EscapeViaParameterAndAbnormalReturn should be$EscapeViaParameterAndNormalAndAbnormalReturn")
        assert(EscapeViaReturn meet EscapeViaNormalAndAbnormalReturn eq EscapeViaNormalAndAbnormalReturn, s"$EscapeViaReturn meet $EscapeViaNormalAndAbnormalReturn should be$EscapeViaNormalAndAbnormalReturn")
        assert(EscapeViaReturn meet EscapeViaParameterAndNormalAndAbnormalReturn eq EscapeViaParameterAndNormalAndAbnormalReturn, s"$EscapeViaReturn meet $EscapeViaParameterAndNormalAndAbnormalReturn should be$EscapeViaParameterAndNormalAndAbnormalReturn")
        assert(EscapeViaReturn meet AtMost(EscapeInCallee) eq AtMost(EscapeViaReturn), s"$EscapeViaReturn meet $AtMost(EscapeInCallee) should be$AtMost(EscapeViaReturn)")
        assert(EscapeViaReturn meet AtMost(EscapeViaReturn) eq AtMost(EscapeViaReturn), s"$EscapeViaReturn meet $AtMost(EscapeViaReturn) should be$AtMost(EscapeViaReturn)")
        assert(EscapeViaReturn meet AtMost(EscapeViaParameter) eq AtMost(EscapeViaParameterAndReturn), s"$EscapeViaReturn meet $AtMost(EscapeViaParameter) should be$AtMost(EscapeViaParameterAndReturn)")
        assert(EscapeViaReturn meet AtMost(EscapeViaParameterAndReturn) eq AtMost(EscapeViaParameterAndReturn), s"$EscapeViaReturn meet $AtMost(EscapeViaParameterAndReturn) should be$AtMost(EscapeViaParameterAndReturn)")
        assert(EscapeViaReturn meet AtMost(EscapeViaParameterAndAbnormalReturn) eq AtMost(EscapeViaParameterAndNormalAndAbnormalReturn), s"$EscapeViaReturn meet $AtMost(EscapeViaParameterAndAbnormalReturn) should be$AtMost(EscapeViaParameterAndNormalAndAbnormalReturn)")
        assert(EscapeViaReturn meet AtMost(EscapeViaNormalAndAbnormalReturn) eq AtMost(EscapeViaNormalAndAbnormalReturn), s"$EscapeViaReturn meet $AtMost(EscapeViaNormalAndAbnormalReturn) should be$AtMost(EscapeViaNormalAndAbnormalReturn)")
        assert(EscapeViaReturn meet AtMost(EscapeViaParameterAndNormalAndAbnormalReturn) eq AtMost(EscapeViaParameterAndNormalAndAbnormalReturn), s"$EscapeViaReturn meet $AtMost(EscapeViaParameterAndNormalAndAbnormalReturn) should be$AtMost(EscapeViaParameterAndNormalAndAbnormalReturn)")
        assert(EscapeViaReturn meet GlobalEscape eq GlobalEscape, s"$EscapeViaReturn meet $GlobalEscape should be$GlobalEscape")
        assert(EscapeViaReturn meet EscapeViaStaticField eq EscapeViaStaticField, s"$EscapeViaReturn meet $EscapeViaStaticField should be$EscapeViaStaticField")
        assert(EscapeViaReturn meet EscapeViaHeapObject eq EscapeViaHeapObject, s"$EscapeViaReturn meet $EscapeViaHeapObject should be$EscapeViaHeapObject")

        assert(EscapeViaParameter meet EscapeViaAbnormalReturn eq EscapeViaParameterAndAbnormalReturn, s"$EscapeViaParameter meet $EscapeViaAbnormalReturn should be$EscapeViaParameterAndAbnormalReturn")
        assert(EscapeViaParameter meet EscapeViaParameterAndReturn eq EscapeViaParameterAndReturn, s"$EscapeViaParameter meet $EscapeViaParameterAndReturn should be$EscapeViaParameterAndReturn")
        assert(EscapeViaParameter meet EscapeViaParameterAndAbnormalReturn eq EscapeViaParameterAndAbnormalReturn, s"$EscapeViaParameter meet $EscapeViaParameterAndAbnormalReturn should be$EscapeViaParameterAndAbnormalReturn")
        assert(EscapeViaParameter meet EscapeViaNormalAndAbnormalReturn eq EscapeViaParameterAndNormalAndAbnormalReturn, s"$EscapeViaParameter meet $EscapeViaNormalAndAbnormalReturn should be$EscapeViaParameterAndNormalAndAbnormalReturn")
        assert(EscapeViaParameter meet EscapeViaParameterAndNormalAndAbnormalReturn eq EscapeViaParameterAndNormalAndAbnormalReturn, s"$EscapeViaParameter meet $EscapeViaParameterAndNormalAndAbnormalReturn should be$EscapeViaParameterAndNormalAndAbnormalReturn")
        assert(EscapeViaParameter meet AtMost(EscapeInCallee) eq AtMost(EscapeViaParameter), s"$EscapeViaParameter meet $AtMost(EscapeInCallee) should be$AtMost(EscapeViaParameter)")
        assert(EscapeViaParameter meet AtMost(EscapeViaReturn) eq AtMost(EscapeViaParameterAndReturn), s"$EscapeViaParameter meet $AtMost(EscapeViaReturn) should be$AtMost(EscapeViaParameterAndReturn)")
        assert(EscapeViaParameter meet AtMost(EscapeViaParameter) eq AtMost(EscapeViaParameter), s"$EscapeViaParameter meet $AtMost(EscapeViaParameter) should be$AtMost(EscapeViaParameter)")
        assert(EscapeViaParameter meet AtMost(EscapeViaParameterAndReturn) eq AtMost(EscapeViaParameterAndReturn), s"$EscapeViaParameter meet $AtMost(EscapeViaParameterAndReturn) should be$AtMost(EscapeViaParameterAndReturn)")
        assert(EscapeViaParameter meet AtMost(EscapeViaParameterAndAbnormalReturn) eq AtMost(EscapeViaParameterAndAbnormalReturn), s"$EscapeViaParameter meet $AtMost(EscapeViaParameterAndAbnormalReturn) should be$AtMost(EscapeViaParameterAndAbnormalReturn)")
        assert(EscapeViaParameter meet AtMost(EscapeViaNormalAndAbnormalReturn) eq AtMost(EscapeViaParameterAndNormalAndAbnormalReturn), s"$EscapeViaParameter meet $AtMost(EscapeViaNormalAndAbnormalReturn) should be$AtMost(EscapeViaParameterAndNormalAndAbnormalReturn)")
        assert(EscapeViaParameter meet AtMost(EscapeViaParameterAndNormalAndAbnormalReturn) eq AtMost(EscapeViaParameterAndNormalAndAbnormalReturn), s"$EscapeViaParameter meet $AtMost(EscapeViaParameterAndNormalAndAbnormalReturn) should be$AtMost(EscapeViaParameterAndNormalAndAbnormalReturn)")
        assert(EscapeViaParameter meet GlobalEscape eq GlobalEscape, s"$EscapeViaParameter meet $GlobalEscape should be$GlobalEscape")
        assert(EscapeViaParameter meet EscapeViaStaticField eq EscapeViaStaticField, s"$EscapeViaParameter meet $EscapeViaStaticField should be$EscapeViaStaticField")
        assert(EscapeViaParameter meet EscapeViaHeapObject eq EscapeViaHeapObject, s"$EscapeViaParameter meet $EscapeViaHeapObject should be$EscapeViaHeapObject")

        assert(EscapeViaAbnormalReturn meet EscapeViaParameterAndReturn eq EscapeViaParameterAndNormalAndAbnormalReturn, s"$EscapeViaAbnormalReturn meet $EscapeViaParameterAndReturn should be$EscapeViaParameterAndNormalAndAbnormalReturn")
        assert(EscapeViaAbnormalReturn meet EscapeViaParameterAndAbnormalReturn eq EscapeViaParameterAndAbnormalReturn, s"$EscapeViaAbnormalReturn meet $EscapeViaParameterAndAbnormalReturn should be$EscapeViaParameterAndAbnormalReturn")
        assert(EscapeViaAbnormalReturn meet EscapeViaNormalAndAbnormalReturn eq EscapeViaNormalAndAbnormalReturn, s"$EscapeViaAbnormalReturn meet $EscapeViaNormalAndAbnormalReturn should be$EscapeViaNormalAndAbnormalReturn")
        assert(EscapeViaAbnormalReturn meet EscapeViaParameterAndNormalAndAbnormalReturn eq EscapeViaParameterAndNormalAndAbnormalReturn, s"$EscapeViaAbnormalReturn meet $EscapeViaParameterAndNormalAndAbnormalReturn should be$EscapeViaParameterAndNormalAndAbnormalReturn")
        assert(EscapeViaAbnormalReturn meet AtMost(EscapeInCallee) eq AtMost(EscapeViaAbnormalReturn), s"$EscapeViaAbnormalReturn meet $AtMost(EscapeInCallee) should be$AtMost(EscapeViaAbnormalReturn)")
        assert(EscapeViaAbnormalReturn meet AtMost(EscapeViaReturn) eq AtMost(EscapeViaNormalAndAbnormalReturn), s"$EscapeViaAbnormalReturn meet $AtMost(EscapeViaReturn) should be$AtMost(EscapeViaNormalAndAbnormalReturn)")
        assert(EscapeViaAbnormalReturn meet AtMost(EscapeViaParameter) eq AtMost(EscapeViaParameterAndAbnormalReturn), s"$EscapeViaAbnormalReturn meet $AtMost(EscapeViaParameter) should be$AtMost(EscapeViaParameterAndAbnormalReturn)")
        assert(EscapeViaAbnormalReturn meet AtMost(EscapeViaParameterAndReturn) eq AtMost(EscapeViaParameterAndNormalAndAbnormalReturn), s"$EscapeViaAbnormalReturn meet $AtMost(EscapeViaParameterAndReturn) should be$AtMost(EscapeViaParameterAndNormalAndAbnormalReturn)")
        assert(EscapeViaAbnormalReturn meet AtMost(EscapeViaParameterAndAbnormalReturn) eq AtMost(EscapeViaParameterAndAbnormalReturn), s"$EscapeViaAbnormalReturn meet $AtMost(EscapeViaParameterAndAbnormalReturn) should be$AtMost(EscapeViaParameterAndAbnormalReturn)")
        assert(EscapeViaAbnormalReturn meet AtMost(EscapeViaNormalAndAbnormalReturn) eq AtMost(EscapeViaNormalAndAbnormalReturn), s"$EscapeViaAbnormalReturn meet $AtMost(EscapeViaNormalAndAbnormalReturn) should be$AtMost(EscapeViaNormalAndAbnormalReturn)")
        assert(EscapeViaAbnormalReturn meet AtMost(EscapeViaParameterAndNormalAndAbnormalReturn) eq AtMost(EscapeViaParameterAndNormalAndAbnormalReturn), s"$EscapeViaAbnormalReturn meet $AtMost(EscapeViaParameterAndNormalAndAbnormalReturn) should be$AtMost(EscapeViaParameterAndNormalAndAbnormalReturn)")
        assert(EscapeViaAbnormalReturn meet GlobalEscape eq GlobalEscape, s"$EscapeViaAbnormalReturn meet $GlobalEscape should be$GlobalEscape")
        assert(EscapeViaAbnormalReturn meet EscapeViaStaticField eq EscapeViaStaticField, s"$EscapeViaAbnormalReturn meet $EscapeViaStaticField should be$EscapeViaStaticField")
        assert(EscapeViaAbnormalReturn meet EscapeViaHeapObject eq EscapeViaHeapObject, s"$EscapeViaAbnormalReturn meet $EscapeViaHeapObject should be$EscapeViaHeapObject")

        assert(EscapeViaParameterAndReturn meet EscapeViaParameterAndAbnormalReturn eq EscapeViaParameterAndNormalAndAbnormalReturn, s"$EscapeViaParameterAndReturn meet $EscapeViaParameterAndAbnormalReturn should be$EscapeViaParameterAndNormalAndAbnormalReturn")
        assert(EscapeViaParameterAndReturn meet EscapeViaNormalAndAbnormalReturn eq EscapeViaParameterAndNormalAndAbnormalReturn, s"$EscapeViaParameterAndReturn meet $EscapeViaNormalAndAbnormalReturn should be$EscapeViaParameterAndNormalAndAbnormalReturn")
        assert(EscapeViaParameterAndReturn meet EscapeViaParameterAndNormalAndAbnormalReturn eq EscapeViaParameterAndNormalAndAbnormalReturn, s"$EscapeViaParameterAndReturn meet $EscapeViaParameterAndNormalAndAbnormalReturn should be$EscapeViaParameterAndNormalAndAbnormalReturn")
        assert(EscapeViaParameterAndReturn meet AtMost(EscapeInCallee) eq AtMost(EscapeViaParameterAndReturn), s"$EscapeViaParameterAndReturn meet $AtMost(EscapeInCallee) should be$AtMost(EscapeViaParameterAndReturn)")
        assert(EscapeViaParameterAndReturn meet AtMost(EscapeViaReturn) eq AtMost(EscapeViaParameterAndReturn), s"$EscapeViaParameterAndReturn meet $AtMost(EscapeViaReturn) should be$AtMost(EscapeViaParameterAndReturn)")
        assert(EscapeViaParameterAndReturn meet AtMost(EscapeViaParameter) eq AtMost(EscapeViaParameterAndReturn), s"$EscapeViaParameterAndReturn meet $AtMost(EscapeViaParameter) should be$AtMost(EscapeViaParameterAndReturn)")
        assert(EscapeViaParameterAndReturn meet AtMost(EscapeViaParameterAndReturn) eq AtMost(EscapeViaParameterAndReturn), s"$EscapeViaParameterAndReturn meet $AtMost(EscapeViaParameterAndReturn) should be$AtMost(EscapeViaParameterAndReturn)")
        assert(EscapeViaParameterAndReturn meet AtMost(EscapeViaParameterAndAbnormalReturn) eq AtMost(EscapeViaParameterAndNormalAndAbnormalReturn), s"$EscapeViaParameterAndReturn meet $AtMost(EscapeViaParameterAndAbnormalReturn) should be$AtMost(EscapeViaParameterAndNormalAndAbnormalReturn)")
        assert(EscapeViaParameterAndReturn meet AtMost(EscapeViaNormalAndAbnormalReturn) eq AtMost(EscapeViaParameterAndNormalAndAbnormalReturn), s"$EscapeViaParameterAndReturn meet $AtMost(EscapeViaNormalAndAbnormalReturn) should be$AtMost(EscapeViaParameterAndNormalAndAbnormalReturn)")
        assert(EscapeViaParameterAndReturn meet AtMost(EscapeViaParameterAndNormalAndAbnormalReturn) eq AtMost(EscapeViaParameterAndNormalAndAbnormalReturn), s"$EscapeViaParameterAndReturn meet $AtMost(EscapeViaParameterAndNormalAndAbnormalReturn) should be$AtMost(EscapeViaParameterAndNormalAndAbnormalReturn)")
        assert(EscapeViaParameterAndReturn meet GlobalEscape eq GlobalEscape, s"$EscapeViaParameterAndReturn meet $GlobalEscape should be$GlobalEscape")
        assert(EscapeViaParameterAndReturn meet EscapeViaStaticField eq EscapeViaStaticField, s"$EscapeViaParameterAndReturn meet $EscapeViaStaticField should be$EscapeViaStaticField")
        assert(EscapeViaParameterAndReturn meet EscapeViaHeapObject eq EscapeViaHeapObject, s"$EscapeViaParameterAndReturn meet $EscapeViaHeapObject should be$EscapeViaHeapObject")

        assert(EscapeViaParameterAndAbnormalReturn meet EscapeViaNormalAndAbnormalReturn eq EscapeViaParameterAndNormalAndAbnormalReturn, s"$EscapeViaParameterAndAbnormalReturn meet $EscapeViaNormalAndAbnormalReturn should be$EscapeViaParameterAndNormalAndAbnormalReturn")
        assert(EscapeViaParameterAndAbnormalReturn meet EscapeViaParameterAndNormalAndAbnormalReturn eq EscapeViaParameterAndNormalAndAbnormalReturn, s"$EscapeViaParameterAndAbnormalReturn meet $EscapeViaParameterAndNormalAndAbnormalReturn should be$EscapeViaParameterAndNormalAndAbnormalReturn")
        assert(EscapeViaParameterAndAbnormalReturn meet AtMost(EscapeInCallee) eq AtMost(EscapeViaParameterAndAbnormalReturn), s"$EscapeViaParameterAndAbnormalReturn meet $AtMost(EscapeInCallee) should be$AtMost(EscapeViaParameterAndAbnormalReturn)")
        assert(EscapeViaParameterAndAbnormalReturn meet AtMost(EscapeViaReturn) eq AtMost(EscapeViaParameterAndNormalAndAbnormalReturn), s"$EscapeViaParameterAndAbnormalReturn meet $AtMost(EscapeViaReturn) should be$AtMost(EscapeViaParameterAndNormalAndAbnormalReturn)")
        assert(EscapeViaParameterAndAbnormalReturn meet AtMost(EscapeViaParameter) eq AtMost(EscapeViaParameterAndAbnormalReturn), s"$EscapeViaParameterAndAbnormalReturn meet $AtMost(EscapeViaParameter) should be$AtMost(EscapeViaParameterAndAbnormalReturn)")
        assert(EscapeViaParameterAndAbnormalReturn meet AtMost(EscapeViaParameterAndReturn) eq AtMost(EscapeViaParameterAndNormalAndAbnormalReturn), s"$EscapeViaParameterAndAbnormalReturn meet $AtMost(EscapeViaParameterAndReturn) should be$AtMost(EscapeViaParameterAndNormalAndAbnormalReturn)")
        assert(EscapeViaParameterAndAbnormalReturn meet AtMost(EscapeViaParameterAndAbnormalReturn) eq AtMost(EscapeViaParameterAndAbnormalReturn), s"$EscapeViaParameterAndAbnormalReturn meet $AtMost(EscapeViaParameterAndAbnormalReturn) should be$AtMost(EscapeViaParameterAndAbnormalReturn)")
        assert(EscapeViaParameterAndAbnormalReturn meet AtMost(EscapeViaNormalAndAbnormalReturn) eq AtMost(EscapeViaParameterAndNormalAndAbnormalReturn), s"$EscapeViaParameterAndAbnormalReturn meet $AtMost(EscapeViaNormalAndAbnormalReturn) should be$AtMost(EscapeViaParameterAndNormalAndAbnormalReturn)")
        assert(EscapeViaParameterAndAbnormalReturn meet AtMost(EscapeViaParameterAndNormalAndAbnormalReturn) eq AtMost(EscapeViaParameterAndNormalAndAbnormalReturn), s"$EscapeViaParameterAndAbnormalReturn meet $AtMost(EscapeViaParameterAndNormalAndAbnormalReturn) should be$AtMost(EscapeViaParameterAndNormalAndAbnormalReturn)")
        assert(EscapeViaParameterAndAbnormalReturn meet GlobalEscape eq GlobalEscape, s"$EscapeViaParameterAndAbnormalReturn meet $GlobalEscape should be$GlobalEscape")
        assert(EscapeViaParameterAndAbnormalReturn meet EscapeViaStaticField eq EscapeViaStaticField, s"$EscapeViaParameterAndAbnormalReturn meet $EscapeViaStaticField should be$EscapeViaStaticField")
        assert(EscapeViaParameterAndAbnormalReturn meet EscapeViaHeapObject eq EscapeViaHeapObject, s"$EscapeViaParameterAndAbnormalReturn meet $EscapeViaHeapObject should be$EscapeViaHeapObject")

        assert(EscapeViaNormalAndAbnormalReturn meet EscapeViaParameterAndNormalAndAbnormalReturn eq EscapeViaParameterAndNormalAndAbnormalReturn, s"$EscapeViaNormalAndAbnormalReturn meet $EscapeViaParameterAndNormalAndAbnormalReturn should be$EscapeViaParameterAndNormalAndAbnormalReturn")
        assert(EscapeViaNormalAndAbnormalReturn meet AtMost(EscapeInCallee) eq AtMost(EscapeViaNormalAndAbnormalReturn), s"$EscapeViaNormalAndAbnormalReturn meet $AtMost(EscapeInCallee) should be$AtMost(EscapeViaNormalAndAbnormalReturn)")
        assert(EscapeViaNormalAndAbnormalReturn meet AtMost(EscapeViaReturn) eq AtMost(EscapeViaNormalAndAbnormalReturn), s"$EscapeViaNormalAndAbnormalReturn meet $AtMost(EscapeViaReturn) should be$AtMost(EscapeViaNormalAndAbnormalReturn)")
        assert(EscapeViaNormalAndAbnormalReturn meet AtMost(EscapeViaParameter) eq AtMost(EscapeViaParameterAndNormalAndAbnormalReturn), s"$EscapeViaNormalAndAbnormalReturn meet $AtMost(EscapeViaParameter) should be$AtMost(EscapeViaParameterAndNormalAndAbnormalReturn)")
        assert(EscapeViaNormalAndAbnormalReturn meet AtMost(EscapeViaParameterAndReturn) eq AtMost(EscapeViaParameterAndNormalAndAbnormalReturn), s"$EscapeViaNormalAndAbnormalReturn meet $AtMost(EscapeViaParameterAndReturn) should be$AtMost(EscapeViaParameterAndNormalAndAbnormalReturn)")
        assert(EscapeViaNormalAndAbnormalReturn meet AtMost(EscapeViaParameterAndAbnormalReturn) eq AtMost(EscapeViaParameterAndNormalAndAbnormalReturn), s"$EscapeViaNormalAndAbnormalReturn meet $AtMost(EscapeViaParameterAndAbnormalReturn) should be$AtMost(EscapeViaParameterAndNormalAndAbnormalReturn)")
        assert(EscapeViaNormalAndAbnormalReturn meet AtMost(EscapeViaNormalAndAbnormalReturn) eq AtMost(EscapeViaNormalAndAbnormalReturn), s"$EscapeViaNormalAndAbnormalReturn meet $AtMost(EscapeViaNormalAndAbnormalReturn) should be$AtMost(EscapeViaNormalAndAbnormalReturn)")
        assert(EscapeViaNormalAndAbnormalReturn meet AtMost(EscapeViaParameterAndNormalAndAbnormalReturn) eq AtMost(EscapeViaParameterAndNormalAndAbnormalReturn), s"$EscapeViaNormalAndAbnormalReturn meet $AtMost(EscapeViaParameterAndNormalAndAbnormalReturn) should be$AtMost(EscapeViaParameterAndNormalAndAbnormalReturn)")
        assert(EscapeViaNormalAndAbnormalReturn meet GlobalEscape eq GlobalEscape, s"$EscapeViaNormalAndAbnormalReturn meet $GlobalEscape should be$GlobalEscape")
        assert(EscapeViaNormalAndAbnormalReturn meet EscapeViaStaticField eq EscapeViaStaticField, s"$EscapeViaNormalAndAbnormalReturn meet $EscapeViaStaticField should be$EscapeViaStaticField")
        assert(EscapeViaNormalAndAbnormalReturn meet EscapeViaHeapObject eq EscapeViaHeapObject, s"$EscapeViaNormalAndAbnormalReturn meet $EscapeViaHeapObject should be$EscapeViaHeapObject")

        assert(EscapeViaParameterAndNormalAndAbnormalReturn meet AtMost(EscapeInCallee) eq AtMost(EscapeViaParameterAndNormalAndAbnormalReturn), s"$EscapeViaParameterAndNormalAndAbnormalReturn meet $AtMost(EscapeInCallee) should be$AtMost(EscapeViaParameterAndNormalAndAbnormalReturn)")
        assert(EscapeViaParameterAndNormalAndAbnormalReturn meet AtMost(EscapeViaReturn) eq AtMost(EscapeViaParameterAndNormalAndAbnormalReturn), s"$EscapeViaParameterAndNormalAndAbnormalReturn meet $AtMost(EscapeViaReturn) should be$AtMost(EscapeViaParameterAndNormalAndAbnormalReturn)")
        assert(EscapeViaParameterAndNormalAndAbnormalReturn meet AtMost(EscapeViaParameter) eq AtMost(EscapeViaParameterAndNormalAndAbnormalReturn), s"$EscapeViaParameterAndNormalAndAbnormalReturn meet $AtMost(EscapeViaParameter) should be$AtMost(EscapeViaParameterAndNormalAndAbnormalReturn)")
        assert(EscapeViaParameterAndNormalAndAbnormalReturn meet AtMost(EscapeViaParameterAndReturn) eq AtMost(EscapeViaParameterAndNormalAndAbnormalReturn), s"$EscapeViaParameterAndNormalAndAbnormalReturn meet $AtMost(EscapeViaParameterAndReturn) should be$AtMost(EscapeViaParameterAndNormalAndAbnormalReturn)")
        assert(EscapeViaParameterAndNormalAndAbnormalReturn meet AtMost(EscapeViaParameterAndAbnormalReturn) eq AtMost(EscapeViaParameterAndNormalAndAbnormalReturn), s"$EscapeViaParameterAndNormalAndAbnormalReturn meet $AtMost(EscapeViaParameterAndAbnormalReturn) should be$AtMost(EscapeViaParameterAndNormalAndAbnormalReturn)")
        assert(EscapeViaParameterAndNormalAndAbnormalReturn meet AtMost(EscapeViaNormalAndAbnormalReturn) eq AtMost(EscapeViaParameterAndNormalAndAbnormalReturn), s"$EscapeViaParameterAndNormalAndAbnormalReturn meet $AtMost(EscapeViaNormalAndAbnormalReturn) should be$AtMost(EscapeViaParameterAndNormalAndAbnormalReturn)")
        assert(EscapeViaParameterAndNormalAndAbnormalReturn meet AtMost(EscapeViaParameterAndNormalAndAbnormalReturn) eq AtMost(EscapeViaParameterAndNormalAndAbnormalReturn), s"$EscapeViaParameterAndNormalAndAbnormalReturn meet $AtMost(EscapeViaParameterAndNormalAndAbnormalReturn) should be$AtMost(EscapeViaParameterAndNormalAndAbnormalReturn)")
        assert(EscapeViaParameterAndNormalAndAbnormalReturn meet GlobalEscape eq GlobalEscape, s"$EscapeViaParameterAndNormalAndAbnormalReturn meet $GlobalEscape should be$GlobalEscape")
        assert(EscapeViaParameterAndNormalAndAbnormalReturn meet EscapeViaStaticField eq EscapeViaStaticField, s"$EscapeViaParameterAndNormalAndAbnormalReturn meet $EscapeViaStaticField should be$EscapeViaStaticField")
        assert(EscapeViaParameterAndNormalAndAbnormalReturn meet EscapeViaHeapObject eq EscapeViaHeapObject, s"$EscapeViaParameterAndNormalAndAbnormalReturn meet $EscapeViaHeapObject should be$EscapeViaHeapObject")

        assert(AtMost(EscapeInCallee) meet AtMost(EscapeViaReturn) eq AtMost(EscapeViaReturn), s"$AtMost(EscapeInCallee) meet $AtMost(EscapeViaReturn) should be$AtMost(EscapeViaReturn)")
        assert(AtMost(EscapeInCallee) meet AtMost(EscapeViaParameter) eq AtMost(EscapeViaParameter), s"$AtMost(EscapeInCallee) meet $AtMost(EscapeViaParameter) should be$AtMost(EscapeViaParameter)")
        assert(AtMost(EscapeInCallee) meet AtMost(EscapeViaParameterAndReturn) eq AtMost(EscapeViaParameterAndReturn), s"$AtMost(EscapeInCallee) meet $AtMost(EscapeViaParameterAndReturn) should be$AtMost(EscapeViaParameterAndReturn)")
        assert(AtMost(EscapeInCallee) meet AtMost(EscapeViaParameterAndAbnormalReturn) eq AtMost(EscapeViaParameterAndAbnormalReturn), s"$AtMost(EscapeInCallee) meet $AtMost(EscapeViaParameterAndAbnormalReturn) should be$AtMost(EscapeViaParameterAndAbnormalReturn)")
        assert(AtMost(EscapeInCallee) meet AtMost(EscapeViaNormalAndAbnormalReturn) eq AtMost(EscapeViaNormalAndAbnormalReturn), s"$AtMost(EscapeInCallee) meet $AtMost(EscapeViaNormalAndAbnormalReturn) should be$AtMost(EscapeViaNormalAndAbnormalReturn)")
        assert(AtMost(EscapeInCallee) meet AtMost(EscapeViaParameterAndNormalAndAbnormalReturn) eq AtMost(EscapeViaParameterAndNormalAndAbnormalReturn), s"$AtMost(EscapeInCallee) meet $AtMost(EscapeViaParameterAndNormalAndAbnormalReturn) should be$AtMost(EscapeViaParameterAndNormalAndAbnormalReturn)")
        assert(AtMost(EscapeInCallee) meet GlobalEscape eq GlobalEscape, s"$AtMost(EscapeInCallee) meet $GlobalEscape should be$GlobalEscape")
        assert(AtMost(EscapeInCallee) meet EscapeViaStaticField eq EscapeViaStaticField, s"$AtMost(EscapeInCallee) meet $EscapeViaStaticField should be$EscapeViaStaticField")
        assert(AtMost(EscapeInCallee) meet EscapeViaHeapObject eq EscapeViaHeapObject, s"$AtMost(EscapeInCallee) meet $EscapeViaHeapObject should be$EscapeViaHeapObject")

        assert(AtMost(EscapeViaReturn) meet AtMost(EscapeViaParameter) eq AtMost(EscapeViaParameterAndReturn), s"$AtMost(EscapeViaReturn) meet $AtMost(EscapeViaParameter) should be$AtMost(EscapeViaParameterAndReturn)")
        assert(AtMost(EscapeViaReturn) meet AtMost(EscapeViaParameterAndReturn) eq AtMost(EscapeViaParameterAndReturn), s"$AtMost(EscapeViaReturn) meet $AtMost(EscapeViaParameterAndReturn) should be$AtMost(EscapeViaParameterAndReturn)")
        assert(AtMost(EscapeViaReturn) meet AtMost(EscapeViaParameterAndAbnormalReturn) eq AtMost(EscapeViaParameterAndNormalAndAbnormalReturn), s"$AtMost(EscapeViaReturn) meet $AtMost(EscapeViaParameterAndAbnormalReturn) should be$AtMost(EscapeViaParameterAndNormalAndAbnormalReturn)")
        assert(AtMost(EscapeViaReturn) meet AtMost(EscapeViaNormalAndAbnormalReturn) eq AtMost(EscapeViaNormalAndAbnormalReturn), s"$AtMost(EscapeViaReturn) meet $AtMost(EscapeViaNormalAndAbnormalReturn) should be$AtMost(EscapeViaNormalAndAbnormalReturn)")
        assert(AtMost(EscapeViaReturn) meet AtMost(EscapeViaParameterAndNormalAndAbnormalReturn) eq AtMost(EscapeViaParameterAndNormalAndAbnormalReturn), s"$AtMost(EscapeViaReturn) meet $AtMost(EscapeViaParameterAndNormalAndAbnormalReturn) should be$AtMost(EscapeViaParameterAndNormalAndAbnormalReturn)")
        assert(AtMost(EscapeViaReturn) meet GlobalEscape eq GlobalEscape, s"$AtMost(EscapeViaReturn) meet $GlobalEscape should be$GlobalEscape")
        assert(AtMost(EscapeViaReturn) meet EscapeViaStaticField eq EscapeViaStaticField, s"$AtMost(EscapeViaReturn) meet $EscapeViaStaticField should be$EscapeViaStaticField")
        assert(AtMost(EscapeViaReturn) meet EscapeViaHeapObject eq EscapeViaHeapObject, s"$AtMost(EscapeViaReturn) meet $EscapeViaHeapObject should be$EscapeViaHeapObject")

        assert(AtMost(EscapeViaParameter) meet AtMost(EscapeViaParameterAndReturn) eq AtMost(EscapeViaParameterAndReturn), s"$AtMost(EscapeViaParameter) meet $AtMost(EscapeViaParameterAndReturn) should be$AtMost(EscapeViaParameterAndReturn)")
        assert(AtMost(EscapeViaParameter) meet AtMost(EscapeViaParameterAndAbnormalReturn) eq AtMost(EscapeViaParameterAndAbnormalReturn), s"$AtMost(EscapeViaParameter) meet $AtMost(EscapeViaParameterAndAbnormalReturn) should be$AtMost(EscapeViaParameterAndAbnormalReturn)")
        assert(AtMost(EscapeViaParameter) meet AtMost(EscapeViaNormalAndAbnormalReturn) eq AtMost(EscapeViaParameterAndNormalAndAbnormalReturn), s"$AtMost(EscapeViaParameter) meet $AtMost(EscapeViaNormalAndAbnormalReturn) should be$AtMost(EscapeViaParameterAndNormalAndAbnormalReturn)")
        assert(AtMost(EscapeViaParameter) meet AtMost(EscapeViaParameterAndNormalAndAbnormalReturn) eq AtMost(EscapeViaParameterAndNormalAndAbnormalReturn), s"$AtMost(EscapeViaParameter) meet $AtMost(EscapeViaParameterAndNormalAndAbnormalReturn) should be$AtMost(EscapeViaParameterAndNormalAndAbnormalReturn)")
        assert(AtMost(EscapeViaParameter) meet GlobalEscape eq GlobalEscape, s"$AtMost(EscapeViaParameter) meet $GlobalEscape should be$GlobalEscape")
        assert(AtMost(EscapeViaParameter) meet EscapeViaStaticField eq EscapeViaStaticField, s"$AtMost(EscapeViaParameter) meet $EscapeViaStaticField should be$EscapeViaStaticField")
        assert(AtMost(EscapeViaParameter) meet EscapeViaHeapObject eq EscapeViaHeapObject, s"$AtMost(EscapeViaParameter) meet $EscapeViaHeapObject should be$EscapeViaHeapObject")

        assert(AtMost(EscapeViaParameterAndReturn) meet AtMost(EscapeViaParameterAndAbnormalReturn) eq AtMost(EscapeViaParameterAndNormalAndAbnormalReturn), s"$AtMost(EscapeViaParameterAndReturn) meet $AtMost(EscapeViaParameterAndAbnormalReturn) should be$AtMost(EscapeViaParameterAndNormalAndAbnormalReturn)")
        assert(AtMost(EscapeViaParameterAndReturn) meet AtMost(EscapeViaNormalAndAbnormalReturn) eq AtMost(EscapeViaParameterAndNormalAndAbnormalReturn), s"$AtMost(EscapeViaParameterAndReturn) meet $AtMost(EscapeViaNormalAndAbnormalReturn) should be$AtMost(EscapeViaParameterAndNormalAndAbnormalReturn)")
        assert(AtMost(EscapeViaParameterAndReturn) meet AtMost(EscapeViaParameterAndNormalAndAbnormalReturn) eq AtMost(EscapeViaParameterAndNormalAndAbnormalReturn), s"$AtMost(EscapeViaParameterAndReturn) meet $AtMost(EscapeViaParameterAndNormalAndAbnormalReturn) should be$AtMost(EscapeViaParameterAndNormalAndAbnormalReturn)")
        assert(AtMost(EscapeViaParameterAndReturn) meet GlobalEscape eq GlobalEscape, s"$AtMost(EscapeViaParameterAndReturn) meet $GlobalEscape should be$GlobalEscape")
        assert(AtMost(EscapeViaParameterAndReturn) meet EscapeViaStaticField eq EscapeViaStaticField, s"$AtMost(EscapeViaParameterAndReturn) meet $EscapeViaStaticField should be$EscapeViaStaticField")
        assert(AtMost(EscapeViaParameterAndReturn) meet EscapeViaHeapObject eq EscapeViaHeapObject, s"$AtMost(EscapeViaParameterAndReturn) meet $EscapeViaHeapObject should be$EscapeViaHeapObject")

        assert(AtMost(EscapeViaParameterAndAbnormalReturn) meet AtMost(EscapeViaNormalAndAbnormalReturn) eq AtMost(EscapeViaParameterAndNormalAndAbnormalReturn), s"$AtMost(EscapeViaParameterAndAbnormalReturn) meet $AtMost(EscapeViaNormalAndAbnormalReturn) should be$AtMost(EscapeViaParameterAndNormalAndAbnormalReturn)")
        assert(AtMost(EscapeViaParameterAndAbnormalReturn) meet AtMost(EscapeViaParameterAndNormalAndAbnormalReturn) eq AtMost(EscapeViaParameterAndNormalAndAbnormalReturn), s"$AtMost(EscapeViaParameterAndAbnormalReturn) meet $AtMost(EscapeViaParameterAndNormalAndAbnormalReturn) should be$AtMost(EscapeViaParameterAndNormalAndAbnormalReturn)")
        assert(AtMost(EscapeViaParameterAndAbnormalReturn) meet GlobalEscape eq GlobalEscape, s"$AtMost(EscapeViaParameterAndAbnormalReturn) meet $GlobalEscape should be$GlobalEscape")
        assert(AtMost(EscapeViaParameterAndAbnormalReturn) meet EscapeViaStaticField eq EscapeViaStaticField, s"$AtMost(EscapeViaParameterAndAbnormalReturn) meet $EscapeViaStaticField should be$EscapeViaStaticField")
        assert(AtMost(EscapeViaParameterAndAbnormalReturn) meet EscapeViaHeapObject eq EscapeViaHeapObject, s"$AtMost(EscapeViaParameterAndAbnormalReturn) meet $EscapeViaHeapObject should be$EscapeViaHeapObject")

        assert(AtMost(EscapeViaNormalAndAbnormalReturn) meet AtMost(EscapeViaParameterAndNormalAndAbnormalReturn) eq AtMost(EscapeViaParameterAndNormalAndAbnormalReturn), s"$AtMost(EscapeViaNormalAndAbnormalReturn) meet $AtMost(EscapeViaParameterAndNormalAndAbnormalReturn) should be$AtMost(EscapeViaParameterAndNormalAndAbnormalReturn)")
        assert(AtMost(EscapeViaNormalAndAbnormalReturn) meet GlobalEscape eq GlobalEscape, s"$AtMost(EscapeViaNormalAndAbnormalReturn) meet $GlobalEscape should be$GlobalEscape")
        assert(AtMost(EscapeViaNormalAndAbnormalReturn) meet EscapeViaStaticField eq EscapeViaStaticField, s"$AtMost(EscapeViaNormalAndAbnormalReturn) meet $EscapeViaStaticField should be$EscapeViaStaticField")
        assert(AtMost(EscapeViaNormalAndAbnormalReturn) meet EscapeViaHeapObject eq EscapeViaHeapObject, s"$AtMost(EscapeViaNormalAndAbnormalReturn) meet $EscapeViaHeapObject should be$EscapeViaHeapObject")

        assert(AtMost(EscapeViaParameterAndNormalAndAbnormalReturn) meet GlobalEscape eq GlobalEscape, s"$AtMost(EscapeViaParameterAndNormalAndAbnormalReturn) meet $GlobalEscape should be$GlobalEscape")
        assert(AtMost(EscapeViaParameterAndNormalAndAbnormalReturn) meet EscapeViaStaticField eq EscapeViaStaticField, s"$AtMost(EscapeViaParameterAndNormalAndAbnormalReturn) meet $EscapeViaStaticField should be$EscapeViaStaticField")
        assert(AtMost(EscapeViaParameterAndNormalAndAbnormalReturn) meet EscapeViaHeapObject eq EscapeViaHeapObject, s"$AtMost(EscapeViaParameterAndNormalAndAbnormalReturn) meet $EscapeViaHeapObject should be$EscapeViaHeapObject")

        assert(GlobalEscape meet EscapeViaStaticField eq GlobalEscape, s"$GlobalEscape meet $EscapeViaStaticField should be$GlobalEscape")
        assert(GlobalEscape meet EscapeViaHeapObject eq GlobalEscape, s"$GlobalEscape meet $EscapeViaHeapObject should be$GlobalEscape")

        assert(EscapeViaStaticField meet EscapeViaHeapObject eq GlobalEscape, s"$EscapeViaStaticField meet $EscapeViaHeapObject should be$GlobalEscape")
    }

    behavior of "the less or equal restrictive relation"
    it should "be antisymmetric" in {
        for (prop1 <- allProperties) {
            for (prop2 <- allProperties) {
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
        for (prop1 <- allProperties) {
            for (prop2 <- allProperties) {
                for (prop3 <- allProperties) {
                    if ((prop1 lessOrEqualRestrictive prop2) && (prop2 lessOrEqualRestrictive prop3))
                        assert(prop1 lessOrEqualRestrictive prop3, s"$prop1 <= $prop2 and $prop2 <= $prop3, so $prop1 should be <= $prop3")
                }
            }
        }
    }

    behavior of "the property value id"
    it should "be unique" in {
        for (prop1 <- allProperties) {
            for (prop2 <- allProperties) {
                assert((prop1.propertyValueID != prop2.propertyValueID) || (prop1 eq prop2), s"$prop1 and $prop2 should not have the same property id")
            }
        }
    }
}

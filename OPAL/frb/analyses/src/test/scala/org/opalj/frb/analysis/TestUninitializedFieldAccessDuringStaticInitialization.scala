/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2014
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
package frb
package analysis

import AnalysisTest._
import analyses._
import br._
import br.analyses._
import java.net.URL

/**
 * Unit-tests for analysis UninitializedFieldAccessDuringStaticInitialization.
 *
 * @author Florian Brandherm
 * @author Roberts Kolosovs
 * @author Daniel Klauer
 */
@org.junit.runner.RunWith(classOf[org.scalatest.junit.JUnitRunner])
class TestUninitializedFieldAccessDuringStaticInitialization extends AnalysisTest {
    import TestUninitializedFieldAccessDuringStaticInitialization._

    behavior of "UninitializedFieldAccessDuringStaticInitialization"

    def MethodReport(
        className: String,
        descriptor: MethodDescriptor,
        name: String,
        line: Int,
        message: String) = {
        val classType = ObjectType("UninitializedFieldAccessDuringStaticInitialization/"+className)
        LineAndColumnBasedReport(project.source(classType), Severity.Error, classType,
            descriptor, name, Some(line), None,
            message)
    }

    def ClinitReport(className: String, line: Int, message: String) = {
        MethodReport(className, MethodDescriptor.NoArgsAndReturnVoid, "<clinit>",
            line, message)
    }

    val NoArgsAndReturnInt = MethodDescriptor(IndexedSeq(), IntegerType)

    def GetterReport(
        className: String,
        name: String,
        line: Int,
        message: String) = {
        MethodReport(className, NoArgsAndReturnInt, name, line, message)
    }

    it should "detect that A.<clinit>() accesses uninitialized subclass static field B.B1" in {
        reports should contain(ClinitReport("A", 44,
            "Access to uninitialized static field 'UninitializedFieldAccessDuringStaticInitialization/B.B1'"))
    }

    it should "detect that A.<clinit>() accesses uninitialized subclass static field C.C2" in {
        reports should contain(ClinitReport("A", 45,
            "Access to uninitialized static field 'UninitializedFieldAccessDuringStaticInitialization/C.C2'"))
    }

    it should "detect that A.<clinit>() accesses uninitialized subclass static field B.B3" in {
        reports should contain(ClinitReport("A", 113,
            "Access to uninitialized static field 'UninitializedFieldAccessDuringStaticInitialization/B.B3'"))
    }

    it should "detect that A.<clinit>() accesses uninitialized subclass static field C.C4" in {
        reports should contain(ClinitReport("A", 114,
            "Access to uninitialized static field 'UninitializedFieldAccessDuringStaticInitialization/C.C4'"))
    }

    it should "detect that A.<clinit>() causes an uninitialized subclass static field access in A.A_getB21()" in {
        reports should contain(GetterReport("A", "A_getB21", 54,
            "Access to uninitialized static field 'UninitializedFieldAccessDuringStaticInitialization/B.B21' during static initialization"))
    }

    it should "detect that A.<clinit>() causes an uninitialized subclass static field access in A.A_getC22()" in {
        reports should contain(GetterReport("A", "A_getC22", 58,
            "Access to uninitialized static field 'UninitializedFieldAccessDuringStaticInitialization/C.C22' during static initialization"))
    }

    it should "detect that A.<clinit>() causes an uninitialized subclass static field access in B.B_getB24()" in {
        reports should contain(GetterReport("B", "B_getB24", 49,
            "Access to uninitialized static field 'B24' during static initialization in 'UninitializedFieldAccessDuringStaticInitialization/A'"))
    }

    it should "detect that A.<clinit>() causes an uninitialized subclass static field access in B.B_getC25()" in {
        reports should contain(GetterReport("B", "B_getC25", 53,
            "Access to uninitialized static field 'UninitializedFieldAccessDuringStaticInitialization/C.C25' during static initialization in 'UninitializedFieldAccessDuringStaticInitialization/A'"))
    }

    it should "detect that A.<clinit>() causes an uninitialized subclass static field access in C.C_getB27()" in {
        reports should contain(GetterReport("C", "C_getB27", 48,
            "Access to uninitialized static field 'UninitializedFieldAccessDuringStaticInitialization/B.B27' during static initialization in 'UninitializedFieldAccessDuringStaticInitialization/A'"))
    }

    it should "detect that A.<clinit>() causes an uninitialized subclass static field access in C.C_getC28()" in {
        reports should contain(GetterReport("C", "C_getC28", 54,
            "Access to uninitialized static field 'C28' during static initialization in 'UninitializedFieldAccessDuringStaticInitialization/A'"))
    }

    it should "detect that A.<clinit>() indirectly causes an uninitialized subclass static field access in A.A_getB31()" in {
        reports should contain(GetterReport("A", "A_getB31", 74,
            "Access to uninitialized static field 'UninitializedFieldAccessDuringStaticInitialization/B.B31' during static initialization"))
    }

    it should "detect that A.<clinit>() indirectly causes an uninitialized subclass static field access in B.B_getB33()" in {
        reports should contain(GetterReport("B", "B_getB33", 66,
            "Access to uninitialized static field 'B33' during static initialization in 'UninitializedFieldAccessDuringStaticInitialization/A'"))
    }

    it should "detect that A.<clinit>() indirectly causes an uninitialized subclass static field access in A.A_getB35()" in {
        reports should contain(GetterReport("A", "A_getB35", 98,
            "Access to uninitialized static field 'UninitializedFieldAccessDuringStaticInitialization/B.B35' during static initialization"))
    }

    it should "detect that A.<clinit>() indirectly causes an uninitialized subclass static field access in B.B_getB37()" in {
        reports should contain(GetterReport("B", "B_getB37", 90,
            "Access to uninitialized static field 'B37' during static initialization in 'UninitializedFieldAccessDuringStaticInitialization/A'"))
    }

    it should "detect that A.<clinit>() accesses uninitialized subclass static field B.samename1" in {
        reports should contain(ClinitReport("A", 142,
            "Access to uninitialized static field 'UninitializedFieldAccessDuringStaticInitialization/B.samename1'"))
    }

    it should "detect that A.<clinit>() accesses uninitialized subclass static field B.samename2" in {
        reports should contain(ClinitReport("A", 148,
            "Access to uninitialized static field 'UninitializedFieldAccessDuringStaticInitialization/B.samename2'"))
    }

    it should "detect that A.<clinit>() accesses uninitialized subclass static field B.B50 via C.B50" in {
        reports should contain(ClinitReport("A", 155,
            "Access to uninitialized static field 'UninitializedFieldAccessDuringStaticInitialization/B.B50'"))
    }

    it should "detect that A.<clinit>() accesses uninitialized subclass static field B.B60 twice, but only generate one report" in {
        reports should contain(ClinitReport("A", 159,
            "Access to uninitialized static field 'UninitializedFieldAccessDuringStaticInitialization/B.B60'"))
    }

    it should "detect that A.<clinit>() accesses uninitialized inner subclass static field InnerB.InnerB40" in {
        reports should contain(ClinitReport("A", 177,
            "Access to uninitialized static field 'UninitializedFieldAccessDuringStaticInitialization/A$InnerB.InnerB40'"))
    }

    it should "detect that CodePaths.<clinit>() accesses uninitialized subclass static field CodePathsSubclass.i10" in {
        reports should contain(ClinitReport("CodePaths", 68,
            "Access to uninitialized static field 'UninitializedFieldAccessDuringStaticInitialization/CodePathsSubclass.i10'"))
    }

    it should "detect that CodePaths.<clinit>() causes an uninitialized subclass static field access in test11" in {
        reports should contain(MethodReport("CodePaths", MethodDescriptor.NoArgsAndReturnVoid, "test11", 52,
            "Access to uninitialized static field 'UninitializedFieldAccessDuringStaticInitialization/CodePathsSubclass.i11' during static initialization"))
    }

    it should "detect that CodePaths.<clinit>() accesses uninitialized subclass static field CodePathsSubclass.i12 on one code path" in {
        reports should contain(ClinitReport("CodePaths", 77,
            "Access to uninitialized static field 'UninitializedFieldAccessDuringStaticInitialization/CodePathsSubclass.i12'"))
    }

    it should "detect that CodePaths.<clinit>() accesses uninitialized subclass static field CodePathsSubclass.i12 also on another code path" in {
        reports should contain(ClinitReport("CodePaths", 79,
            "Access to uninitialized static field 'UninitializedFieldAccessDuringStaticInitialization/CodePathsSubclass.i12'"))
    }

    it should "detect that CodePaths.<clinit>() accesses uninitialized subclass static field CodePathsSubclass.i20" in {
        reports should contain(ClinitReport("CodePaths", 87,
            "Access to uninitialized static field 'UninitializedFieldAccessDuringStaticInitialization/CodePathsSubclass.i20'"))
    }

    it should "detect that CodePaths.<clinit>() accesses uninitialized subclass static field CodePathsSubclass.i30" in {
        reports should contain(ClinitReport("CodePaths", 106,
            "Access to uninitialized static field 'UninitializedFieldAccessDuringStaticInitialization/CodePathsSubclass.i30'"))
    }

    it should "detect that CodePaths.<clinit>() accesses uninitialized subclass static field CodePathsSubclass.i31" in {
        reports should contain(ClinitReport("CodePaths", 113,
            "Access to uninitialized static field 'UninitializedFieldAccessDuringStaticInitialization/CodePathsSubclass.i31'"))
    }

    it should "detect that Recursion.<clinit>() causes an uninitialized subclass static field access in limitedRecursion()" in {
        reports should contain(
            MethodReport(
                "Recursion",
                MethodDescriptor(IndexedSeq(IntegerType), VoidType),
                "limitedRecursion",
                43,
                "Access to uninitialized static field "+
                    "'UninitializedFieldAccessDuringStaticInitialization/RecursionSubclass.i1' "+
                    "during static initialization"))
    }

    it should "detect that Recursion.<clinit>() causes an uninitialized subclass static field access in infiniteRecursion()" in {
        reports should contain(
            MethodReport(
                "Recursion",
                MethodDescriptor.NoArgsAndReturnVoid,
                "infiniteRecursion",
                48,
                "Access to uninitialized static field "+
                    "'UninitializedFieldAccessDuringStaticInitialization/RecursionSubclass.i2' "+
                    "during static initialization"))
    }

    it should "detect that StaticNativeMethod.<clinit>() accesses uninitialized subclass static field StaticNativeMethodSubclass.i" in {
        reports should contain(ClinitReport("StaticNativeMethod", 48,
            "Access to uninitialized static field 'UninitializedFieldAccessDuringStaticInitialization/StaticNativeMethodSubclass.i'"))
    }

    it should "find 31 issues in total" in {
        reports.size should be(31)
    }
}

object TestUninitializedFieldAccessDuringStaticInitialization {
    val project = makeProjectFromJar("UninitializedFieldAccessDuringStaticInitialization.jar")
    val reports =
        new UninitializedFieldAccessDuringStaticInitialization[URL].analyze(project).toSet
}

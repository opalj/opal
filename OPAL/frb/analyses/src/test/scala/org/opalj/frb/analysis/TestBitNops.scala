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
 * Tests the BitNops analysis.
 * @author Daniel Klauer
 */
@org.junit.runner.RunWith(classOf[org.scalatest.junit.JUnitRunner])
class TestBitNops extends AnalysisTest {

    behavior of "the analysis BitNops"

    val project = createProject("BitNops.jar")
    val analysis = new BitNops[URL]
    val reports = analysis.analyze(project).toSet

    it should "detect a useless 0 | x operation at BitNops/Or:44" in {
        reports should contain(
            LineAndColumnBasedReport(
                project.source(ObjectType("BitNops/Or")),
                Severity.Info,
                ObjectType("BitNops/Or"),
                MethodDescriptor(IndexedSeq(IntegerType), IntegerType),
                "testZeroLhs",
                Some(44),
                None,
                "0 | x: bit or operation with 0 left operand is useless"))
    }

    it should "detect a useless x | 0 operation at BitNops/Or:49" in {
        reports should contain(
            LineAndColumnBasedReport(
                project.source(ObjectType("BitNops/Or")),
                Severity.Info,
                ObjectType("BitNops/Or"),
                MethodDescriptor(IndexedSeq(IntegerType), IntegerType),
                "testZeroRhs",
                Some(49),
                None,
                "x | 0: bit or operation with 0 right operand is useless"))
    }

    it should "detect a useless 0 | 0 operation at BitNops/Or:55" in {
        reports should (
            contain(LineAndColumnBasedReport(
                project.source(ObjectType("BitNops/Or")),
                Severity.Info,
                ObjectType("BitNops/Or"),
                MethodDescriptor(IndexedSeq.empty, IntegerType),
                "testZeroBoth",
                Some(55),
                None,
                "0 | x: bit or operation with 0 left operand is useless"))
            or contain(LineAndColumnBasedReport(
                project.source(ObjectType("BitNops/Or")),
                Severity.Info,
                ObjectType("BitNops/Or"),
                MethodDescriptor(IndexedSeq.empty, IntegerType),
                "testZeroBoth",
                Some(55),
                None,
                "x | 0: bit or operation with 0 right operand is useless")))
    }

    it should "detect a -1 | x operation at BitNops/Or:60" in {
        reports should contain(
            LineAndColumnBasedReport(
                project.source(ObjectType("BitNops/Or")),
                Severity.Info,
                ObjectType("BitNops/Or"),
                MethodDescriptor(IndexedSeq(IntegerType), IntegerType),
                "testMinusOneLhs",
                Some(60),
                None,
                "-1 | x: bit or operation with -1 left operand always returns -1"))
    }

    it should "detect a x | -1 operation at BitNops/Or:65" in {
        reports should contain(
            LineAndColumnBasedReport(
                project.source(ObjectType("BitNops/Or")),
                Severity.Info,
                ObjectType("BitNops/Or"),
                MethodDescriptor(IndexedSeq(IntegerType), IntegerType),
                "testMinusOneRhs",
                Some(65),
                None,
                "x | -1: bit or operation with -1 right operand always returns -1"))
    }

    it should "detect a -1 | -1 operation at BitNops/Or:71" in {
        reports should (
            contain(LineAndColumnBasedReport(
                project.source(ObjectType("BitNops/Or")),
                Severity.Info,
                ObjectType("BitNops/Or"),
                MethodDescriptor(IndexedSeq.empty, IntegerType),
                "testMinusOneBoth",
                Some(71),
                None,
                "-1 | x: bit or operation with -1 left operand always returns -1"))
            or contain(LineAndColumnBasedReport(
                project.source(ObjectType("BitNops/Or")),
                Severity.Info,
                ObjectType("BitNops/Or"),
                MethodDescriptor(IndexedSeq.empty, IntegerType),
                "testMinusOneBoth",
                Some(71),
                None,
                "x | -1: bit or operation with -1 right operand always returns -1")))
    }

    it should "detect a useless 0 & x operation at BitNops/And:44" in {
        reports should contain(
            LineAndColumnBasedReport(
                project.source(ObjectType("BitNops/And")),
                Severity.Info,
                ObjectType("BitNops/And"),
                MethodDescriptor(IndexedSeq(IntegerType), IntegerType),
                "testZeroLhs",
                Some(44),
                None,
                "0 & x: bit and operation with 0 left operand always returns 0"))
    }

    it should "detect a useless x & 0 operation at BitNops/And:49" in {
        reports should contain(
            LineAndColumnBasedReport(
                project.source(ObjectType("BitNops/And")),
                Severity.Info,
                ObjectType("BitNops/And"),
                MethodDescriptor(IndexedSeq(IntegerType), IntegerType),
                "testZeroRhs",
                Some(49),
                None,
                "x & 0: bit and operation with 0 right operand always returns 0"))
    }

    it should "detect a useless 0 & 0 operation at BitNops/And:55" in {
        reports should (
            contain(LineAndColumnBasedReport(
                project.source(ObjectType("BitNops/And")),
                Severity.Info,
                ObjectType("BitNops/And"),
                MethodDescriptor(IndexedSeq.empty, IntegerType),
                "testZeroBoth",
                Some(55),
                None,
                "0 & x: bit and operation with 0 left operand always returns 0"))
            or contain(LineAndColumnBasedReport(
                project.source(ObjectType("BitNops/And")),
                Severity.Info,
                ObjectType("BitNops/And"),
                MethodDescriptor(IndexedSeq.empty, IntegerType),
                "testZeroBoth",
                Some(55),
                None,
                "x & 0: bit and operation with 0 right operand always returns 0")))
    }

    it should "detect a -1 & x operation at BitNops/And:60" in {
        reports should contain(
            LineAndColumnBasedReport(
                project.source(ObjectType("BitNops/And")),
                Severity.Info,
                ObjectType("BitNops/And"),
                MethodDescriptor(IndexedSeq(IntegerType), IntegerType),
                "testMinusOneLhs",
                Some(60),
                None,
                "-1 & x: bit and operation with -1 left operand is useless"))
    }

    it should "detect a x & -1 operation at BitNops/And:65" in {
        reports should contain(
            LineAndColumnBasedReport(
                project.source(ObjectType("BitNops/And")),
                Severity.Info,
                ObjectType("BitNops/And"),
                MethodDescriptor(IndexedSeq(IntegerType), IntegerType),
                "testMinusOneRhs",
                Some(65),
                None,
                "x & -1: bit and operation with -1 right operand is useless"))
    }

    it should "detect a -1 & -1 operation at BitNops/And:71" in {
        reports should (
            contain(LineAndColumnBasedReport(
                project.source(ObjectType("BitNops/And")),
                Severity.Info,
                ObjectType("BitNops/And"),
                MethodDescriptor(IndexedSeq.empty, IntegerType),
                "testMinusOneBoth",
                Some(71),
                None,
                "-1 & x: bit and operation with -1 left operand is useless"))
            or contain(LineAndColumnBasedReport(
                project.source(ObjectType("BitNops/And")),
                Severity.Info,
                ObjectType("BitNops/And"),
                MethodDescriptor(IndexedSeq.empty, IntegerType),
                "testMinusOneBoth",
                Some(71),
                None,
                "x & -1: bit and operation with -1 right operand is useless")))
    }

    it should "only find 12 issues in BitNops.jar" in {
        reports.size should be(12)
    }
}

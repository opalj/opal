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
package test
package analysis

import AnalysisTest._
import analyses._
import br._
import br.analyses._
import java.net.URL

/**
 * Test for UselessIncrementInReturn
 *
 * @author Roberts Kolosovs
 * @author Daniel Klauer
 */
@org.junit.runner.RunWith(classOf[org.scalatest.junit.JUnitRunner])
class TestUselessIncrementInReturn extends AnalysisTest {
    import UselessIncrementInReturn._

    behavior of "UselessIncrementInReturn"

    it should "detect a useless int++ (IINC) instruction in a return statement at "+
        "UselessIncrementInReturn/IntParameterIncrementInReturn:50" in {
            val declaringClass =
                ObjectType("UselessIncrementInReturn/IntParameterIncrementInReturn")
            reports should contain(
                LineAndColumnBasedReport(
                    project.source(declaringClass),
                    Severity.Info,
                    declaringClass,
                    MethodDescriptor(IndexedSeq(IntegerType), IntegerType),
                    "intToIreturn",
                    Some(50),
                    None,
                    "Increment during return statement is dead code"))
        }

    it should "detect a useless int++ (IINC) instruction in a return statement at "+
        "UselessIncrementInReturn/IntParameterIncrementInReturn:54" in {
            val declaringClass =
                ObjectType("UselessIncrementInReturn/IntParameterIncrementInReturn")
            reports should contain(
                LineAndColumnBasedReport(
                    project.source(declaringClass),
                    Severity.Info,
                    ObjectType("UselessIncrementInReturn/IntParameterIncrementInReturn"),
                    MethodDescriptor(IndexedSeq(IntegerType), LongType),
                    "intToLreturn",
                    Some(54),
                    None,
                    "Increment during return statement is dead code"))
        }

    it should "detect a useless int++ (IINC) instruction in a return statement at "+
        "UselessIncrementInReturn/IntParameterIncrementInReturn:58" in {
            val declaringClass =
                ObjectType("UselessIncrementInReturn/IntParameterIncrementInReturn")
            reports should contain(
                LineAndColumnBasedReport(
                    project.source(declaringClass),
                    Severity.Info,
                    declaringClass,
                    MethodDescriptor(IndexedSeq(IntegerType), FloatType),
                    "intToFreturn",
                    Some(58),
                    None,
                    "Increment during return statement is dead code"))
        }

    it should "detect a useless int++ (IINC) instruction in a return statement at "+
        "UselessIncrementInReturn/IntParameterIncrementInReturn:62" in {
            val declaringClass =
                ObjectType("UselessIncrementInReturn/IntParameterIncrementInReturn")
            reports should contain(
                LineAndColumnBasedReport(
                    project.source(declaringClass),
                    Severity.Info,
                    declaringClass,
                    MethodDescriptor(IndexedSeq(IntegerType), DoubleType),
                    "intToDreturn",
                    Some(62),
                    None,
                    "Increment during return statement is dead code"))
        }

    it should "only find 4 issues in UselessIncrementInReturn.jar" in {
        reports.size should be(4)
    }
}

object UselessIncrementInReturn {
    val project = makeProjectFromJar("UselessIncrementInReturn.jar")
    val reports = new UselessIncrementInReturn[URL].analyze(project)
}

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
package de.tud.cs.st
package bat
package findrealbugs
package test
package analysis

import AnalysisTest._
import analyses._
import resolved._
import resolved.analyses._
import java.net.URL

/**
 * Tests the SyncSetUnsyncGet analysis.
 * @author Roberts Kolosovs
 * @author Daniel Klauer
 * @author Peter Spieler
 */
@org.junit.runner.RunWith(classOf[org.scalatest.junit.JUnitRunner])
class TestSyncSetUnsyncGet extends AnalysisTest {
    import TestSyncSetUnsyncGet._

    behavior of "SyncSetUnsyncGet"

    it should "detect that getter is not synchronized like setter for field 'String a'" in {
        val declaringClass = ObjectType("SyncSetUnsyncGet/VariousSettersAndGetters")
        results should contain(
            MethodBasedReport(
                project.source(declaringClass),
                Severity.Warning,
                declaringClass,
                MethodDescriptor(IndexedSeq.empty, ObjectType.String),
                "getA",
                "Is not synchronized like setA"))
    }

    it should "detect that getter is not synchronized like setter for field 'int b'" in {
        val declaringClass = ObjectType("SyncSetUnsyncGet/VariousSettersAndGetters")
        results should contain(
            MethodBasedReport(
                project.source(declaringClass),
                Severity.Warning,
                declaringClass,
                MethodDescriptor(IndexedSeq.empty, IntegerType),
                "getB",
                "Is not synchronized like setB"))
    }

    it should "find (only) 2 issues in SyncSetUnsyncGet.jar" in {
        results.size should be(2)
    }
}

object TestSyncSetUnsyncGet {
    val project = makeProjectFromJar("SyncSetUnsyncGet.jar")
    val results = new SyncSetUnsyncGet[URL].analyze(project)
}

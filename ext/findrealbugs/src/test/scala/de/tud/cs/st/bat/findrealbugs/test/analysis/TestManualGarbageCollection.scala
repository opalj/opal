/* License (BSD Style License):
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
 *  - Neither the name of the Software Technology Group or Technische
 *    Universität Darmstadt nor the names of its contributors may be used to
 *    endorse or promote products derived from this software without specific
 *    prior written permission.
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
 * Unit-tests for analysis ManualGarbageCollection.
 *
 * @author Daniel Klauer
 * @author Florian Brandherm
 */
@org.junit.runner.RunWith(classOf[org.scalatest.junit.JUnitRunner])
class TestManualGarbageCollection extends AnalysisTest {
    import TestManualGarbageCollection._

    behavior of "ManualGarbageCollection"

    it should "report class RuntimeGc which invokes "+
        "java.lang.Runtime.getRuntime().gc()" in {
            val declaringClass = ObjectType("ManualGarbageCollection/RuntimeGc")
            results should contain(MethodBasedReport(project.source(declaringClass),
                Severity.Info,
                MethodDescriptor.NoArgsAndReturnVoid,
                "main",
                "Contains unnecessary call to gc()"))
        }

    it should "report class SystemGc which invokes java.lang.System.gc()" in {
        val declaringClass = ObjectType("ManualGarbageCollection/SystemGc")
        results should contain(MethodBasedReport(project.source(declaringClass),
            Severity.Info,
            MethodDescriptor.NoArgsAndReturnVoid,
            "main",
            "Contains unnecessary call to gc()"))
    }

    it should "find 2 issues in total" in {
        results.size should be(2)
    }
}

object TestManualGarbageCollection {
    val project = makeProjectFromJar("ManualGarbageCollection.jar")
    val results = new ManualGarbageCollection[URL].analyze(project).toSet
}

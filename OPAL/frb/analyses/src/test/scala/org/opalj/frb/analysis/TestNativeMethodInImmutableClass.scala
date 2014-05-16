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
 * Unit Test for NativeMethodInImmutableClass.
 *
 * @author Roberts Kolosovs
 * @author Peter Spieler
 */
@org.junit.runner.RunWith(classOf[org.scalatest.junit.JUnitRunner])
class TestNativeMethodInImmutableClass extends AnalysisTest {
    import TestNativeMethodInImmutableClass._

    behavior of "NativeMethodInImmutableClass"

    it should "detect that JCIPAnnotatedWithNativeMethod contains native method "+
        "changeFoo" in {
            val declaringClass = ObjectType(
                "NativeMethodInImmutableClass/JCIPAnnotatedWithNativeMethod")
            results should contain(
                MethodBasedReport(
                    project.source(declaringClass),
                    Severity.Info,
                    declaringClass,
                    MethodDescriptor.apply(IntegerType, VoidType),
                    "changeFoo",
                    "is a native method in an immutable class."))
        }

    it should "detect 1 issue in total" in {
        results.size should be(1)
    }
}

object TestNativeMethodInImmutableClass {
    val project = makeProjectFromJars(
        Seq("JCIPAnnotations.jar", "NativeMethodInImmutableClass.jar"))
    val results = new NativeMethodInImmutableClass[URL].analyze(project).toSet
}

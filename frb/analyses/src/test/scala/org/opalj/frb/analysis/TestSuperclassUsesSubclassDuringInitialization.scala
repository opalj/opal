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
 * Unit-tests for analysis SuperclassUsesSubclassDuringInitialization.
 *
 * @author Florian Brandherm
 * @author Roberts Kolosovs
 */
@org.junit.runner.RunWith(classOf[org.scalatest.junit.JUnitRunner])
class TestSuperclassUsesSubclassDuringInitialization extends AnalysisTest {
    import TestSuperclassUsesSubclassDuringInitialization._

    behavior of "SuperclassUsesSubclassDuringInitialization"

    it should "find that NestedClass uses field \"singleton\" of inner subclass "+
        "InnerClass" in {
            val declaringClass =
                ObjectType("SuperclassUsesSubclassDuringInitialization/NestedClass")
            results should contain(ClassBasedReport(
                project.source(declaringClass),
                Severity.Error,
                declaringClass,
                "Class uses uninitialized field singleton of subclass "+
                    "SuperclassUsesSubclassDuringInitialization/NestedClass$InnerClass "+
                    "during initialization."))
        }

    it should "find that Superclass uses field \"foo\" of subclass Subclass during "+
        "initialization" in {
            val declaringClass =
                ObjectType("SuperclassUsesSubclassDuringInitialization/Superclass")
            results should contain(ClassBasedReport(
                project.source(declaringClass),
                Severity.Error,
                declaringClass,
                "Class uses uninitialized field foo of subclass "+
                    "SuperclassUsesSubclassDuringInitialization/Subclass during "+
                    "initialization."))
        }

    it should "detect that Test1Superclass uses field \"foo\" of subclass Test1Subclass "+
        "during initialization" in {
            val declaringClass =
                ObjectType("SuperclassUsesSubclassDuringInitialization/Test1Superclass")
            results should contain(ClassBasedReport(
                project.source(declaringClass),
                Severity.Error,
                declaringClass,
                "Class uses uninitialized field foo of subclass "+
                    "SuperclassUsesSubclassDuringInitialization/Test1Subclass during "+
                    "initialization."))
        }

    it should "detect that Test2Superclass uses field \"foo\" of subclass Test2Subclass "+
        "during initialization" in {
            val declaringClass =
                ObjectType("SuperclassUsesSubclassDuringInitialization/Test2Superclass")
            results should contain(ClassBasedReport(
                project.source(declaringClass),
                Severity.Error,
                declaringClass,
                "Class uses uninitialized field foo of subclass "+
                    "SuperclassUsesSubclassDuringInitialization/Test2Subclass during "+
                    "initialization."))
        }

    it should "detect that Test2Superclass uses field \"foo\" of subsubclass "+
        "Test2Subsubclass during initialization" in {
            val declaringClass =
                ObjectType("SuperclassUsesSubclassDuringInitialization/Test2Superclass")
            results should contain(ClassBasedReport(
                project.source(declaringClass),
                Severity.Error,
                declaringClass,
                "Class uses uninitialized field foo of subclass "+
                    "SuperclassUsesSubclassDuringInitialization/Test2Subsubclass during "+
                    "initialization."))
        }

    it should "detect that Test3Superclass indirectly uses field \"foo\" of subsubclass "+
        "Test3Subsubclass during initialization" in {
            val declaringClass =
                ObjectType("SuperclassUsesSubclassDuringInitialization/Test3Superclass")
            results should contain(ClassBasedReport(
                project.source(declaringClass),
                Severity.Error,
                declaringClass,
                "Class uses uninitialized field foo of subclass "+
                    "SuperclassUsesSubclassDuringInitialization/Test3Subsubclass during "+
                    "initialization."))
        }

    it should "detect that Test4Superclass indirectly uses field \"foo\" of subsubclass "+
        "Test4Subsubclass during initialization" in {
            val declaringClass =
                ObjectType("SuperclassUsesSubclassDuringInitialization/Test4Superclass")
            results should contain(ClassBasedReport(
                project.source(declaringClass),
                Severity.Error,
                declaringClass,
                "Class uses uninitialized field foo of subclass "+
                    "SuperclassUsesSubclassDuringInitialization/Test4Subsubclass during "+
                    "initialization."))
        }

    it should "detect that TestIndirectSuperclass indirectly uses field \"foo\" of "+
        "subsubclass TestIndirectSubclass during initialization" in {
            val declaringClass = ObjectType(
                "SuperclassUsesSubclassDuringInitialization/TestIndirectSuperclass")
            results should contain(ClassBasedReport(
                project.source(declaringClass),
                Severity.Error,
                declaringClass,
                "Class uses uninitialized field foo of subclass "+
                    "SuperclassUsesSubclassDuringInitialization/TestIndirectSubclass "+
                    "during initialization."))
        }

    it should "find 8 issues in total" in {
        results.size should be(8)
    }
}

object TestSuperclassUsesSubclassDuringInitialization {
    val project = makeProjectFromJar("SuperclassUsesSubclassDuringInitialization.jar")
    val results =
        new SuperclassUsesSubclassDuringInitialization[URL].analyze(project).toSet
}

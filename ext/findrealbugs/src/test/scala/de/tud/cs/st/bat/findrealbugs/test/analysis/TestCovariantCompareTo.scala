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
 * Tests the CovariantCompareTo analysis.
 *
 * @author Daniel Klauer
 * @author Roberts Kolosovs
 */
@org.junit.runner.RunWith(classOf[org.scalatest.junit.JUnitRunner])
class TestCovariantCompareTo extends AnalysisTest {
    import TestCovariantCompareTo._

    behavior of "CovariantCompareTo"

    def shouldReport(className: String) {
        val classType = ObjectType("CovariantCompareTo/"+className)
        results should contain(ClassBasedReport(
            project.source(classType),
            Severity.Warning,
            classType,
            "Missing compareTo(Object) to override Comparable.compareTo(Object)"))
    }

    it should "detect a covariant compareTo() in a Comparable abstract class" in {
        shouldReport("AbstractComparableWithCovariantCompareTo")
    }

    it should "detect a covariant compareTo() in a Comparable generic abstract class" in {
        shouldReport("AbstractGenericComparableWithCovariantCompareTo")
    }

    it should "detect a covariant compareTo() in a self-Comparable abstract class" in {
        shouldReport("AbstractSelfComparableWithCovariantCompareTo")
    }

    it should
        "detect a covariant compareTo() in a class inheriting from a Comparable class" in {
            shouldReport("ExtendsProperComparableWithCovariantCompareTo")
        }

    it should "only find 4 issues in CovariantCompareTo.jar" in {
        results.size should be(4)
    }
}

object TestCovariantCompareTo {
    val project = makeProjectFromJar("CovariantCompareTo.jar")
    val results = new CovariantCompareTo[URL].analyze(project)
}

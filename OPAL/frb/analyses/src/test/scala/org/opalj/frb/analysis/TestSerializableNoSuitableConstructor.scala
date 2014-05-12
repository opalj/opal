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
 * Unit Test for SerializableNoSuitableConstructor
 *
 * @author Roberts Kolosovs
 * @author Daniel Klauer
 * @author Peter Spieler
 */
@org.junit.runner.RunWith(classOf[org.scalatest.junit.JUnitRunner])
class TestSerializableNoSuitableConstructor extends AnalysisTest {
    import TestSerializableNoSuitableConstructor._

    behavior of "SerializableNoSuitableConstructor"

    it should "detect that class SerializableNoSuitableConstructor is the superclass of"+
        " a Serializable subclass and does not have a zero-arguments constructor" in {
            val classToReport =
                ObjectType("SerializableNoSuitableConstructor/SerializableNoSuitableConstructor")
            results should contain(ClassBasedReport(
                project.source(classToReport),
                Severity.Error,
                classToReport,
                "Is a superclass of a Serializable class ("+
                    "SerializableNoSuitableConstructor/"+
                    "SerializableInheritsFromNoSuitableConstructor"+
                    ") but does not define a zero-arguments constructor."))
        }

    it should "find exactly 1 issue in SerializableNoSuitableConstructor.jar" in {
        results.size should be(1)
    }
}

object TestSerializableNoSuitableConstructor {
    val project = makeProjectFromJar("SerializableNoSuitableConstructor.jar")
    val results = new SerializableNoSuitableConstructor[URL].analyze(project).toSet
}

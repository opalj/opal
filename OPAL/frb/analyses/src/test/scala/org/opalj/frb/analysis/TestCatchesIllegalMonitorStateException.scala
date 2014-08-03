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
 * Unit Test for CatchesIllegalMonitorStateException.
 *
 * @author Daniel Klauer
 * @author Peter Spieler
 */
@org.junit.runner.RunWith(classOf[org.scalatest.junit.JUnitRunner])
class TestCatchesIllegalMonitorStateException extends AnalysisTest {

    behavior of "CatchesIllegalMonitorStateException"

    val project = createProject("CatchesIllegalMonitorStateException.jar")
    val results = new CatchesIllegalMonitorStateException[URL].analyze(project).toSet

    it should "detect that MissingSynchronized.test() has a catch block for "+
        "IllegalMonitorStateException" in {
            results should contain {
                val declaringClass = ObjectType("CatchesIllegalMonitorStateException/"+
                    "MissingSynchronized")
                MethodBasedReport(
                    project.source(declaringClass),
                    Severity.Info,
                    declaringClass,
                    MethodDescriptor.NoArgsAndReturnVoid,
                    "test",
                    "Handles IllegalMonitorStateException")
            }
        }

    it should "detect that the run() method of the inner anonymous Runnable class "+
        "MissingSynchronized$1 has a catch block for IllegalMonitorStateException" in {
            results should contain {
                val declaringClass = ObjectType("CatchesIllegalMonitorStateException/"+
                    "MissingSynchronized$1")
                MethodBasedReport(
                    project.source(declaringClass),
                    Severity.Info,
                    declaringClass,
                    MethodDescriptor.NoArgsAndReturnVoid,
                    "run",
                    "Handles IllegalMonitorStateException")
            }
        }

    it should "find exactly 2 issues in CatchesIllegalMonitorStateException.jar" in {
        results.size should be(2)
    }
}

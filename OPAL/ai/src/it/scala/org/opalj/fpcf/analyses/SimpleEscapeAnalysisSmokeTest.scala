/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2017
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
package fpcf
package analyses

import org.junit.runner.RunWith
import org.scalatest.FunSpec
import org.scalatest.Matchers
import org.scalatest.junit.JUnitRunner

import org.opalj.util.PerformanceEvaluation.time
import org.opalj.util.Nanoseconds
import org.opalj.br.TestSupport.allBIProjects
import org.opalj.br.TestSupport.createJREProject
import org.opalj.br.analyses.SomeProject
import org.opalj.fpcf.analyses.escape.EagerSimpleEscapeAnalysis
import org.opalj.fpcf.properties.EscapeProperty

/**
 * Tests that the [[EagerSimpleEscapeAnalysis]] does not throw exceptions.
 *
 * @author Florian Kübler
 */
@RunWith(classOf[JUnitRunner])
class SimpleEscapeAnalysisSmokeTest extends FunSpec with Matchers {

    def reportAnalysisTime(t: Nanoseconds): Unit = { info(s"analysis took ${t.toSeconds}") }

    def checkProject(p: SomeProject): Unit = {
        val ps = p.get(PropertyStoreKey)
        ps.setupPhase(Set(EscapeProperty), Set.empty)
        EagerSimpleEscapeAnalysis.start(p)
        ps.waitOnPhaseCompletion()
    }

    describe(s"executing the simple escape analysis should not fail") {

        allBIProjects() foreach { biProject ⇒
            val (name, projectFactory) = biProject
            it(s"for $name") {
                val p = projectFactory()
                time { checkProject(p) } { reportAnalysisTime }
            }
        }

        it(s"for the JDK") {
            val p = createJREProject()
            time { checkProject(p) } { reportAnalysisTime }
        }
    }
}

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
package analysis

import org.junit.runner.RunWith
import org.opalj.ai.common.SimpleAIKey
import org.opalj.ai.domain.l0.PrimitiveTACAIDomain
import org.opalj.br.TestSupport.allBIProjects
import org.opalj.br.TestSupport.createJREProject
import org.opalj.br.analyses.PropertyStoreKey
import org.opalj.br.analyses.SomeProject
import org.opalj.util.PerformanceEvaluation.time
import org.scalatest.FunSpec
import org.scalatest.Matchers
import org.scalatest.junit.JUnitRunner

/**
 * Tests that all methods of the JDK can be converted to the ai-based three address representation.
 *
 * @author Florian Kübler
 */
@RunWith(classOf[JUnitRunner])
class SimpleEscapeAnalysisIntegrationTest extends FunSpec with Matchers {

    def checkProject(project: SomeProject): Unit = {
        SimpleAIKey.domainFactory = (p, cf, m) ⇒ new PrimitiveTACAIDomain(p.classHierarchy, cf, m)
        PropertyStoreKey.makeAllocationSitesAvailable(project)
        PropertyStoreKey.makeFormalParametersAvailable(project)
        val analysesManager = project.get(FPCFAnalysesManagerKey)
        analysesManager.runWithRecommended(SimpleEscapeAnalysis)(waitOnCompletion = true)
    }

    allBIProjects() foreach { biProject ⇒
        val (name, projectFactory) = biProject
        it(s"it should be able to run the analysis with $name") {
            time {
                checkProject(projectFactory())
            } { t ⇒ info(s"analysis took ${t.toSeconds}") }
        }
    }

    it(s"it should be able to run the analysis with the JDK") {
        time {
            checkProject(createJREProject())
        } { t ⇒ info(s"analysis took ${t.toSeconds}") }
    }
}

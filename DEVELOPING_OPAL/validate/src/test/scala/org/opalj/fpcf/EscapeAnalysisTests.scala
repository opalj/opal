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
package org.opalj.fpcf

import java.net.URL

import org.opalj.ai.common.SimpleAIKey
import org.opalj.ai.domain.l1.DefaultDomainWithCFGAndDefUse
import org.opalj.br.Method
import org.opalj.br.analyses.Project
import org.opalj.fpcf.analyses.escape.SimpleEscapeAnalysis
import org.opalj.fpcf.analyses.escape.InterproceduralEscapeAnalysis

/**
 * Tests if the escape properties specified in the test project (the classes in the (sub-)package of
 * org.opalj.fpcf.fixture) and the computed ones match. The actual matching is delegated to
 * PropertyMatchers to facilitate matching arbitrary complex property specifications.
 *
 * @author Florian Kübler
 */
class EscapeAnalysisTests extends PropertiesTest {

    override def executeAnalyses(
        analysisRunners: Set[FPCFAnalysisRunner]
    ): (Project[URL], PropertyStore, Set[FPCFAnalysis]) = {
        val p = FixtureProject.recreate() // to ensure that this project is not "polluted"

        p.getOrCreateProjectInformationKeyInitializationData(
            SimpleAIKey,
            (m: Method) ⇒ {
                new DefaultDomainWithCFGAndDefUse(p, m) with org.opalj.ai.domain.l1.DefaultArrayValuesBinding
            }
        )
        org.opalj.br.analyses.PropertyStoreKey.makeAllocationSitesAvailable(p)
        org.opalj.br.analyses.PropertyStoreKey.makeFormalParametersAvailable(p)
        val ps = p.get(org.opalj.br.analyses.PropertyStoreKey)
        val as = analysisRunners.map(ar ⇒ ar.start(p, ps))
        ps.waitOnPropertyComputationCompletion(true, true)
        (p, ps, as)
    }

    describe("no analysis is scheduled") {
        val as = executeAnalyses(Set.empty)
        validateProperties(
            as,
            allocationSitesWithAnnotations ++ explicitFormalParametersWithAnnotations,
            Set("EscapeProperty")
        )
    }

    describe("the org.opalj.fpcf.analyses.escape.SimpleEscapeAnalysis is executed") {
        val as = executeAnalyses(Set(SimpleEscapeAnalysis))
        validateProperties(
            as,
            allocationSitesWithAnnotations ++ explicitFormalParametersWithAnnotations,
            Set("EscapeProperty")
        )
    }

    describe("the org.opalj.fpcf.analyses.escape.InterproceduralEscapeAnalysis is executed") {
        val as = executeAnalyses(Set(InterproceduralEscapeAnalysis))
        validateProperties(
            as,
            allocationSitesWithAnnotations ++ explicitFormalParametersWithAnnotations,
            Set("EscapeProperty")
        )
    }

}

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
import org.opalj.ai.domain.l2.DefaultPerformInvocationsDomainWithCFGAndDefUse
import org.opalj.br.Method
import org.opalj.br.analyses.Project
import org.opalj.fpcf.analyses.LazyVirtualCallAggregatingEscapeAnalysis
import org.opalj.fpcf.analyses.escape.EagerInterProceduralEscapeAnalysis
import org.opalj.fpcf.analyses.escape.EagerSimpleEscapeAnalysis

/**
 * Tests if the escape properties specified in the test project (the classes in the (sub-)package of
 * org.opalj.fpcf.fixture) and the computed ones match. The actual matching is delegated to
 * PropertyMatchers to facilitate matching arbitrary complex property specifications.
 *
 * @author Florian Kuebler
 */
class EscapeAnalysisTests extends PropertiesTest {

    override def executeAnalyses(
        eagerAnalysisRunners: Set[FPCFEagerAnalysisScheduler],
        lazyAnalysisRunners:  Set[FPCFLazyAnalysisScheduler]
    ): (Project[URL], PropertyStore, Set[FPCFAnalysis]) = {
        val p = FixtureProject.recreate()

        p.getOrCreateProjectInformationKeyInitializationData(
            SimpleAIKey,
            (m: Method) ⇒ {
                new DefaultPerformInvocationsDomainWithCFGAndDefUse(p, m)
            }
        )
        val ps = p.get(PropertyStoreKey)

        ps.setupPhase((eagerAnalysisRunners ++ lazyAnalysisRunners).flatMap(
            _.derives.map(_.asInstanceOf[PropertyMetaInformation].key)
        ))

        lazyAnalysisRunners.foreach(_.startLazily(p, ps))
        val as = eagerAnalysisRunners.map(ar ⇒ ar.start(p, ps))
        ps.waitOnPhaseCompletion()
        (p, ps, as)
    }

    describe("no analysis is scheduled") {
        val as = executeAnalyses(Set.empty, Set.empty)
        validateProperties(
            as,
            allocationSitesWithAnnotations(as._1) ++ explicitFormalParametersWithAnnotations(as._1),
            Set("EscapeProperty")
        )
    }

    describe("the org.opalj.fpcf.analyses.escape.SimpleEscapeAnalysis is executed") {
        val as = executeAnalyses(Set(EagerSimpleEscapeAnalysis), Set.empty)
        validateProperties(
            as,
            allocationSitesWithAnnotations(as._1) ++ explicitFormalParametersWithAnnotations(as._1),
            Set("EscapeProperty")
        )
    }

    describe("the org.opalj.fpcf.analyses.escape.InterProceduralEscapeAnalysis is executed") {
        val as = executeAnalyses(Set(EagerInterProceduralEscapeAnalysis), Set(LazyVirtualCallAggregatingEscapeAnalysis))
        validateProperties(
            as,
            allocationSitesWithAnnotations(as._1) ++ explicitFormalParametersWithAnnotations(as._1),
            Set("EscapeProperty")
        )
    }

}

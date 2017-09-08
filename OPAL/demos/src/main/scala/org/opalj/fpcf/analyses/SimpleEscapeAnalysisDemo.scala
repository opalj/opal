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

import org.opalj.ai.domain.l0.PrimitiveTACAIDomain
import org.opalj.br.analyses.Project
import org.opalj.fpcf.properties.MaybeArgEscape
import java.net.URL

import org.opalj.fpcf.properties.GlobalEscapeViaStaticFieldAssignment
import org.opalj.tac.DefaultTACAIKey
import org.opalj.fpcf.properties.ArgEscape
import org.opalj.br.analyses.PropertyStoreKey
import org.opalj.br.analyses.FormalParameter
import org.opalj.fpcf.properties.NoEscape
import org.opalj.br.AllocationSite
import org.opalj.fpcf.properties.MaybeNoEscape
import org.opalj.ai.common.SimpleAIKey
import org.opalj.br.analyses.DefaultOneStepAnalysis
import org.opalj.br.analyses.BasicReport
import org.opalj.util.PerformanceEvaluation.time
import org.opalj.fpcf.properties.MaybeMethodEscape

class SimpleEscapeAnalysisDemo extends DefaultOneStepAnalysis {

    override def title: String = "determines those methods that are pure"

    override def description: String = {
        "identifies methods which are pure; i.e. which just operate on the passed parameters"
    }

    override def doAnalyze(
        project:       Project[URL],
        parameters:    Seq[String],
        isInterrupted: () ⇒ Boolean
    ): BasicReport = {
        SimpleAIKey.domainFactory = (p, m) ⇒ new PrimitiveTACAIDomain(p, m)
        time {
            val tacai = project.get(DefaultTACAIKey)
            for {
                m ← project.allMethodsWithBody.par
            } {
                tacai(m)
            }
        } { t ⇒ println(s"tac took ${t.toSeconds}") }

        PropertyStoreKey.makeAllocationSitesAvailable(project)
        PropertyStoreKey.makeFormalParametersAvailable(project)
        val analysesManager = project.get(FPCFAnalysesManagerKey)
        time {
            analysesManager.run(SimpleEscapeAnalysis)
        } { t ⇒ println(s"escape analysis took ${t.toSeconds}") }

        val propertyStore = project.get(PropertyStoreKey)
        val staticEscapes =
            propertyStore.entities(GlobalEscapeViaStaticFieldAssignment)
        val maybeNoEscape =
            propertyStore.entities(MaybeNoEscape)
        val maybeArgEscape =
            propertyStore.entities(MaybeArgEscape)
        val maybeMethodEscape =
            propertyStore.entities(MaybeMethodEscape)
        val argEscapes = propertyStore.entities(ArgEscape)
        val noEscape = propertyStore.entities(NoEscape)

        println("ALLOCATION SITES:")
        println(s"# of global escaping objects: ${sizeAsAS(staticEscapes)}")
        println(s"# of maybe no escaping objects: ${sizeAsAS(maybeNoEscape)}")
        println(s"# of maybe arg escaping objects: ${sizeAsAS(maybeArgEscape)}")
        println(s"# of maybe method escaping objects: ${sizeAsAS(maybeMethodEscape)}")
        println(s"# of arg escaping objects: ${sizeAsAS(argEscapes)}")
        println(s"# of local objects: ${sizeAsAS(noEscape)}")

        println("FORMAL PARAMETERS")
        println(s"# of global escaping objects: ${sizeAsFP(staticEscapes)}")
        println(s"# of maybe no escaping objects: ${sizeAsFP(maybeNoEscape)}")
        println(s"# of maybe arg escaping objects: ${sizeAsFP(maybeArgEscape)}")
        println(s"# of maybe method escaping objects: ${sizeAsFP(maybeMethodEscape)}")
        println(s"# of arg escaping objects: ${sizeAsFP(argEscapes)}")
        println(s"# of local objects: ${sizeAsFP(noEscape)}")

        def sizeAsAS(entities: Traversable[Entity]) = entities.collect { case x: AllocationSite ⇒ x }.size
        def sizeAsFP(entities: Traversable[Entity]) = entities.collect { case x: FormalParameter ⇒ x }.size

        BasicReport("")
    }

}

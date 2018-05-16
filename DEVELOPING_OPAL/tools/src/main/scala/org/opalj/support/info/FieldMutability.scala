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
package support
package info

import java.net.URL

import org.opalj.br.analyses.BasicReport
import org.opalj.br.analyses.DefaultOneStepAnalysis
import org.opalj.br.analyses.Project
import org.opalj.fpcf.PropertyStoreKey
import org.opalj.fpcf.analyses.EagerL1FieldMutabilityAnalysis
import org.opalj.fpcf.analyses.LazyL2PurityAnalysis
import org.opalj.fpcf.analyses.LazyVirtualMethodPurityAnalysis
import org.opalj.fpcf.analyses.LazyUnsoundPrematurelyReadFieldsAnalysis
import org.opalj.fpcf.properties.VirtualMethodPurity
import org.opalj.fpcf.properties.DeclaredFinalField
import org.opalj.fpcf.properties.EffectivelyFinalField
import org.opalj.fpcf.properties.LazyInitializedField
import org.opalj.fpcf.properties.NonFinalFieldByAnalysis

/**
 * Determines the field mutability for all fields in the current project.
 *
 * @author Dominik Helm
 */
object FieldMutability extends DefaultOneStepAnalysis {
    override def doAnalyze(
        project:       Project[URL],
        parameters:    Seq[String],
        isInterrupted: () ⇒ Boolean
    ): BasicReport = {
        val ps = project.get(PropertyStoreKey)

        ps.setupPhase(
            Set(
                fpcf.properties.FieldMutability.key,
                fpcf.properties.Purity.key,
                VirtualMethodPurity.key
            )
        )
        LazyUnsoundPrematurelyReadFieldsAnalysis.startLazily(project, ps)
        LazyL2PurityAnalysis.startLazily(project, ps)
        LazyVirtualMethodPurityAnalysis.startLazily(project, ps)

        EagerL1FieldMutabilityAnalysis.start(project, ps)
        ps.waitOnPhaseCompletion()

        val declared = ps.finalEntities(DeclaredFinalField).toSeq
        val effectively = ps.finalEntities(EffectivelyFinalField).toSeq
        val lazily = ps.finalEntities(LazyInitializedField).toSeq
        val nonFinal = ps.finalEntities(NonFinalFieldByAnalysis).toSeq

        val message =
            s"""|# of declared final fields: ${declared.size}
                |# of effectively final fields: ${effectively.size}
                |# of lazily initialized fields: ${lazily.size}
                |# of non-final fields: ${nonFinal.size}
                |"""

        BasicReport(message.stripMargin('|'))
    }
}

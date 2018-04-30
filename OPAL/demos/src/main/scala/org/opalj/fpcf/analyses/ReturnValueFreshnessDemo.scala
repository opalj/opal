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

import java.net.URL

import org.opalj.br.DeclaredMethod
import org.opalj.br.analyses.BasicReport
import org.opalj.br.analyses.DefaultOneStepAnalysis
import org.opalj.br.analyses.Project
import org.opalj.fpcf.analyses.escape.LazyInterProceduralEscapeAnalysis
import org.opalj.fpcf.properties.EscapeProperty
import org.opalj.fpcf.properties.ExtensibleGetter
import org.opalj.fpcf.properties.FieldLocality
import org.opalj.fpcf.properties.FreshReturnValue
import org.opalj.fpcf.properties.Getter
import org.opalj.fpcf.properties.NoFreshReturnValue
import org.opalj.fpcf.properties.PrimitiveReturnValue
import org.opalj.fpcf.properties.ReturnValueFreshness
import org.opalj.fpcf.properties.VExtensibleGetter
import org.opalj.fpcf.properties.VFreshReturnValue
import org.opalj.fpcf.properties.VGetter
import org.opalj.fpcf.properties.VNoFreshReturnValue
import org.opalj.fpcf.properties.VPrimitiveReturnValue
import org.opalj.fpcf.properties.VirtualMethodEscapeProperty
import org.opalj.fpcf.properties.VirtualMethodReturnValueFreshness

/**
 * A small demo determining the return value freshness for all methods in the current project.
 *
 * @author Florian Kuebler
 */
object ReturnValueFreshnessDemo extends DefaultOneStepAnalysis {
    override def doAnalyze(
        project:       Project[URL],
        parameters:    Seq[String],
        isInterrupted: () ⇒ Boolean
    ): BasicReport = {
        val ps = project.get(PropertyStoreKey)
        ps.debug = true

        ps.setupPhase(Set(
            EscapeProperty.key,
            FieldLocality.key,
            VirtualMethodEscapeProperty.key,
            VirtualMethodReturnValueFreshness.key,
            ReturnValueFreshness.key
        ))

        LazyInterProceduralEscapeAnalysis.startLazily(project, ps)
        LazyFieldLocalityAnalysis.startLazily(project, ps)
        LazyVirtualCallAggregatingEscapeAnalysis.startLazily(project, ps)
        LazyVirtualReturnValueFreshnessAnalysis.startLazily(project, ps)

        LazyReturnValueFreshnessAnalysis.startLazily(project, ps)

        for (dm ← project.get(DeclaredMethodsKey).declaredMethods) {
            ps(dm, ReturnValueFreshness.key)
        }

        ps.waitOnPhaseCompletion()

        val fresh = ps.finalEntities(FreshReturnValue).toSeq
        val notFresh = ps.finalEntities(NoFreshReturnValue).toSeq
        val prim = ps.finalEntities(PrimitiveReturnValue).toSeq
        val getter = ps.finalEntities(Getter).toSeq
        val extGetter = ps.finalEntities(ExtensibleGetter).toSeq
        val vfresh = ps.finalEntities(VFreshReturnValue).toSeq
        val vnotFresh = ps.finalEntities(VNoFreshReturnValue).toSeq
        val vprim = ps.finalEntities(VPrimitiveReturnValue).toSeq
        val vgetter = ps.finalEntities(VGetter).toSeq
        val vextGetter = ps.finalEntities(VExtensibleGetter).toSeq

        for (ep ← notFresh) {
            println(s"${ep.asInstanceOf[DeclaredMethod].toJava} -> No Fresh")
        }

        for (ep ← getter) {
            println(s"${ep.asInstanceOf[DeclaredMethod].toJava} -> Getter")
        }

        for (ep ← extGetter) {
            println(s"${ep.asInstanceOf[DeclaredMethod].toJava} -> Ext. Getter")
        }

        val message =
            s"""|# of methods with fresh return value: ${fresh.size}
                |# of methods without fresh return value: ${notFresh.size}
                |# of methods with primitive return value: ${prim.size}
                |# of methods that are getters: ${getter.size}
                |# of methods that are extensible getters: ${extGetter.size}
                |# of vmethods with fresh return value: ${vfresh.size}
                |# of vmethods without fresh return value: ${vnotFresh.size}
                |# of vmethods with primitive return value: ${vprim.size}
                |# of vmethods that are getters: ${vgetter.size}
                |# of vmethods that are extensible getters: ${vextGetter.size}
                |"""

        BasicReport(message.stripMargin('|'))
    }
}

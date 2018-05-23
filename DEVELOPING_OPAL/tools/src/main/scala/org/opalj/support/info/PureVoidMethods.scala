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

import org.opalj.br.DefinedMethod
import org.opalj.br.analyses.DefaultOneStepAnalysis
import org.opalj.br.analyses.Project
import org.opalj.br.analyses.BasicReport
import org.opalj.fpcf.FinalEP
import org.opalj.fpcf.PropertyStoreKey
import org.opalj.fpcf.FPCFAnalysesManagerKey
import org.opalj.fpcf.analyses.LazyStaticDataUsageAnalysis
import org.opalj.fpcf.analyses.LazyL0CompileTimeConstancyAnalysis
import org.opalj.fpcf.analyses.LazyFieldLocalityAnalysis
import org.opalj.fpcf.analyses.LazyClassImmutabilityAnalysis
import org.opalj.fpcf.analyses.LazyTypeImmutabilityAnalysis
import org.opalj.fpcf.analyses.LazyVirtualMethodStaticDataUsageAnalysis
import org.opalj.fpcf.analyses.LazyVirtualCallAggregatingEscapeAnalysis
import org.opalj.fpcf.analyses.LazyReturnValueFreshnessAnalysis
import org.opalj.fpcf.analyses.LazyVirtualReturnValueFreshnessAnalysis
import org.opalj.fpcf.analyses.LazyL1FieldMutabilityAnalysis
import org.opalj.fpcf.analyses.LazyVirtualMethodPurityAnalysis
import org.opalj.fpcf.analyses.escape.LazyInterProceduralEscapeAnalysis
import org.opalj.fpcf.analyses.purity.EagerL2PurityAnalysis
import org.opalj.fpcf.properties.LBPure
import org.opalj.fpcf.properties.LBSideEffectFree
import org.opalj.fpcf.properties.CompileTimePure

/**
 * Identifies pure/side-effect free methods with a void return type.
 *
 * @author Dominik Helm
 */
object PureVoidMethods extends DefaultOneStepAnalysis {

    override def title: String = "Pure Void Methods Analysis"

    override def description: String = {
        "finds useless methods because they are side effect free and do not return a value (void)"
    }

    override def doAnalyze(
        project: Project[URL], parameters: Seq[String], isInterrupted: () ⇒ Boolean
    ): BasicReport = {

        val propertyStore = project.get(PropertyStoreKey)

        project.get(FPCFAnalysesManagerKey).runAll(
            LazyL0CompileTimeConstancyAnalysis,
            LazyStaticDataUsageAnalysis,
            LazyVirtualMethodStaticDataUsageAnalysis,
            LazyInterProceduralEscapeAnalysis,
            LazyVirtualCallAggregatingEscapeAnalysis,
            LazyReturnValueFreshnessAnalysis,
            LazyVirtualReturnValueFreshnessAnalysis,
            LazyFieldLocalityAnalysis,
            LazyL1FieldMutabilityAnalysis,
            LazyClassImmutabilityAnalysis,
            LazyTypeImmutabilityAnalysis,
            LazyVirtualMethodPurityAnalysis,
            EagerL2PurityAnalysis
        )

        val entities = propertyStore.entities(fpcf.properties.Purity.key)

        val voidReturn = entities.collect {
            case FinalEP(m: DefinedMethod, p @ (CompileTimePure | LBPure | LBSideEffectFree)) // Do not report empty methods, they are e.g. used for base implementations of listeners
            // Empty methods still have a return instruction and therefore a body size of 1
            if m.definedMethod.returnType.isVoidType && !m.definedMethod.isConstructor &&
                m.definedMethod.body.isDefined && m.definedMethod.body.get.instructions.size != 1 ⇒
                (m, p)
        }

        BasicReport(
            voidReturn.toIterable map { mp ⇒
                val (m, p) = mp
                s"${m.toJava} has a void return type but it is $p"
            }
        )
    }
}

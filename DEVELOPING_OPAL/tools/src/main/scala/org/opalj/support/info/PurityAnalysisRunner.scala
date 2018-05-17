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
import org.opalj.br.analyses.BasicReport
import org.opalj.br.analyses.DefaultOneStepAnalysis
import org.opalj.br.analyses.Project
import org.opalj.fpcf.EPS
import org.opalj.fpcf.Entity
import org.opalj.fpcf.FinalEP
import org.opalj.fpcf.Property
import org.opalj.fpcf.PropertyStoreKey
import org.opalj.fpcf.analyses.EagerL0PurityAnalysis
import org.opalj.fpcf.analyses.EagerL1PurityAnalysis
import org.opalj.fpcf.analyses.EagerL2PurityAnalysis
import org.opalj.fpcf.analyses.LazyClassImmutabilityAnalysis
import org.opalj.fpcf.analyses.LazyFieldLocalityAnalysis
import org.opalj.fpcf.analyses.LazyL1FieldMutabilityAnalysis
import org.opalj.fpcf.analyses.LazyReturnValueFreshnessAnalysis
import org.opalj.fpcf.analyses.LazyTypeImmutabilityAnalysis
import org.opalj.fpcf.analyses.LazyVirtualCallAggregatingEscapeAnalysis
import org.opalj.fpcf.analyses.LazyVirtualMethodPurityAnalysis
import org.opalj.fpcf.analyses.LazyVirtualReturnValueFreshnessAnalysis
import org.opalj.fpcf.analyses.LazyL0FieldMutabilityAnalysis
import org.opalj.fpcf.analyses.escape.LazyInterProceduralEscapeAnalysis
import org.opalj.fpcf.properties.ClassImmutability
import org.opalj.fpcf.properties.EscapeProperty
import org.opalj.fpcf.properties.FieldLocality
import org.opalj.fpcf.properties.FieldMutability
import org.opalj.fpcf.properties.LBContextuallyPure
import org.opalj.fpcf.properties.LBContextuallySideEffectFree
import org.opalj.fpcf.properties.LBDContextuallyPure
import org.opalj.fpcf.properties.LBDContextuallySideEffectFree
import org.opalj.fpcf.properties.LBDExternallyPure
import org.opalj.fpcf.properties.LBDExternallySideEffectFree
import org.opalj.fpcf.properties.LBDPure
import org.opalj.fpcf.properties.LBDSideEffectFree
import org.opalj.fpcf.properties.LBExternallyPure
import org.opalj.fpcf.properties.LBExternallySideEffectFree
import org.opalj.fpcf.properties.LBImpure
import org.opalj.fpcf.properties.LBPure
import org.opalj.fpcf.properties.LBSideEffectFree
import org.opalj.fpcf.properties.Purity
import org.opalj.fpcf.properties.ReturnValueFreshness
import org.opalj.fpcf.properties.TypeImmutability
import org.opalj.fpcf.properties.VirtualMethodEscapeProperty
import org.opalj.fpcf.properties.VirtualMethodPurity
import org.opalj.fpcf.properties.VirtualMethodReturnValueFreshness
import org.opalj.fpcf.properties.CompileTimePure

/**
 * Executes a purity analysis for a given code base.
 *
 * @author Michael Eichberg
 * @author Dominik Helm
 */
object PurityAnalysisRunner extends DefaultOneStepAnalysis {

    final val L1PurityAnalysisParameter = "-analysis=L1PurityAnalysis"
    final val L2PurityAnalysisParameter = "-analysis=L2PurityAnalysis"
    final val suppressPerMethodReports = "-suppressPerMethodReports"

    override def title: String = "Purity Analysis"

    override def description: String = {
        "assess the purity of the projects methods using the configured analysis"
    }

    override def checkAnalysisSpecificParameters(parameters: Seq[String]): Traversable[String] = {
        super.checkAnalysisSpecificParameters(
            parameters.filter(p ⇒
                p != L1PurityAnalysisParameter && p != L2PurityAnalysisParameter && p != suppressPerMethodReports)
        )
    }

    override def doAnalyze(
        project: Project[URL], parameters: Seq[String], isInterrupted: () ⇒ Boolean
    ): BasicReport = {
        val propertyStore = project.get(PropertyStoreKey)

        propertyStore.setupPhase(Set(
            ClassImmutability.key,
            TypeImmutability.key,
            FieldMutability.key,
            VirtualMethodPurity.key,
            ReturnValueFreshness.key,
            VirtualMethodReturnValueFreshness.key,
            EscapeProperty.key,
            VirtualMethodEscapeProperty.key,
            FieldLocality.key,
            Purity.key
        ))

        LazyClassImmutabilityAnalysis.schedule(propertyStore)
        LazyTypeImmutabilityAnalysis.schedule(propertyStore)

        if (parameters.contains(L1PurityAnalysisParameter)) {
            LazyL1FieldMutabilityAnalysis.startLazily(project, propertyStore)
            LazyVirtualMethodPurityAnalysis.startLazily(project, propertyStore)
            EagerL1PurityAnalysis.start(project, propertyStore)
        } else if (parameters.contains(L2PurityAnalysisParameter)) {
            LazyL1FieldMutabilityAnalysis.startLazily(project, propertyStore)
            LazyVirtualMethodPurityAnalysis.startLazily(project, propertyStore)
            LazyReturnValueFreshnessAnalysis.startLazily(project, propertyStore)
            LazyVirtualReturnValueFreshnessAnalysis.startLazily(project, propertyStore)
            LazyInterProceduralEscapeAnalysis.startLazily(project, propertyStore)
            LazyVirtualCallAggregatingEscapeAnalysis.startLazily(project, propertyStore)
            LazyFieldLocalityAnalysis.startLazily(project, propertyStore)
            EagerL2PurityAnalysis.start(project, propertyStore)
        } else {
            LazyL0FieldMutabilityAnalysis.startLazily(project, propertyStore)
            EagerL0PurityAnalysis.start(project, propertyStore)
        }

        propertyStore.waitOnPhaseCompletion()

        val entitiesWithPurityProperty: Traversable[EPS[Entity, Purity]] =
            propertyStore.entities(Purity.key).toSeq
        val methodsWithPurityProperty: Traversable[(DefinedMethod, Property)] =
            entitiesWithPurityProperty.collect { case FinalEP(m: DefinedMethod, p) ⇒ (m, p) }
        val methodsWithPurityPropertyAsStrings =
            methodsWithPurityProperty.map(m ⇒ m._1.toJava+" >> "+m._2)

        val methodInfo = if (parameters.contains(suppressPerMethodReports))
            ""
        else
            methodsWithPurityPropertyAsStrings.toList.sorted.mkString(
                "\nPurity of methods:\n\t",
                "\n\t",
                s"\n\tTotal: ${methodsWithPurityProperty.size}\n"
            )

        val result = methodInfo +
            propertyStore.toString(false)+
                "\ncompile-time pure:                     "+methodsWithPurityProperty.count(m ⇒ m._2 == CompileTimePure)+
                "\nAt least pure:                         "+methodsWithPurityProperty.count(m ⇒ m._2 == LBPure)+
                "\nAt least domain-specficic pure:        "+methodsWithPurityProperty.count(m ⇒ m._2 == LBDPure)+
                "\nAt least side-effect free:             "+methodsWithPurityProperty.count(m ⇒ m._2 == LBSideEffectFree)+
                "\nAt least d-s side effect free:         "+methodsWithPurityProperty.count(m ⇒ m._2 == LBDSideEffectFree)+
                "\nAt least externally pure:              "+methodsWithPurityProperty.count(m ⇒ m._2 == LBExternallyPure)+
                "\nAt least d-s externally pure:          "+methodsWithPurityProperty.count(m ⇒ m._2 == LBDExternallyPure)+
                "\nAt least externally side-effect free:  "+methodsWithPurityProperty.count(m ⇒ m._2 == LBExternallySideEffectFree)+
                "\nAt least d-s ext. side-effect free:    "+methodsWithPurityProperty.count(m ⇒ m._2 == LBDExternallySideEffectFree)+
                "\nAt least contextually pure:            "+methodsWithPurityProperty.count(m ⇒ m._2 == LBContextuallyPure)+
                "\nAt least d-s contextually pure:        "+methodsWithPurityProperty.count(m ⇒ m._2 == LBDContextuallyPure)+
                "\nAt least contextually side-effect free:"+methodsWithPurityProperty.count(m ⇒ m._2 == LBContextuallySideEffectFree)+
                "\nAt least d-s cont. side-effect free:   "+methodsWithPurityProperty.count(m ⇒ m._2 == LBDContextuallySideEffectFree)+
                "\nImpure:                                "+methodsWithPurityProperty.count(m ⇒ m._2 == LBImpure)

        println(result)
        BasicReport("")
    }
}

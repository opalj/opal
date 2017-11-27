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

import org.opalj.br.Method
import org.opalj.br.analyses.PropertyStoreKey
import org.opalj.br.analyses.DefaultOneStepAnalysis
import org.opalj.br.analyses.Project
import org.opalj.br.analyses.BasicReport
<<<<<<< HEAD:DEVELOPING_OPAL/tools/src/main/scala/org/opalj/fpcf/PurityAnalysisRunner.scala
=======
import org.opalj.br.Method
import org.opalj.fpcf.Entity
import org.opalj.fpcf.Property
import org.opalj.fpcf.EP
import org.opalj.fpcf.analyses.AdvancedFieldMutabilityAnalysis
>>>>>>> 998bb1ee7fc7e62ed17c7ca6e6d49fb87b9e596a:DEVELOPING_OPAL/tools/src/main/scala/org/opalj/support/info/PurityAnalysisRunner.scala
import org.opalj.fpcf.analyses.ClassImmutabilityAnalysis
import org.opalj.fpcf.analyses.TypeImmutabilityAnalysis
import org.opalj.fpcf.analyses.L1FieldMutabilityAnalysis
import org.opalj.fpcf.analyses.L1PurityAnalysis
import org.opalj.fpcf.analyses.L0PurityAnalysis
import org.opalj.fpcf.properties.Pure
import org.opalj.fpcf.properties.Purity
import org.opalj.fpcf.properties.SideEffectFree

/**
 * Executes a purity analysis for a given code base.
 *
 * @author Michael Eichberg
 * @author Dominik Helm
 */
object PurityAnalysisRunner extends DefaultOneStepAnalysis {

    final val L1PurityAnalysisParameter = "-analysis=L1PurityAnalysis"

    override def title: String = "Purity Analysis"

    override def description: String = {
        "assess the purity of the projects methods using the configured analysis"
    }

    override def checkAnalysisSpecificParameters(parameters: Seq[String]): Traversable[String] = {
        super.checkAnalysisSpecificParameters(parameters.filter(_ != L1PurityAnalysisParameter))
    }

    override def doAnalyze(
        project: Project[URL], parameters: Seq[String], isInterrupted: () ⇒ Boolean
    ): BasicReport = {

        val propertyStore = project.get(PropertyStoreKey)

        if (parameters.contains(L1PurityAnalysisParameter)) {
            ClassImmutabilityAnalysis.start(project, propertyStore)
            TypeImmutabilityAnalysis.start(project, propertyStore)
            L1FieldMutabilityAnalysis.start(project, propertyStore)
            L1PurityAnalysis.start(project, propertyStore)
        } else {
            L1FieldMutabilityAnalysis.start(project, propertyStore)
            L0PurityAnalysis.start(project, propertyStore)
        }

        propertyStore.waitOnPropertyComputationCompletion(true)

        val entitiesWithPurityProperty: Traversable[EP[Entity, Purity]] =
            propertyStore.entities(Purity.key)
        val methodsWithPurityProperty: Traversable[(Method, Property)] =
            entitiesWithPurityProperty.collect { case EP(m: Method, p) ⇒ (m, p) }
        val methodsWithPurityPropertyAsStrings =
            methodsWithPurityProperty.map(m ⇒ m._2+" >> "+m._1.toJava)

        val methodInfo =
            methodsWithPurityPropertyAsStrings.toList.sorted.mkString(
                "\nPurity of methods:\n\t",
                "\n\t",
                s"\n\tTotal: ${methodsWithPurityProperty.size}\n"
            )

        val result = methodInfo +
            propertyStore.toString(false)+
            "\nPure methods:             "+methodsWithPurityProperty.count(m ⇒ m._2 == Pure)+
            "\nSide-effect free methods: "+methodsWithPurityProperty.count(m ⇒ m._2 == SideEffectFree)

        BasicReport(result)
    }
}

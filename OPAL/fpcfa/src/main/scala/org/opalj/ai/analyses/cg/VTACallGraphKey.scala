/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2014
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
package ai
package analyses
package cg

import org.opalj.br.analyses.ProjectInformationKey
import br.analyses.ProjectInformationKey
import br.analyses.SomeProject
import org.opalj.fpcf.analysis.FPCFAnalysesManagerKey
import org.opalj.br.analyses.SourceElementsPropertyStoreKey
import org.opalj.br.Method
import org.opalj.fpcf.analysis.IsEntryPoint
import org.opalj.fpcf.analysis.JavaEEEntryPointsAnalysis
import org.opalj.br.analyses.InstantiableClassesKey
import org.opalj.br.analyses.InjectedClassesInformationKey

/**
 * The ''key'' object to get a call graph that was calculated using the VTA algorithm.
 *
 * You can assume that – in general – the call graph calculated using the VTA algorithm
 * is more precise than the call graph calculated using the CHA algorithm. Depending
 * on the project, the performance may be better, equal or worse.
 *
 * @example
 *      To get the call graph object use the `Project`'s `get` method and pass in
 *      `this` object.
 *      {{{
 *      val ComputedCallGraph = project.get(VTACallGraphKey)
 *      }}}
 *
 * @author Michael Eichberg
 * @author Michael Reif
 */
object VTACallGraphKey extends ProjectInformationKey[ComputedCallGraph] {

    override protected def requirements =
        Seq(
            InjectedClassesInformationKey,
            FPCFAnalysesManagerKey,
            SourceElementsPropertyStoreKey,
            InstantiableClassesKey,
            FieldValuesKey,
            MethodReturnValuesKey
        )

    /**
     * Computes the `CallGraph` for the given project.
     */
    override protected def compute(project: SomeProject): ComputedCallGraph = {

        val analysisMode = project.analysisMode

        //TODO Develop entry point analysis for desktop applications.
        val entryPoints = analysisMode match {
            case AnalysisModes.DesktopApplication ⇒
                // This entry point set can be used but it is unnecessary imprecise...
                CallGraphFactory.defaultEntryPointsForLibraries(project)
            case AnalysisModes.JEE6WebApplication ⇒ {
                val fpcfManager = project.get(FPCFAnalysesManagerKey)
                if (!fpcfManager.isDerived(JavaEEEntryPointsAnalysis.derivedProperties))
                    fpcfManager.runWithRecommended(JavaEEEntryPointsAnalysis)(true)
                val propertyStore = project.get(SourceElementsPropertyStoreKey)
                propertyStore.collect { case (m: Method, IsEntryPoint) if m.body.nonEmpty ⇒ m }.toSet
            }
            case _ ⇒
                throw new IllegalArgumentException(s"This call graph does not support the current analysis mode: $analysisMode")
        }

        CallGraphFactory.create(
            project,
            () ⇒ entryPoints,
            new DefaultVTACallGraphAlgorithmConfiguration(project)
        )
    }
}


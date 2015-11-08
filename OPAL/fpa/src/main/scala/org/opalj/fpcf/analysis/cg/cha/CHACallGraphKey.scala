/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2015
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
package cg
package cha

import org.opalj.br.Method
import org.opalj.br.analyses.{CallBySignatureResolutionKey, ProjectInformationKey, SomeProject, _}

/**
 * The ''key'' object to get a call graph that was calculated using the CHA algorithm.
 *
 * In general, a CHA call graph is only a very rough approximation of the ''ideal''
 * call graph and may contain a large number edges that will never (cannot) occur
 * at runtime.
 *
 * @example
 *      To get the call graph object use the `Project`'s `get` method and pass in
 *      `this` object.
 *
 * @author Michael Reif
 */
object CHACallGraphKey extends ProjectInformationKey[ComputedCallGraph] {

    /**
     * The CHACallGraph has no special prerequisites.W
     *
     * @return `Nil`.
     */
    override protected def requirements = Seq(CallBySignatureResolutionKey, InstantiableClassesIndexKey)

    /**
     * Computes the `CallGraph` for the given project.
     */
    override protected def compute(project: SomeProject): ComputedCallGraph = {
        val fpcfManager = project.get(FPCFAnalysisManagerKey)
        if (!fpcfManager.isDerived(EntryPointsAnalysis.derivedProperties))
            fpcfManager.runWithRecommendations(EntryPointsAnalysis)(true)
        val entryPoints = getEntryPointsFromPropertyStore(project)
        CallGraphFactory.create(
            project, () ⇒ entryPoints,
            new CHACallGraphAlgorithmConfiguration(project)
        )
    }

    /*
     * Get all methods from the property store that are entry points.
     */
    private[this] def getEntryPointsFromPropertyStore(project: SomeProject): Set[Method] = {
        val propertyStore = project.get(SourceElementsPropertyStoreKey)
        propertyStore.collect { case (m: Method, IsEntryPoint) if m.body.nonEmpty ⇒ m }.toSet
    }
}
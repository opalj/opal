/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package analyses
package cg
package cha

import org.opalj.ai.analyses.cg._
import org.opalj.fpcf.analyses.CallBySignatureResolutionKey
import org.opalj.br.analyses._
import org.opalj.br.analyses.cg.InstantiableClassesKey

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
 * @author Michael Reif
 */
object CHACallGraphKey extends ProjectInformationKey[ComputedCallGraph, Nothing] {

    /**
     * The CHACallGraph has no special prerequisites.W
     *
     * @return `Nil`.
     */
    override protected def requirements = Seq(CallBySignatureResolutionKey, InstantiableClassesKey, EntryPointKey)

    /**
     * Computes the `CallGraph` for the given project.
     */
    override protected def compute(project: SomeProject): ComputedCallGraph = {

        val analysisMode = project.analysisMode
        val isLibrary = analysisMode == AnalysisModes.CPA || analysisMode == AnalysisModes.OPA
        val entryPoints = project.get(EntryPointKey).getEntryPoints()

        CallGraphFactory.create(
            project, () ⇒ entryPoints,
            new CHACallGraphAlgorithmConfiguration(project, isLibrary)
        )
    }
}

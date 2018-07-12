/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai
package analyses
package cg

import br.analyses.ProjectInformationKey
import br.analyses.SomeProject
import org.opalj.br.analyses.PropertyStoreKey
import org.opalj.br.analyses.cg.InstantiableClassesKey

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
 * @author Michael Eichberg
 * @author Michael Reif
 */
object VTACallGraphKey extends ProjectInformationKey[ComputedCallGraph, Nothing] {

    override protected def requirements =
        Seq(
            EntryPointKey,
            PropertyStoreKey,
            InstantiableClassesKey,
            FieldValuesKey,
            MethodReturnValuesKey
        )

    /**
     * Computes the `CallGraph` for the given project.
     */
    override protected def compute(project: SomeProject): ComputedCallGraph = {
        import AnalysisModes.isLibraryLike

        if (isLibraryLike(project.analysisMode)) {
            throw new IllegalArgumentException(s"This call graph does not support the current analysis mode: ${project.analysisMode}")
        }

        val entryPoints = project.get(EntryPointKey).getEntryPoints()

        CallGraphFactory.create(
            project,
            () ⇒ entryPoints,
            new DefaultVTACallGraphAlgorithmConfiguration(project)
        )
    }
}

/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf

import org.opalj.br.analyses.SomeProject

/**
 *  The underlying analysis will only be registered with the property store and
 *  scheduled for a specific entity if queried.
 *
 * @author Michael Eichberg
 */
trait FPCFLazyAnalysisScheduler extends AbstractFPCFAnalysisScheduler {

    final override def isLazy: Boolean = true

    final override def schedule(ps: PropertyStore, i: InitializationData): Unit = {
        startLazily(ps.context(classOf[SomeProject]), ps, i)
    }

    final def startLazily(project: SomeProject, i: InitializationData): FPCFAnalysis = {
        startLazily(project, project.get(PropertyStoreKey), i)
    }

    /**
     * Starts the analysis lazily.
     */
    def startLazily(
        project:       SomeProject,
        propertyStore: PropertyStore,
        i:             InitializationData
    ): FPCFAnalysis

}

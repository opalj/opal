/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package de

import org.opalj.br.analyses.{JavaProjectInformationKey, SomeProject}

/**
 * Key that can be used to get a `DependencyStore` that contains all dependencies
 * except self dependencies.
 *
 * ==Usage==
 * Just pass this object to a `Project` to get the [[DependencyStore]].
 *
 * @author Michael Eichberg
 */
object DependencyStoreWithoutSelfDependenciesKey
    extends JavaProjectInformationKey[DependencyStore, Nothing] {

    override def requirements(project: SomeProject): Seq[JavaProjectInformationKey[_ <: AnyRef, Nothing]] = Nil

    override def compute(project: SomeProject): DependencyStore = {
        def createDependencyProcessor(dp: DependencyProcessor) = {
            val baseProcessor = new DependencyProcessorDecorator(dp) with FilterSelfDependencies
            new DependencyExtractor(baseProcessor)
        }

        DependencyStore(project.allClassFiles, createDependencyProcessor)(project.logContext)
    }
}

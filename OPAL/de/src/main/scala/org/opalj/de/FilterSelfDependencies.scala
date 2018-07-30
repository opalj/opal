/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package de

import br._

/**
 * A `DependencyProcessor` that filters self-dependencies.
 *
 * @see [[DependencyStoreWithoutSelfDependenciesKey]] for a usage.
 *
 * @author Michael Eichberg
 */
trait FilterSelfDependencies extends DependencyProcessor {

    /**
     * Processes a dependency of the given type between the source and target.
     *
     * @param source The `source` element that has a dependency on the `target` element.
     * @param target The `target` element that the `source` element depends on.
     * @param dependencyType The type of the dependency.
     */
    abstract override def processDependency(
        source: VirtualSourceElement,
        target: VirtualSourceElement,
        dType:  DependencyType
    ): Unit = {
        if (source != target) super.processDependency(source, target, dType)
    }

}

/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj

/**
 * Functionality to extract dependencies between class files.
 *
 * @author Michael Eichberg
 */
package object de {

    type DependencyType = DependencyTypes.Value

    type DependencyTypesSet = scala.collection.Set[DependencyType]

    type DependencyTypesBitSet = Long
}

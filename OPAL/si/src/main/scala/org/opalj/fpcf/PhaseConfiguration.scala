/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf

case class PhaseConfiguration[A](
    propertyKinds: PropertyKindsConfiguration,
    scheduled:     List[ComputationSpecification[A]],
    toDelete:      Set[Int] = Set.empty
)

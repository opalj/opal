/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package de

import br._

/**
 * Basic implementation of the `DependencyProcessor` trait that does nothing when a
 * dependency is reported.
 *
 * @author Michael Eichberg
 */
class DependencyProcessorAdapter extends DependencyProcessor {

    override def processDependency(
        source: VirtualSourceElement,
        target: VirtualSourceElement,
        dType:  DependencyType
    ): Unit = {}

    override def processDependency(
        source:   VirtualSourceElement,
        baseType: ArrayType,
        dType:    DependencyType
    ): Unit = {}

    override def processDependency(
        source:   VirtualSourceElement,
        baseType: BaseType,
        dType:    DependencyType
    ): Unit = {}

}

object DependencyProcessorAdapter extends DependencyProcessorAdapter


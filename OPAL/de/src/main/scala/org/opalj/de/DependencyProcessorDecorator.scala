/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package de

import br._

/**
 * Decorator for a given [[DependencyProcessor]].
 *
 * ==Usage Scenario==
 * If some special processing of some `VirtualSourceElement`s needs to be done, but
 * in other cases processing should just be delegated to another dependency processor.
 *
 * ==Thread Safety==
 * This class is thread-safe if the specified base dependency processor is also thread-safe.
 */
class DependencyProcessorDecorator(
        baseDependencyProcessor: DependencyProcessor
) extends DependencyProcessor {

    def processDependency(
        source: VirtualSourceElement,
        target: VirtualSourceElement,
        dType:  DependencyType
    ): Unit = {
        baseDependencyProcessor.processDependency(source, target, dType)
    }

    def processDependency(
        source:    VirtualSourceElement,
        arrayType: ArrayType,
        dType:     DependencyType
    ): Unit = {
        baseDependencyProcessor.processDependency(source, arrayType, dType)
    }

    def processDependency(
        source:   VirtualSourceElement,
        baseType: BaseType,
        dType:    DependencyType
    ): Unit = {
        baseDependencyProcessor.processDependency(source, baseType, dType)
    }

}

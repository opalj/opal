/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package de

import org.opalj.br._

/**
 * A dependency processor that just counts the number of dependencies.
 *
 * Typically, a `DependencyProcessor` is passed to a [[DependencyExtractor]].
 * The latter calls back the `processDependency` methods for each identified dependency.
 *
 * @author Michael Eichberg
 */
class DependencyCountingDependencyProcessor extends DependencyProcessor {

    import java.util.concurrent.atomic.AtomicInteger

    protected[this] val dependencyCount = new AtomicInteger(0)
    override def processDependency(
        source: VirtualSourceElement,
        target: VirtualSourceElement,
        dType:  DependencyType
    ): Unit = {
        dependencyCount.incrementAndGet()
    }
    def currentDependencyCount: Int = dependencyCount.get

    protected[this] val dependencyOnArraysCount = new AtomicInteger(0)
    override def processDependency(
        source:    VirtualSourceElement,
        arrayType: ArrayType,
        dType:     DependencyType
    ): Unit = {
        dependencyOnArraysCount.incrementAndGet()
    }
    def currentDependencyOnArraysCount: Int = dependencyOnArraysCount.get

    protected[this] val dependencyOnPrimitivesCount = new AtomicInteger(0)
    override def processDependency(
        source:   VirtualSourceElement,
        baseType: BaseType,
        dType:    DependencyType
    ): Unit = {
        dependencyOnPrimitivesCount.incrementAndGet()
    }
    def currentDependencyOnPrimitivesCount: Int = dependencyOnPrimitivesCount.get

    final val DummyClassType = ObjectType("<-DUMMY_CLASSTYPE->")

    final val DummyVirtualClass = VirtualClass(DummyClassType)

    override def asVirtualClass(objectType: ObjectType): VirtualClass = {
        DummyVirtualClass
    }

    final val DummyVirtualField =
        VirtualField(DummyClassType, "<-DUMMY_FIELD->", DummyClassType)

    override def asVirtualField(
        declaringClassType: ObjectType, // Recall...new Int[]{1,2,3,...}.length => arraylength
        name:               String,
        fieldType:          FieldType
    ): VirtualField = {
        DummyVirtualField
    }

    final val DummyVirtualMethod =
        VirtualMethod(
            DummyClassType,
            "<-DUMMY_METHOD->",
            MethodDescriptor.NoArgsAndReturnVoid
        )

    override def asVirtualMethod(
        declaringClassType: ReferenceType, // Recall...new Int[]{1,2,3,...}.clone()
        name:               String,
        descriptor:         MethodDescriptor
    ): VirtualMethod = {
        DummyVirtualMethod
    }

}

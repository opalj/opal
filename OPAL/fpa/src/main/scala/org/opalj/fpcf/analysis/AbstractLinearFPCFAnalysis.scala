package org.opalj
package fpcf
package analysis

import org.opalj.br.analyses._

/**
 *
 * @author Michael Reif
 */
abstract class AbstractLinearFPCFAnalysis[T <: Entity](
    val project:        SomeProject,
    val entitySelector: PartialFunction[Entity, T] = PropertyStore.entitySelector()
)
        extends FPCFAnalysis {

    def determineProperty(entity: T): Traversable[EP]

    implicit val propertyStore = project.get(SourceElementsPropertyStoreKey)

    propertyStore.execute(entitySelector)(
        (determineProperty _).asInstanceOf[T ⇒ Traversable[EP]]
    )
}
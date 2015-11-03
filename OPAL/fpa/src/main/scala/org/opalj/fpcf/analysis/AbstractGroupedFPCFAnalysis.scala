package org.opalj.fpcf.analysis

import org.opalj.br.analyses._
import org.opalj.fpcf._

/**
 * @author Michael Reif
 */
abstract class AbstractGroupedFPCFAnalysis[K, E <: Entity](
    val project:        SomeProject,
    val groupBy:        E ⇒ K,
    val entitySelector: PartialFunction[Entity, E] = PropertyStore.entitySelector()
)
        extends FPCFAnalysis {

    def determineProperty(key: K, entities: Seq[E]): Traversable[EP]

    implicit val propertyStore = project.get(SourceElementsPropertyStoreKey)

    propertyStore.execute(entitySelector, groupBy)((key, entities) ⇒
        determineProperty(key, entities))
}
package org.opalj
package fpcf
package analysis

import org.opalj.fpcf.Entity
import org.opalj.br.analyses.SomeProject
import org.opalj.fpcf.PropertyComputationResult
import org.opalj.br.analyses.SourceElementsPropertyStoreKey
import org.opalj.AnalysisModes._
import scala.reflect.ClassTag
import net.ceedubs.ficus.Ficus._

/**
 *
 *
 * @author Michael Reif
 */
abstract class FPCFAnalysis[E <: Entity](val project: SomeProject) {

    def determineProperty(entity: E): PropertyComputationResult

}

abstract class DefaultFPCFAnalysis[T <: Entity](
    project:            SomeProject,
    val entitySelector: PartialFunction[Entity, T] = DefaultFPCFAnalysis.entitySelector()
)
        extends FPCFAnalysis[T](project) {

    implicit val propertyStore = project.get(SourceElementsPropertyStoreKey)

    propertyStore <||< (entitySelector, (determineProperty _).
        asInstanceOf[T ⇒ PropertyComputationResult])
}

abstract class FPCFAnalysisModeAnalysis[T <: Entity](
    project:        SomeProject,
    entitySelector: PartialFunction[Entity, T] = DefaultFPCFAnalysis.entitySelector()
)
        extends DefaultFPCFAnalysis[T](project, entitySelector) {

    lazy val analysisMode = AnalysisModes.withName(project.config.as[String]("org.opalj.analysisMode"))

    def isOpenLibrary = analysisMode eq OPA

    def isClosedLibrary = analysisMode eq CPA

    def isApplication = analysisMode eq APP
}

private[analysis] object DefaultFPCFAnalysis {
    def entitySelector[T <: Entity: ClassTag](): PartialFunction[Entity, T] = new PartialFunction[Entity, T] {
        def apply(v1: Entity): T = {
            if (isDefinedAt(v1))
                v1.asInstanceOf[T]
            else
                throw new IllegalArgumentException
        }

        def isDefinedAt(x: Entity): Boolean = {
            val ct = implicitly[ClassTag[T]]
            x.getClass.isInstance(ct.runtimeClass)
        }
    }
}

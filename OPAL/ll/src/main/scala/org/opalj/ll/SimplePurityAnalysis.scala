/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.ll
import org.opalj.br.analyses.{ProjectInformationKeys, SomeProject}
import org.opalj.br.fpcf.{BasicFPCFEagerAnalysisScheduler, FPCFAnalysis, FPCFAnalysisScheduler}
import org.opalj.fpcf.{Entity, OrderedProperty, ProperPropertyComputationResult, PropertyBounds, PropertyKey, PropertyMetaInformation, PropertyStore, Result}

sealed trait SimplePurityPropertyMetaInformation extends PropertyMetaInformation {
    final type Self = SimplePurity
}

sealed trait SimplePurity extends SimplePurityPropertyMetaInformation with OrderedProperty {
    def meet(other: SimplePurity): SimplePurity = {
        (this, other) match {
            case (Pure, Pure) ⇒ Pure
            case (_, _)       ⇒ Impure
        }
    }

    override def checkIsEqualOrBetterThan(e: Entity, other: SimplePurity): Unit = {
        if (meet(other) != other) {
            throw new IllegalArgumentException(s"$e: impossible refinement: $other ⇒ $this")
        }
    }

    final def key: PropertyKey[SimplePurity] = SimplePurity.key
}

case object Pure extends SimplePurity

case object Impure extends SimplePurity

object SimplePurity extends SimplePurityPropertyMetaInformation {
    final val key: PropertyKey[SimplePurity] = PropertyKey.create(
        "SimplePurity",
        Impure
    )
}

class SimplePurityAnalysis(val project: SomeProject) extends FPCFAnalysis {
    def analyzeSimplePurity(function: Function): ProperPropertyComputationResult = {
        Result(function, Impure)
    }
}

trait SimplePurityAnalysisScheduler extends FPCFAnalysisScheduler {
    def derivedProperty: PropertyBounds = PropertyBounds.ub(SimplePurity)

    override def requiredProjectInformation: ProjectInformationKeys = Seq(LLVMFunctionsKey)

    override def uses: Set[PropertyBounds] = Set.empty // TODO: check this later
}

object EagerSimplePurityAnalysis extends SimplePurityAnalysisScheduler with BasicFPCFEagerAnalysisScheduler {
    override def derivesEagerly: Set[PropertyBounds] = Set(derivedProperty)

    override def derivesCollaboratively: Set[PropertyBounds] = Set.empty

    override def start(project: SomeProject, propertyStore: PropertyStore, initData: InitializationData): FPCFAnalysis = {
        val analysis = new SimplePurityAnalysis(project)
        propertyStore.scheduleEagerComputationsForEntities(project.get(LLVMFunctionsKey))(analysis.analyzeSimplePurity)
        analysis
    }
}

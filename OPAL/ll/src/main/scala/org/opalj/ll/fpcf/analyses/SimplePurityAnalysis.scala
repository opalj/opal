/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.ll.fpcf.analyses

import org.opalj.br.analyses.{JavaProjectInformationKeys, ProjectBasedAnalysis, SomeProject}
import org.opalj.br.fpcf.{JavaBasicFPCFEagerAnalysisScheduler, JavaFPCFAnalysisScheduler}
import org.opalj.fpcf._
import org.opalj.ll.LLVMProjectKey
import org.opalj.ll.llvm.value.{Function, GlobalVariable, Store}
import org.opalj.si.FPCFAnalysis

sealed trait SimplePurityPropertyMetaInformation extends PropertyMetaInformation {
    final type Self = SimplePurity
}

sealed trait SimplePurity extends SimplePurityPropertyMetaInformation with OrderedProperty {
    def meet(other: SimplePurity): SimplePurity = {
        (this, other) match {
            case (Pure, Pure) => Pure
            case (_, _)       => Impure
        }
    }

    override def checkIsEqualOrBetterThan(e: Entity, other: SimplePurity): Unit = {
        if (meet(other) != other) {
            throw new IllegalArgumentException(s"$e: impossible refinement: $other => $this")
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

class SimplePurityAnalysis(val project: SomeProject) extends ProjectBasedAnalysis {
    def analyzeSimplePurity(function: Function): ProperPropertyComputationResult = {
        function
            .basicBlocks
            .flatMap(_.instructions)
            .foreach {
                case instruction: Store =>
                    instruction.dst match {
                        case _: GlobalVariable =>
                            return Result(function, Impure)
                        case _ => ()
                    }
                case _ => ()
            }
        Result(function, Pure)
    }
}

trait SimplePurityAnalysisScheduler extends JavaFPCFAnalysisScheduler {
    def derivedProperty: PropertyBounds = PropertyBounds.ub(SimplePurity)

    override def requiredProjectInformation: JavaProjectInformationKeys = Seq(LLVMProjectKey)

    override def uses: Set[PropertyBounds] = Set.empty // TODO: check this later
}

object EagerSimplePurityAnalysis
    extends SimplePurityAnalysisScheduler
    with JavaBasicFPCFEagerAnalysisScheduler {
    override def derivesEagerly: Set[PropertyBounds] = Set(derivedProperty)

    override def derivesCollaboratively: Set[PropertyBounds] = Set.empty

    override def start(
        project:       SomeProject,
        propertyStore: PropertyStore,
        initData:      InitializationData
    ): FPCFAnalysis = {
        val analysis = new SimplePurityAnalysis(project)
        val llvm_project = project.get(LLVMProjectKey)
        propertyStore.scheduleEagerComputationsForEntities(llvm_project.functions)(
            analysis.analyzeSimplePurity
        )
        analysis
    }
}

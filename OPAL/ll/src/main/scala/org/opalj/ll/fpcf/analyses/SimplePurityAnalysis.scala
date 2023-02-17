/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ll
package fpcf
package analyses

import org.opalj.br.analyses.ProjectInformationKeys
import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.BasicFPCFEagerAnalysisScheduler
import org.opalj.br.fpcf.FPCFAnalysis
import org.opalj.br.fpcf.FPCFAnalysisScheduler
import org.opalj.fpcf.Entity
import org.opalj.fpcf.OrderedProperty
import org.opalj.fpcf.ProperPropertyComputationResult
import org.opalj.fpcf.Result
import org.opalj.fpcf.PropertyBounds
import org.opalj.fpcf.PropertyKey
import org.opalj.fpcf.PropertyMetaInformation
import org.opalj.fpcf.PropertyStore
import org.opalj.ll.LLVMProjectKey
import org.opalj.ll.llvm.value.Function
import org.opalj.ll.llvm.value.GlobalVariable
import org.opalj.ll.llvm.value.Store

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

class SimplePurityAnalysis(val project: SomeProject) extends FPCFAnalysis {
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

trait SimplePurityAnalysisScheduler extends FPCFAnalysisScheduler {
    def derivedProperty: PropertyBounds = PropertyBounds.ub(SimplePurity)

    override def requiredProjectInformation: ProjectInformationKeys = Seq(LLVMProjectKey)

    override def uses: Set[PropertyBounds] = Set.empty // TODO: check this later
}

object EagerSimplePurityAnalysis
    extends SimplePurityAnalysisScheduler
    with BasicFPCFEagerAnalysisScheduler {
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

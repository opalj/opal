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
import org.opalj.br.fpcf.properties.ImpureByAnalysis
import org.opalj.br.fpcf.properties.Pure
import org.opalj.br.fpcf.properties.Purity
import org.opalj.fpcf.ProperPropertyComputationResult
import org.opalj.fpcf.PropertyBounds
import org.opalj.fpcf.PropertyStore
import org.opalj.fpcf.Result
import org.opalj.ll.LLVMProjectKey
import org.opalj.ll.llvm.value.Function
import org.opalj.ll.llvm.value.GlobalVariable
import org.opalj.ll.llvm.value.Store

/**
 * Simple llvm purity analysis
 *
 * @author Marc Clement
 */
class LLVMPurityAnalysis(val project: SomeProject) extends FPCFAnalysis {
    def analyzeSimplePurity(function: Function): ProperPropertyComputationResult = {
        function
            .basicBlocks
            .flatMap(_.instructions)
            .foreach {
                case instruction: Store =>
                    instruction.dst match {
                        case _: GlobalVariable =>
                            return Result(function, ImpureByAnalysis)
                        case _ => ()
                    }
                case _ => ()
            }
        Result(function, Pure)
    }
}

trait SimplePurityAnalysisScheduler extends FPCFAnalysisScheduler {
    def derivedProperty: PropertyBounds = PropertyBounds.ub(Purity)

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
        val analysis = new LLVMPurityAnalysis(project)
        val llvm_project = project.get(LLVMProjectKey)
        propertyStore.scheduleEagerComputationsForEntities(llvm_project.functions)(
            analysis.analyzeSimplePurity
        )
        analysis
    }
}

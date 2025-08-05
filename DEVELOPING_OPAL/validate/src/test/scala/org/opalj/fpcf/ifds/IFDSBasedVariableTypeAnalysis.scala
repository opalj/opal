/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package ifds

import java.io.PrintWriter

import org.opalj.br.Method
import org.opalj.br.analyses.DeclaredMethodsKey
import org.opalj.br.analyses.ProjectInformationKeys
import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.ContextProviderKey
import org.opalj.br.fpcf.properties.cg.Callers
import org.opalj.cli.SubsumptionArg
import org.opalj.fpcf.PropertyBounds
import org.opalj.fpcf.PropertyKey
import org.opalj.fpcf.PropertyStore
import org.opalj.fpcf.PropertyStoreKey
import org.opalj.ifds.IFDSAnalysis
import org.opalj.ifds.IFDSAnalysisScheduler
import org.opalj.ifds.IFDSProperty
import org.opalj.ifds.IFDSPropertyMetaInformation
import org.opalj.ifds.Statistics
import org.opalj.tac.fpcf.analyses.ifds.IFDSEvaluationRunner
import org.opalj.tac.fpcf.analyses.ifds.JavaStatement
import org.opalj.tac.fpcf.properties.TACAI

/**
 * A variable type analysis implemented as an IFDS analysis.
 * In contrast to an ordinary variable type analysis, which also determines types of fields,
 * this analysis only determines the types of local variables.
 * The subsuming taint can be mixed in to enable subsuming.
 *
 * @param project The analyzed project.
 *
 * @author Mario Trageser
 */
class IFDSBasedVariableTypeAnalysis(project: SomeProject, subsumeFacts: Boolean = false)
    extends IFDSAnalysis(project, new VariableTypeProblem(project, subsumeFacts), VTAResult)

class IFDSBasedVariableTypeAnalysisScheduler(subsumeFacts: Boolean = false)
    extends IFDSAnalysisScheduler[VTAFact, Method, JavaStatement] {
    override def init(p: SomeProject, ps: PropertyStore) = new IFDSBasedVariableTypeAnalysis(p, subsumeFacts)
    override def property: IFDSPropertyMetaInformation[JavaStatement, VTAFact] = VTAResult
    override val uses: Set[PropertyBounds] = Set(PropertyBounds.finalP(TACAI), PropertyBounds.finalP(Callers))
    override def requiredProjectInformation: ProjectInformationKeys =
        Seq(DeclaredMethodsKey, ContextProviderKey, PropertyStoreKey)
}

/**
 * The IFDSProperty for this analysis.
 */
case class VTAResult(flows: Map[JavaStatement, Set[VTAFact]], debugData: Map[JavaStatement, Set[VTAFact]] = Map.empty)
    extends IFDSProperty[JavaStatement, VTAFact] {

    override type Self = VTAResult
    override def create(result: Map[JavaStatement, Set[VTAFact]]): IFDSProperty[JavaStatement, VTAFact] =
        new VTAResult(result)
    override def create(
        result:    Map[JavaStatement, Set[VTAFact]],
        debugData: Map[JavaStatement, Set[VTAFact]]
    ): IFDSProperty[JavaStatement, VTAFact] = new VTAResult(result, debugData)

    override def key: PropertyKey[VTAResult] = VTAResult.key
}

object VTAResult extends IFDSPropertyMetaInformation[JavaStatement, VTAFact] {

    override type Self = VTAResult
    override def create(result: Map[JavaStatement, Set[VTAFact]]): IFDSProperty[JavaStatement, VTAFact] =
        new VTAResult(result)
    override def create(
        result:    Map[JavaStatement, Set[VTAFact]],
        debugData: Map[JavaStatement, Set[VTAFact]]
    ): IFDSProperty[JavaStatement, VTAFact] = new VTAResult(result, debugData)

    val key: PropertyKey[VTAResult] = PropertyKey.create("VTAnew", new VTAResult(Map.empty))
}

object IFDSBasedVariableTypeAnalysisRunner extends IFDSEvaluationRunner {

    override protected val additionalDescription: String = "variable types (VTA)"

    protected class VTAConfig(args: Array[String]) extends IFDSRunnerConfig(args) {

        args(
            SubsumptionArg
        )
    }

    override protected type ConfigType = VTAConfig

    override protected def createConfig(args: Array[String]): VTAConfig = new VTAConfig(args)

    override def analysisClass(analysisConfig: VTAConfig): IFDSBasedVariableTypeAnalysisScheduler =
        new IFDSBasedVariableTypeAnalysisScheduler(analysisConfig.get(SubsumptionArg, false))

    override type AdditionalResultsType = Statistics

    override protected def additionalEvaluationResult(
        analysis: IFDSAnalysis[_, _, _]
    ): Option[Statistics] =
        if (analysis.ifdsProblem.subsumeFacts) Some(analysis.statistics) else None

    override protected def writeAdditionalEvaluationResultsToFile(
        writer:                      PrintWriter,
        additionalEvaluationResults: Statistics
    ): Unit = {
        writer.println(s"Tries to subsume: ${additionalEvaluationResults.subsumeTries}")
        writer.println(s"Successful subsumes: ${additionalEvaluationResults.subsumptions}")
    }
}

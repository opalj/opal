/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package ifds

import java.io.File
import java.io.PrintWriter

import org.opalj.br.Method
import org.opalj.br.analyses.DeclaredMethodsKey
import org.opalj.br.analyses.ProjectInformationKeys
import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.ContextProviderKey
import org.opalj.br.fpcf.PropertyStoreKey
import org.opalj.br.fpcf.properties.cg.Callers
import org.opalj.fpcf.PropertyBounds
import org.opalj.fpcf.PropertyKey
import org.opalj.fpcf.PropertyStore
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

class IFDSBasedVariableTypeAnalysisRunnerIFDS(subsumeFacts: Boolean = false) extends IFDSEvaluationRunner {

    override def analysisClass: IFDSBasedVariableTypeAnalysisScheduler =
        new IFDSBasedVariableTypeAnalysisScheduler(subsumeFacts)

    override protected def additionalEvaluationResult(
        analysis: IFDSAnalysis[_, _, _]
    ): Option[Object] =
        if (analysis.ifdsProblem.subsumeFacts) Some(analysis.statistics) else None

    override protected def writeAdditionalEvaluationResultsToFile(
        writer:                      PrintWriter,
        additionalEvaluationResults: Seq[Object]
    ): Unit = {
        val numberOfSubsumptions = additionalEvaluationResults.map(_.asInstanceOf[Statistics])
        val length = additionalEvaluationResults.length
        val tries = numberOfSubsumptions.map(_.subsumeTries).sum / length
        val successes = numberOfSubsumptions.map(_.subsumptions).sum / length
        writer.println(s"Average tries to subsume: $tries")
        writer.println(s"Average successful subsumes: $successes")
    }
}

object IFDSBasedVariableTypeAnalysisRunnerIFDS {
    def main(args: Array[String]): Unit = {
        if (args.contains("--help")) {
            println("Potential parameters:")
            println(" -seq (to use the SequentialPropertyStore)")
            println(" -l2 (to use the l2 domain instead of the default l1 domain)")
            println(" -delay (for a three seconds delay before the taint flow analysis is started)")
            println(" -debug (for debugging mode in the property store)")
            println(" -evalSchedulingStrategies (evaluates all available scheduling strategies)")
            println(" -subsumeFacts (enables subsuming)")
            println(" -f <file> (Stores the average runtime to this file)")
        } else {
            val fileIndex = args.indexOf("-f")
            new IFDSBasedVariableTypeAnalysisRunnerIFDS(args.contains("-subsumeFacts")).run(
                args.contains("-debug"),
                args.contains("-l2"),
                args.contains("-evalSchedulingStrategies"),
                if (fileIndex >= 0) Some(new File(args(fileIndex + 1))) else None
            )
        }
    }
}

/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package support
package info

import java.io.File
import java.net.URL
import java.util.concurrent.ConcurrentLinkedQueue
import scala.collection.immutable.ArraySeq
import scala.jdk.CollectionConverters.*

import org.opalj.br.DeclaredMethod
import org.opalj.br.PC
import org.opalj.br.analyses.BasicReport
import org.opalj.br.analyses.DeclaredMethods
import org.opalj.br.analyses.DeclaredMethodsKey
import org.opalj.br.analyses.Project
import org.opalj.br.analyses.ProjectsAnalysisApplication
import org.opalj.br.fpcf.ContextProviderKey
import org.opalj.br.fpcf.analyses.ContextProvider
import org.opalj.br.fpcf.analyses.immutability.LazyClassImmutabilityAnalysis
import org.opalj.br.fpcf.analyses.immutability.LazyTypeImmutabilityAnalysis
import org.opalj.br.fpcf.cli.MultiProjectAnalysisConfig
import org.opalj.br.fpcf.properties.CompileTimePure
import org.opalj.br.fpcf.properties.Context
import org.opalj.br.fpcf.properties.Pure
import org.opalj.br.fpcf.properties.Purity as PurityProperty
import org.opalj.br.fpcf.properties.SideEffectFree
import org.opalj.br.fpcf.properties.cg.Callees
import org.opalj.fpcf.FinalP
import org.opalj.fpcf.FPCFAnalysesManagerKey
import org.opalj.fpcf.PropertyStore
import org.opalj.tac.Call
import org.opalj.tac.DUVar
import org.opalj.tac.ExprStmt
import org.opalj.tac.FunctionCall
import org.opalj.tac.cg.CGBasedCommandLineConfig
import org.opalj.tac.cg.RTACallGraphKey
import org.opalj.tac.fpcf.analyses.LazyFieldLocalityAnalysis
import org.opalj.tac.fpcf.analyses.escape.LazyInterProceduralEscapeAnalysis
import org.opalj.tac.fpcf.analyses.escape.LazyReturnValueFreshnessAnalysis
import org.opalj.tac.fpcf.analyses.fieldaccess.EagerFieldAccessInformationAnalysis
import org.opalj.tac.fpcf.analyses.purity.EagerL2PurityAnalysis
import org.opalj.tac.fpcf.properties.TACAI
import org.opalj.value.ValueInformation

/**
 * Identifies calls to pure/side-effect free methods where the results are not used subsequently.
 *
 * @author Dominik Helm
 */
object UnusedResults extends ProjectsAnalysisApplication {

    protected class UnusedResultsConfig(args: Array[String]) extends MultiProjectAnalysisConfig(args)
        with CGBasedCommandLineConfig {
        val description = "Finds invocations of pure/side-effect free methods where the result is not used"
    }

    protected type ConfigType = UnusedResultsConfig

    protected def createConfig(args: Array[String]): UnusedResultsConfig = new UnusedResultsConfig(args)

    type V = DUVar[ValueInformation]

    override protected def analyze(
        cp:             Iterable[File],
        analysisConfig: UnusedResultsConfig,
        execution:      Int
    ): (Project[URL], BasicReport) = {
        implicit val (project: Project[URL], _) = analysisConfig.setupProject(cp)()
        implicit val (ps, _) = analysisConfig.setupPropertyStore(project)
        analysisConfig.setupCallGaph(project)

        val issues = new ConcurrentLinkedQueue[String]

        implicit val declaredMethods: DeclaredMethods = project.get(DeclaredMethodsKey)

        val callGraph = project.get(RTACallGraphKey)

        project.get(FPCFAnalysesManagerKey).runAll(
            EagerFieldAccessInformationAnalysis,
            LazyInterProceduralEscapeAnalysis,
            LazyReturnValueFreshnessAnalysis,
            LazyFieldLocalityAnalysis,
            // TODO find LazyL1FieldAssignabilityAnalysis,
            LazyClassImmutabilityAnalysis,
            LazyTypeImmutabilityAnalysis,
            EagerL2PurityAnalysis
        )

        implicit val contextProvider: ContextProvider = project.get(ContextProviderKey)

        callGraph.reachableMethods().foreach { context => issues.addAll(analyzeMethod(context).asJava) }

        (project, BasicReport(issues.asScala))
    }

    def analyzeMethod(
        context: Context
    )(
        implicit
        propertyStore:   PropertyStore,
        declaredMethods: DeclaredMethods,
        contextProvider: ContextProvider
    ): Seq[String] = {
        val method = context.method
        if (!method.hasSingleDefinedMethod)
            return Nil;

        val taCodeOption = propertyStore(method.definedMethod, TACAI.key).ub.tac
        if (taCodeOption.isEmpty)
            return Nil;

        val code = taCodeOption.get.stmts

        val callees = propertyStore(method, Callees.key).ub

        val issues = code collect {
            case ExprStmt(pc, call: FunctionCall[V]) =>
                handleCall(method, call, callees.callees(context, pc), pc)
        }

        ArraySeq.unsafeWrapArray(issues) collect { case Some(issue) => issue }
    }

    def handleCall(
        caller:  DeclaredMethod,
        call:    FunctionCall[V],
        callees: Iterator[Context],
        pc:      Int
    )(
        implicit
        propertyStore:   PropertyStore,
        declaredMethods: DeclaredMethods
    ): Option[String] = {
        if (callees.forall { callee =>
                propertyStore(callee, PurityProperty.key) match {
                    case FinalP(CompileTimePure | Pure | SideEffectFree) => true
                    case _                                               => false
                }
            }
        ) {
            val Call(declaringClass, _, name, descriptor) = call
            val callee = declaredMethods(
                declaringClass.asClassType,
                caller.definedMethod.classFile.thisType.packageName,
                declaringClass.asClassType,
                name,
                descriptor
            )
            createIssue(caller, callee, pc)
        } else None

    }

    private def createIssue(
        method: DeclaredMethod,
        target: DeclaredMethod,
        pc:     PC
    ): Some[String] = {
        Some(s"Unused result of call to ${target.toJava} from ${method.toJava} at $pc")
    }
}

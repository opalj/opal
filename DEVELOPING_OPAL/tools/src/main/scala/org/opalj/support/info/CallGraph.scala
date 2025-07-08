/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package support
package info

import scala.language.postfixOps

import java.io.File
import java.io.FileOutputStream
import java.io.PrintWriter
import java.net.URL

import org.rogach.scallop.stringListConverter

import org.opalj.br.DefinedMethod
import org.opalj.br.Field
import org.opalj.br.VirtualDeclaredMethod
import org.opalj.br.analyses.BasicReport
import org.opalj.br.analyses.DeclaredMethods
import org.opalj.br.analyses.DeclaredMethodsKey
import org.opalj.br.analyses.Project
import org.opalj.br.analyses.ProjectsAnalysisApplication
import org.opalj.br.analyses.VirtualFormalParameter
import org.opalj.br.fpcf.ContextProviderKey
import org.opalj.br.fpcf.analyses.ContextProvider
import org.opalj.br.fpcf.cli.MultiProjectAnalysisConfig
import org.opalj.br.fpcf.properties.cg.Callees
import org.opalj.br.fpcf.properties.cg.Callers
import org.opalj.br.fpcf.properties.pointsto.AllocationSitePointsToSet
import org.opalj.cli.ConfigurationNameArg
import org.opalj.cli.OutputFileArg
import org.opalj.cli.PlainArg
import org.opalj.cli.ResultFileArg
import org.opalj.fpcf.Entity
import org.opalj.tac.cg.CallGraphArg
import org.opalj.tac.cg.CallGraphSerializer
import org.opalj.tac.cg.CGBasedCommandLineConfig
import org.opalj.tac.cg.PointsToCallGraphKey
import org.opalj.tac.common.DefinitionSite
import org.opalj.tac.fpcf.analyses.pointsto.ArrayEntity
import org.opalj.tac.fpcf.analyses.pointsto.CallExceptions
import org.opalj.tac.fpcf.analyses.pointsto.MethodExceptions

/**
 * Computes a call graph and reports its size.
 *
 * The default algorithm is RTA.
 *
 * Furthermore, it can be used to print the callees or callers of specific methods.
 *
 * @author Florian Kuebler
 * @author Dominik Helm
 */
object CallGraph extends ProjectsAnalysisApplication {

    protected class CallGraphConfig(args: Array[String]) extends MultiProjectAnalysisConfig(args)
        with CGBasedCommandLineConfig {

        val description = "Compute the number of reachable methods and call edges in the given project"

        args(
            CallGraphArg !,
            CalleesArg,
            CallersArg,
            ConfigurationNameArg !,
            ResultFileArg,
            OutputFileArg
        )

        object CalleesArg extends PlainArg[List[String]] {
            override val name: String = "callees"
            override val argName: String = "method"
            override val description: String =
                "Signatures of methods for which callees should be printed (e.g., toString()java.lang.String"
        }

        object CallersArg extends PlainArg[List[String]] {
            override val name: String = "callers"
            override val argName: String = "method"
            override val description: String =
                "Signatures of methods for which callers should be printed (e.g., toString()java.lang.String"
        }

    }

    protected type ConfigType = CallGraphConfig

    protected def createConfig(args: Array[String]): CallGraphConfig = new CallGraphConfig(args)

    override protected def analyze(
        cp:             Iterable[File],
        analysisConfig: CallGraphConfig,
        execution:      Int
    ): (Project[URL], BasicReport) = {
        val (project, projectTime) = analysisConfig.setupProject()
        implicit val (ps, propertyStoreTime) = analysisConfig.setupPropertyStore(project)
        val (cg, callGraphTime) = analysisConfig.setupCallGaph(project)

        implicit val declaredMethods: DeclaredMethods = project.get(DeclaredMethodsKey)
        val allMethods = declaredMethods.declaredMethods.filter { dm =>
            dm.hasSingleDefinedMethod &&
            (dm.definedMethod.classFile.thisType eq dm.declaringClassType)
        }.to(Iterable)

        val algorithm = analysisConfig(CallGraphArg)

        if (algorithm.isInstanceOf[PointsToCallGraphKey]) {
            val ptss = ps.entities(AllocationSitePointsToSet.key).toList

            println(s"PTSs ${ptss.size}")
            println(s"PTS entries ${ptss.map(p => p.ub.elements.size).sum}")

            val byType = ptss.groupBy(_.e.getClass)

            def getNum(tpe: Class[_ <: Entity]): Int = {
                byType.get(tpe).map(_.size).getOrElse(0)
            }

            def getEntries(tpe: Class[_ <: Entity]): Int = {
                byType.get(tpe).map(_.map(_.ub.numElements).sum).getOrElse(0)
            }

            println(s"DefSite PTSs: ${getNum(classOf[DefinitionSite])}")
            println(s"Parameter PTSs: ${getNum(classOf[VirtualFormalParameter])}")
            println(s"Instance Field PTSs: ${getNum(classOf[(Long, Field)])}")
            println(s"Static Field PTSs: ${getNum(classOf[Field])}")
            println(s"Array PTSs: ${getNum(classOf[ArrayEntity[Long]])}")
            println(s"Return PTSs: ${getNum(classOf[DefinedMethod]) + getNum(classOf[VirtualDeclaredMethod])}")
            println(s"MethodException PTSs: ${getNum(classOf[MethodExceptions])}")
            println(s"CallException PTSs: ${getNum(classOf[CallExceptions])}")

            println(s"DefSite PTS entries: ${getEntries(classOf[DefinitionSite])}")
            println(s"Parameter PTS entries: ${getEntries(classOf[VirtualFormalParameter])}")
            println(s"Instance Field PTS entries: ${getEntries(classOf[(Long, Field)])}")
            println(s"Static Field PTS entries: ${getEntries(classOf[Field])}")
            println(s"Array PTS entries: ${getEntries(classOf[ArrayEntity[Long]])}")
            println(
                s"Return PTS entries: ${getEntries(classOf[DefinedMethod]) + getEntries(classOf[VirtualDeclaredMethod])}"
            )
            println(s"MethodException PTS entries: ${getEntries(classOf[MethodExceptions])}")
            println(s"CallException PTS entries: ${getEntries(classOf[CallExceptions])}")
        }

        val reachableContexts = cg.reachableMethods().to(Iterable)
        val reachableMethods = reachableContexts.map(_.method).toSet

        val numEdges = cg.numEdges

        println(ps.statistics.mkString("\n"))

        implicit val contextProvider: ContextProvider = project.get(ContextProviderKey)

        for (m <- allMethods) {
            val mSig = m.descriptor.toJava(m.name)

            for (methodSignature <- analysisConfig.get(analysisConfig.CalleesArg, List.empty)) {
                if (mSig.contains(methodSignature)) {
                    println(s"Callees of ${m.toJava}:")
                    val calleesProperty = ps(m, Callees.key).ub
                    println(calleesProperty.callerContexts.flatMap { context =>
                        calleesProperty.callSites(context).map {
                            case (pc, callees) => pc -> callees.map(_.method.toJava).mkString(", ")
                        }.toSet.mkString("\t", "\n\t", "\n")
                    })
                }
            }
            for (methodSignature <- analysisConfig.get(analysisConfig.CallersArg, List.empty)) {
                if (mSig.contains(methodSignature)) {
                    println(s"Callers of ${m.toJava}:")
                    println(ps(m, Callers.key).ub.callers(m).iterator.map {
                        case (caller, pc, isDirect) =>
                            s"${caller.toJava}, $pc${if (!isDirect) ", indirect" else ""}"
                    }.iterator.mkString("\t", "\n\t", "\n"))
                }
            }
        }

        if (analysisConfig.get(ResultFileArg).isDefined) {
            CallGraphSerializer.writeCG(cg, ResultFileArg.getResultFile(analysisConfig, execution))
        }

        if (analysisConfig.get(OutputFileArg).isDefined) {
            val output = OutputFileArg.getOutputFile(analysisConfig, execution)
            val newOutputFile = !output.exists()
            val outputWriter = new PrintWriter(new FileOutputStream(output, true))
            try {
                if (newOutputFile) {
                    output.createNewFile()
                    outputWriter.println(
                        "analysisName;project time;propertyStore time;callGraph time;total time;" +
                            "methods;reachable;edges"
                    )
                }

                val totalTime = projectTime + propertyStoreTime + callGraphTime
                outputWriter.println(
                    s"${analysisConfig(ConfigurationNameArg)};${projectTime.toString(false)};" +
                        s"${propertyStoreTime.toString(false)};" +
                        s"${callGraphTime.toString(false)};${totalTime.toString(false)};" +
                        s"${allMethods.size};${reachableMethods.size};$numEdges"
                )

            } finally {
                outputWriter.close()
            }

        }

        val message =
            s"""|# of methods: ${allMethods.size}
                |# of reachable contexts: ${reachableContexts.size}
                |# of reachable methods: ${reachableMethods.size}
                |# of call edges: $numEdges
                |"""

        (project, BasicReport(message.stripMargin('|')))
    }
}

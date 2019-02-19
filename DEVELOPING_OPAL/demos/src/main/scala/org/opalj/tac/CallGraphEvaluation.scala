/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac

import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import java.net.URL

import scala.collection.JavaConverters._

import com.typesafe.config.ConfigValueFactory

import org.opalj.br.analyses.DeclaredMethodsKey
import org.opalj.br.analyses.DefaultOneStepAnalysis
import org.opalj.br.analyses.Project
import org.opalj.br.analyses.ReportableAnalysisResult
import org.opalj.br.fpcf.PropertyStoreKey
import org.opalj.br.fpcf.cg.properties.ReflectionRelatedCallees
import org.opalj.br.fpcf.cg.properties.SerializationRelatedCallees
import org.opalj.br.fpcf.cg.properties.StandardInvokeCallees
import org.opalj.br.MethodDescriptor
import org.opalj.br.analyses.BasicReport
import org.opalj.br.analyses.DeclaredMethods
import org.opalj.br.fpcf.cg.properties.CallersProperty
import org.opalj.ai.domain.l1
import org.opalj.ai.fpcf.properties.AIDomainFactoryKey
import org.opalj.tac.cg.CallGraphSerializer
import org.opalj.tac.cg.CHACallGraphKey
import org.opalj.tac.cg.RTACallGraphKey

object CallGraphEvaluation extends DefaultOneStepAnalysis {

    override def analysisSpecificParametersDescription: String = {
        "[-module=<module>]"+"[-algorithm=CHA|RTA]"+"[-projectName=<name>]"
    }

    override def checkAnalysisSpecificParameters(parameters: Seq[String]): Traversable[String] = {
        val remainingParameters =
            parameters.filter { p ⇒
                !p.startsWith("-algorithm=") &&
                    !p.startsWith("-module=") &&
                    !p.startsWith("-projectName=") &&
                    !(p == "-writeCG")
            }
        super.checkAnalysisSpecificParameters(remainingParameters)
    }

    override def doAnalyze(
        project:       Project[URL],
        parameters:    Seq[String],
        isInterrupted: () ⇒ Boolean
    ): ReportableAnalysisResult = {
        var algorithm: Option[String] = None
        var projectName: Option[String] = None
        var modules: List[String] = Nil
        var writeCG = false

        val algorithmPattern = "-algorithm=(RTA|CHA)".r
        val modulePattern = "-module=(.*)".r
        val projectPattern = "-projectName=(.*)".r

        parameters.foreach {
            case algorithmPattern(algo) ⇒
                if (algorithm.nonEmpty)
                    throw new IllegalArgumentException()
                algorithm = Some(algo)
            case projectPattern(pName) ⇒
                if (projectName.nonEmpty)
                    throw new IllegalArgumentException()
                projectName = Some(pName)
            case modulePattern(module) ⇒
                modules ::= module
            case "-writeCG" ⇒
                writeCG = true

        }

        assert(algorithm.nonEmpty)
        assert(projectName.nonEmpty)

        /*if (modules.isEmpty) {
            modules = List(
                "org.opalj.tac.fpcf.analyses.cg.reflection.TriggeredReflectionRelatedCallsAnalysis",
                "org.opalj.tac.fpcf.analyses.cg.TriggeredSerializationRelatedCallsAnalysis",
                "org.opalj.tac.fpcf.analyses.cg.TriggeredThreadRelatedCallsAnalysis"
            )
        }*/

        val moduleNames = modules.map(_.split('.').last.replace("Triggered", "").replace("RelatedCallsAnalysis", ""))

        modules :::= List(
            "org.opalj.tac.fpcf.analyses.cg.TriggeredFinalizerAnalysisScheduler",
            "org.opalj.tac.fpcf.analyses.cg.TriggeredLoadedClassesAnalysis",
            "org.opalj.tac.fpcf.analyses.cg.TriggeredStaticInitializerAnalysis",
            "org.opalj.tac.fpcf.analyses.cg.TriggeredConfiguredNativeMethodsAnalysis",
            "org.opalj.tac.fpcf.analyses.TriggeredSystemPropertiesAnalysis"
        )

        val config =
            project.config.withValue(
                "org.opalj.tac.cg.CallGraphKey.modules",
                ConfigValueFactory.fromIterable(modules.asJava)
            )

        val p = Project.recreate(project, config)

        val domain = classOf[l1.DefaultDomainWithCFGAndDefUse[_]]
        p.updateProjectInformationKeyInitializationData(
            AIDomainFactoryKey,
            (i: Option[Set[Class[_ <: AnyRef]]]) ⇒ (i match {
                case None               ⇒ Set(domain)
                case Some(requirements) ⇒ requirements + domain
            }): Set[Class[_ <: AnyRef]]
        )

        val start = System.currentTimeMillis()

        val cg = algorithm.get match {
            case "RTA" ⇒ p.get(RTACallGraphKey(false))
            case "CHA" ⇒ p.get(CHACallGraphKey(false))
        }

        val time = System.currentTimeMillis() - start

        if (writeCG) {
            implicit val declaredMethods: DeclaredMethods = p.get(DeclaredMethodsKey)
            val cgFile = new File(s"${algorithm.get}-${moduleNames.mkString("_")}.json")
            CallGraphSerializer.writeCG(cg, cgFile)
        }

        val seconds = (time / 1000)

        var numEdges = cg.numEdges
        val reachableMethods = cg.reachableMethods().toTraversable

        var numStandardEdges = 0
        var numReflectiveEdges = 0
        var numSerializationEdges = 0
        var numThreadEdges = 0

        val ps = p.get(PropertyStoreKey)
        implicit val declaredMethods = p.get(DeclaredMethodsKey)

        for (rm ← reachableMethods) {
            for ((_, tgts) ← ps(rm, StandardInvokeCallees.key).ub.callSites)
                numStandardEdges += tgts.size

            if (moduleNames.contains("Reflection"))
                for ((_, tgts) ← ps(rm, ReflectionRelatedCallees.key).ub.callSites)
                    numReflectiveEdges += tgts.size

            if (moduleNames.contains("Serialization"))
                for ((_, tgts) ← ps(rm, SerializationRelatedCallees.key).ub.callSites)
                    numSerializationEdges += tgts.size

            if (moduleNames.contains("Thread"))
                if ((rm.name == "run" || rm.name == "exit") &&
                    rm.descriptor == MethodDescriptor.NoArgsAndReturnVoid &&
                    ps(rm, CallersProperty.key).ub.hasVMLevelCallers)
                    numThreadEdges += 1
        }
        numEdges += numThreadEdges

        val outFile = new File(s"results/${projectName.get}.tsv")
        val writeHeader =
            if (!outFile.exists()) {
                outFile.getParentFile.mkdirs()
                outFile.createNewFile()
                true
            } else
                false
        val writer = new PrintWriter(new FileWriter(outFile, true))

        if (writeHeader)
            writer.println("config\t# rm\t# edges\t# standard edges\t# reflection edges\t# serialization edges\t# thread edges\ttime")

        writer.println(s"${algorithm.get},${moduleNames.mkString(",")}\t${reachableMethods.size}\t$numEdges\t$numStandardEdges\t$numReflectiveEdges\t$numSerializationEdges\t$numThreadEdges\t$seconds")

        writer.flush()
        writer.close()

        BasicReport("")
    }
}

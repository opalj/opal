/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package support
package info

import java.io.File
import java.net.URL

import org.opalj.fpcf.PropertyStore
import org.opalj.br.analyses.BasicReport
import org.opalj.br.analyses.DeclaredMethods
import org.opalj.br.analyses.DeclaredMethodsKey
import org.opalj.br.analyses.ProjectAnalysisApplication
import org.opalj.br.analyses.Project
import org.opalj.br.fpcf.PropertyStoreKey
import org.opalj.br.fpcf.FPCFAnalysesManagerKey
import org.opalj.br.fpcf.properties.cg.Callees
import org.opalj.br.fpcf.properties.cg.Callers
import org.opalj.br.fpcf.properties.pointsto.TypeBasedPointsToSet
import org.opalj.tac.cg.CallGraphSerializer
import org.opalj.tac.cg.CHACallGraphKey
import org.opalj.tac.cg.PointsToCallGraphKey
import org.opalj.tac.cg.RTACallGraphKey
import org.opalj.tac.fpcf.analyses.pointsto.AndersenStylePointsToAnalysisScheduler

/**
 * Computes a call graph and reports its size.
 *
 * You can specify the call-graph algorithm:
 *  -algorithm=CHA for an CHA-based call graph
 *  -algorithm=RTA for an RTA-based call graph
 *  -algorithm=PointsTo for a points-to based call graph
 * The default algorithm is RTA.
 *
 * Please also specify whether the target (-cp=) is an application or a library using "-projectConf=".
 * Predefined configurations `ApplicationProject.conf` or `LibraryProject.conf` can be used here.
 *
 * Furthermore, it can be used to print the callees or callers of specific methods.
 * To do so, add -callers=m, where m is the method name/signature using Java notation, as parameter
 * (for callees use -callees=m).
 *
 * @author Florian Kuebler
 */
object CallGraph extends ProjectAnalysisApplication {

    override def title: String = "Call Graph Analysis"

    override def description: String = {
        "Provides the number of reachable methods and call edges in the give project."
    }

    override def analysisSpecificParametersDescription: String = {
        "[-algorithm=CHA|RTA|PointsTo]"+"[-callers=method]"+"[-callees=method]"+"[-writeCG=file]"
    }

    private val algorithmRegex = "-algorithm=(CHA|RTA|PointsTo)".r

    override def checkAnalysisSpecificParameters(parameters: Seq[String]): Traversable[String] = {
        val remainingParameters =
            parameters.filter { p ⇒
                !p.matches(algorithmRegex.regex) &&
                    !p.startsWith("-callers=") &&
                    !p.startsWith("-callees=") &&
                    !p.startsWith("-writeCG=")
            }
        super.checkAnalysisSpecificParameters(remainingParameters)
    }

    // todo: we would like to print the edges for a given method
    override def doAnalyze(
        project:       Project[URL],
        parameters:    Seq[String],
        isInterrupted: () ⇒ Boolean
    ): BasicReport = {
        var calleesSigs: List[String] = Nil
        var callersSigs: List[String] = Nil
        var cgAlgorithm: String = "RTA"
        var cgFile: Option[String] = None

        val callersRegex = "-callers=(.*)".r
        val calleesRegex = "-callees=(.*)".r
        val writeCGRegex = "-writeCG=(.*)".r

        parameters.foreach {
            case callersRegex(methodSig) ⇒ callersSigs ::= methodSig
            case calleesRegex(methodSig) ⇒ calleesSigs ::= methodSig
            case algorithmRegex(algo)    ⇒ cgAlgorithm = algo
            case writeCGRegex(fileName) ⇒
                if (cgFile.isEmpty)
                    cgFile = Some(fileName)
                else throw new IllegalArgumentException("-writeCG was set twice")

        }

        implicit val declaredMethods: DeclaredMethods = project.get(DeclaredMethodsKey)
        val allMethods = declaredMethods.declaredMethods.filter { dm ⇒
            dm.hasSingleDefinedMethod &&
                (dm.definedMethod.classFile.thisType eq dm.declaringClassType)
        }.toTraversable

        implicit val ps: PropertyStore = project.get(PropertyStoreKey)

        val cg = cgAlgorithm match {
            case "CHA"      ⇒ project.get(CHACallGraphKey)
            case "RTA"      ⇒ project.get(RTACallGraphKey)
            case "PointsTo" ⇒ project.get(PointsToCallGraphKey)
        }

        if (cgAlgorithm != "PointsTo") {
            val manager = project.get(FPCFAnalysesManagerKey)
            manager.runAll(AndersenStylePointsToAnalysisScheduler)
        }

        println(ps.entities(TypeBasedPointsToSet.key).size)

        val reachableMethods = cg.reachableMethods().toTraversable

        val numEdges = cg.numEdges

        println(ps.statistics.mkString("\n"))

        println(calleesSigs.mkString("\n"))
        println(callersSigs.mkString("\n"))

        for (m ← allMethods) {
            val mSig = m.descriptor.toJava(m.name)

            for (methodSignature ← calleesSigs) {
                if (mSig.contains(methodSignature)) {
                    println(s"Callees of ${m.toJava}:")
                    println(ps(m, Callees.key).ub.callSites().map {
                        case (pc, callees) ⇒ pc → callees.map(_.toJava).mkString(", ")
                    }.mkString("\t", "\n\t", "\n"))
                }
            }
            for (methodSignature ← callersSigs) {
                if (mSig.contains(methodSignature)) {
                    println(s"Callers of ${m.toJava}:")
                    println(ps(m, Callers.key).ub.callers.map {
                        case (caller, pc, isDirect) ⇒
                            s"${caller.toJava}, $pc${if (!isDirect) ", indirect" else ""}"
                    }.mkString("\t", "\n\t", "\n"))
                }
            }
        }

        if (cgFile.nonEmpty) {
            CallGraphSerializer.writeCG(cg, new File(cgFile.get))
        }

        val message =
            s"""|# of methods: ${allMethods.size}
                |# of reachable methods: ${reachableMethods.size}
                |# of call edges: $numEdges
                |"""

        BasicReport(message.stripMargin('|'))
    }
}

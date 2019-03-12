/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package support
package info

import java.net.URL

import org.opalj.fpcf.PropertyStore
import org.opalj.br.analyses.BasicReport
import org.opalj.br.analyses.DeclaredMethods
import org.opalj.br.analyses.DeclaredMethodsKey
import org.opalj.br.analyses.DefaultOneStepAnalysis
import org.opalj.br.analyses.Project
import org.opalj.br.fpcf.PropertyStoreKey
import org.opalj.br.fpcf.cg.properties.Callees
import org.opalj.br.fpcf.cg.properties.CallersProperty
import org.opalj.tac.cg.CHACallGraphKey
import org.opalj.tac.cg.RTACallGraphKey
//import org.opalj.ai.fpcf.analyses.LazyL0BaseAIAnalysis
//import org.opalj.tac.fpcf.analyses.TACAITransformer

/**
 * Computes a call graph and reports its size.
 *
 * Please specify the call-graph algorithm:
 *  -algorithm=CHA for an CHA-based call graph
 *  -algorithm=RTA for an RTA-based call graph
 *
 * Please also specify whether the target (-cp=) is an application or a library:
 *  -mode=app for an application
 *  -mode=library for a library
 *
 * Furthermore, it can be used to print the callees or callers of specific methods.
 * To do so, add -callers=m, where m is the method name/signature using Java notation, as parameter
 * (for callees use -callees=m).
 *
 * The default algorithm is an RTA.
 * Use -algorithm=CHA to compute a CHA-based call graph.
 *
 * @author Florian Kuebler
 */
object CallGraph extends DefaultOneStepAnalysis {

    override def title: String = "Call Graph Analysis"

    override def description: String = {
        "Provides the number of reachable methods and call edges in the give project."
    }

    override def analysisSpecificParametersDescription: String = {
        "[-mode=app|library]"+"[-algorithm=CHA|RTA]"+"[-callers=method]"+"[-callees=method]"
    }

    override def checkAnalysisSpecificParameters(parameters: Seq[String]): Traversable[String] = {
        val remainingParameters =
            parameters.filter { p ⇒
                !p.startsWith("-callers=") &&
                    !p.startsWith("-callees=") &&
                    !p.startsWith("-mode=") &&
                    !p.startsWith("-algorithm=")
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
        var isLibrary: Option[Boolean] = None
        var cgAlgorithm: Option[String] = None

        val callersRegex = "-callers=(.*)".r
        val calleesRegex = "-callees=(.*)".r
        val modeRegex = "-mode=(app|library)".r
        val algorithmRegex = "-algorithm=(CHA|RTA)".r

        parameters.foreach {
            case callersRegex(methodSig) ⇒ callersSigs ::= methodSig
            case calleesRegex(methodSig) ⇒ calleesSigs ::= methodSig
            case modeRegex("app") ⇒
                if (isLibrary.isEmpty)
                    isLibrary = Some(false)
                else throw new IllegalArgumentException("-mode was set twice")
            case modeRegex("library") ⇒
                if (isLibrary.isEmpty)
                    isLibrary = Some(true)
                else throw new IllegalArgumentException("-mode was set twice")
            case algorithmRegex(algo) ⇒
                if (cgAlgorithm.isEmpty)
                    cgAlgorithm = Some(algo)
                else throw new IllegalArgumentException("-algorithm was set twice")

        }

        // todo: also manipulate the entry points and instantiated types keys
        if (isLibrary.isEmpty)
            throw new IllegalArgumentException("-mode was not set")

        if (cgAlgorithm.isEmpty)
            throw new IllegalArgumentException("-algorithm was not set")

        implicit val declaredMethods: DeclaredMethods = project.get(DeclaredMethodsKey)
        val allMethods = declaredMethods.declaredMethods.filter { dm ⇒
            dm.hasSingleDefinedMethod &&
                (dm.definedMethod.classFile.thisType eq dm.declaringClassType)
        }.toTraversable

        implicit val ps: PropertyStore = project.get(PropertyStoreKey)

        val cg = cgAlgorithm.get match {
            case "CHA" ⇒ project.get(CHACallGraphKey(isLibrary.get))
            case "RTA" ⇒ project.get(RTACallGraphKey(isLibrary.get))
        }

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
                    println(ps(m, CallersProperty.key).ub.callers.map {
                        case (caller, pc, isDirect) ⇒
                            s"${caller.toJava}, $pc${if(!isDirect) ", indirect" else ""}"
                    }.mkString("\t", "\n\t", "\n"))
                }
            }
        }

        val message =
            s"""|# of methods: ${allMethods.size}
                |# of reachable methods: ${reachableMethods.size}
                |# of call edges: $numEdges
                |"""

        BasicReport(message.stripMargin('|'))
    }
}

/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package support
package info

import java.net.URL

import org.opalj.collection.immutable.Chain
import org.opalj.fpcf.ComputationSpecification
import org.opalj.br.analyses.BasicReport
import org.opalj.br.analyses.DeclaredMethodsKey
import org.opalj.br.analyses.DefaultOneStepAnalysis
import org.opalj.br.analyses.Project
import org.opalj.br.fpcf.FPCFAnalysesManagerKey
import org.opalj.br.fpcf.FPCFAnalysis
import org.opalj.br.fpcf.PropertyStoreKey
import org.opalj.br.fpcf.cg.properties.Callees
import org.opalj.br.fpcf.cg.properties.CallersProperty
import org.opalj.br.fpcf.cg.properties.NoCallers
import org.opalj.br.fpcf.cg.properties.ReflectionRelatedCallees
import org.opalj.br.fpcf.cg.properties.SerializationRelatedCallees
import org.opalj.br.fpcf.cg.properties.StandardInvokeCallees
import org.opalj.br.fpcf.cg.properties.ThreadRelatedIncompleteCallSites
import org.opalj.tac.fpcf.analyses.cg.EagerLibraryEntryPointsAnalysis
import org.opalj.tac.fpcf.analyses.cg.LazyCalleesAnalysis
import org.opalj.tac.fpcf.analyses.cg.RTACallGraphAnalysisScheduler
import org.opalj.tac.fpcf.analyses.cg.TriggeredConfiguredNativeMethodsAnalysis
import org.opalj.tac.fpcf.analyses.cg.TriggeredFinalizerAnalysisScheduler
import org.opalj.tac.fpcf.analyses.cg.TriggeredInstantiatedTypesAnalysis
import org.opalj.tac.fpcf.analyses.cg.TriggeredLoadedClassesAnalysis
import org.opalj.tac.fpcf.analyses.cg.TriggeredSerializationRelatedCallsAnalysis
import org.opalj.tac.fpcf.analyses.cg.TriggeredStaticInitializerAnalysis
import org.opalj.tac.fpcf.analyses.cg.TriggeredThreadRelatedCallsAnalysis
import org.opalj.tac.fpcf.analyses.cg.reflection.TriggeredReflectionRelatedCallsAnalysis
import org.opalj.tac.fpcf.analyses.TriggeredSystemPropertiesAnalysis
//import org.opalj.ai.fpcf.analyses.LazyL0BaseAIAnalysis
//import org.opalj.tac.fpcf.analyses.TACAITransformer
import org.opalj.tac.fpcf.analyses.LazyTACAIProvider

/**
 * Computes a RTA based call graph and reports its size.
 * Furthermore, it can be used to print the callees or callers of specific methods.
 * To do so, add -callers=m, where m is the method name/signature using Java notation, as parameter
 * (for callees use -callees=m).
 *
 * @author Florian Kuebler
 */
object RTACallGraph extends DefaultOneStepAnalysis {

    override def title: String = "Field Locality"

    override def description: String = {
        "Provides the number of reachable methods and call edges in the give project."
    }

    override def analysisSpecificParametersDescription: String = {
        "[-callers=method]"+"[-callees=method]"
    }

    override def checkAnalysisSpecificParameters(parameters: Seq[String]): Traversable[String] = {
        val remainingParameters =
            parameters.filter { p ⇒
                !p.startsWith("-callers=") && !p.startsWith("-callees=")
            }
        super.checkAnalysisSpecificParameters(remainingParameters)
    }

    // todo: we would like to print the edges for a given method
    override def doAnalyze(
        project:       Project[URL],
        parameters:    Seq[String],
        isInterrupted: () ⇒ Boolean
    ): BasicReport = {
        implicit val declaredMethods = project.get(DeclaredMethodsKey)
        val allMethods = declaredMethods.declaredMethods.filter { dm ⇒
            dm.hasSingleDefinedMethod &&
                (dm.definedMethod.classFile.thisType eq dm.declaringClassType)
        }.toTraversable

        implicit val ps = project.get(PropertyStoreKey)

        val calleesAnalysis = LazyCalleesAnalysis(
            Set(
                StandardInvokeCallees,
                SerializationRelatedCallees,
                ReflectionRelatedCallees,
                ThreadRelatedIncompleteCallSites
            )
        )

        project.get(FPCFAnalysesManagerKey).runAll(
            List(
                // LazyL0BaseAIAnalysis,
                // TACAITransformer,
                LazyTACAIProvider,
                /* Call Graph Analyses */
                RTACallGraphAnalysisScheduler,
                TriggeredStaticInitializerAnalysis,
                TriggeredLoadedClassesAnalysis,
                TriggeredFinalizerAnalysisScheduler,
                TriggeredThreadRelatedCallsAnalysis,
                TriggeredSerializationRelatedCallsAnalysis,
                TriggeredReflectionRelatedCallsAnalysis,
                TriggeredInstantiatedTypesAnalysis,
                TriggeredConfiguredNativeMethodsAnalysis,
                TriggeredSystemPropertiesAnalysis,
                EagerLibraryEntryPointsAnalysis,
                calleesAnalysis
            ),
            { css: Chain[ComputationSpecification[FPCFAnalysis]] ⇒
                if (css.contains(calleesAnalysis)) {
                    allMethods.foreach { dm ⇒ ps.force(dm, br.fpcf.cg.properties.Callees.key) }
                }
            }
        )

        val callersProperties = ps(allMethods, CallersProperty.key)
        assert(callersProperties.forall(_.isFinal))

        val reachableMethods = callersProperties.filterNot(_.ub eq NoCallers).map(_.ub)

        val numEdges = reachableMethods.foldLeft(0) { (accEdges, callersProperty) ⇒
            callersProperty.callers.size + accEdges
        }

        var calleesSigs: List[String] = Nil
        var callersSigs: List[String] = Nil

        val callersRegex = "-callers=(.*)".r
        val calleesRegex = "-callees=(.*)".r
        parameters.foreach {
            case callersRegex(methodSig) ⇒ callersSigs ::= methodSig
            case calleesRegex(methodSig) ⇒ calleesSigs ::= methodSig
        }

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
                        case (caller, pc) ⇒ caller.toJava → pc
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

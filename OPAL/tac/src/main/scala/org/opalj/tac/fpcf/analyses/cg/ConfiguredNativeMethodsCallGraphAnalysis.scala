/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package cg

import org.opalj.fpcf.EOptionP
import org.opalj.fpcf.EPS
import org.opalj.fpcf.FinalP
import org.opalj.fpcf.InterimPartialResult
import org.opalj.fpcf.NoResult
import org.opalj.fpcf.PartialResult
import org.opalj.fpcf.ProperPropertyComputationResult
import org.opalj.fpcf.Property
import org.opalj.fpcf.PropertyBounds
import org.opalj.fpcf.PropertyComputationResult
import org.opalj.fpcf.PropertyKind
import org.opalj.fpcf.PropertyStore
import org.opalj.fpcf.Results
import org.opalj.fpcf.SomeEPS
import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.FPCFAnalysis
import org.opalj.br.DeclaredMethod
import org.opalj.br.analyses.DeclaredMethods
import org.opalj.br.analyses.DeclaredMethodsKey
import org.opalj.br.analyses.ProjectInformationKeys
import org.opalj.tac.fpcf.properties.cg.Callees
import org.opalj.tac.fpcf.properties.cg.Callers
import org.opalj.tac.fpcf.properties.cg.NoCallers
import org.opalj.br.fpcf.BasicFPCFTriggeredAnalysisScheduler
import org.opalj.tac.cg.TypeProviderKey

/**
 * Add calls from configured native methods to the call graph.
 * Calls can be specialized under the config key [[configKey]].
 *
 * @example specify that `registerNatives` will call `initializeSystemClass`.
 * {
 *  cf = "java/lang/System",
 *  name = "registerNatives",
 *  desc = "()V",
 *  methodInvocations = [
 *      { cf = "java/lang/System", name = "initializeSystemClass", desc = "()V" }
 *  ]
 * }
 *
 * @author Florian Kuebler
 */
class ConfiguredNativeMethodsCallGraphAnalysis private[analyses] (
        final val project: SomeProject
) extends FPCFAnalysis {

    val configKey = "org.opalj.fpcf.analyses.ConfiguredNativeMethodsAnalysis"

    private[this] implicit val declaredMethods: DeclaredMethods = project.get(DeclaredMethodsKey)
    private[this] implicit val typeProvider: TypeProvider = project.get(TypeProviderKey)

    private[this] val nativeMethodData: Map[DeclaredMethod, Option[Array[MethodDescription]]] = {
        ConfiguredMethods.reader.read(
            p.config, configKey
        ).nativeMethods.map { v => (v.method, v.methodInvocations) }.toMap
    }

    def analyze(dm: DeclaredMethod): PropertyComputationResult = {
        val callers = propertyStore(dm, Callers.key)
        (callers: @unchecked) match {
            case FinalP(NoCallers) =>
                // nothing to do, since there is no caller
                return NoResult;

            case eps: EPS[_, _] =>
                if (eps.ub eq NoCallers) {
                    // we can not create a dependency here, so the analysis is not allowed to create
                    // such a result
                    throw new IllegalStateException("illegal immediate result for callers")
                }
            // the method is reachable, so we analyze it!
        }

        // we only allow defined methods
        if (!dm.hasSingleDefinedMethod) return NoResult

        val method = dm.definedMethod

        if (!method.isNative || !nativeMethodData.contains(dm)) {
            return NoResult;
        }

        val tgtsOpt = nativeMethodData(dm)
        if (tgtsOpt.isEmpty) {
            return NoResult;
        }

        returnResults(callers, null, tgtsOpt.get)
    }

    def c(
        oldCallers: Callers
    )(
        update: SomeEPS
    ): ProperPropertyComputationResult = {
        val newCallers = update.asInstanceOf[EPS[DeclaredMethod, Callers]]
        val tgtsOpt = nativeMethodData(newCallers.e)
        returnResults(newCallers, oldCallers, tgtsOpt.get)
    }

    def returnResults(
        eOptP: EOptionP[DeclaredMethod, Callers], seen: Callers, tgts: Array[MethodDescription]
    ): ProperPropertyComputationResult = {
        val callers = eOptP.ub

        var results: Iterator[PartialResult[_, _ >: Null <: Property]] = Iterator.empty
        callers.forNewCalleeContexts(seen, eOptP.e) { calleeContext =>
            val directCalls = new DirectCalls()
            for (tgt <- tgts) {
                val tgtMethod = tgt.method(declaredMethods)
                directCalls.addCall(
                    calleeContext, 0, typeProvider.expandContext(calleeContext, tgtMethod, 0)
                )
            }
            results ++= directCalls.partialResults(calleeContext)
        }

        Results(InterimPartialResult(Set(eOptP), c(eOptP.ub)), results)
    }
}

object ConfiguredNativeMethodsCallGraphAnalysisScheduler extends BasicFPCFTriggeredAnalysisScheduler {
    override def requiredProjectInformation: ProjectInformationKeys =
        Seq(DeclaredMethodsKey, TypeProviderKey)

    override def uses: Set[PropertyBounds] = PropertyBounds.ubs(Callers)

    override def derivesCollaboratively: Set[PropertyBounds] = PropertyBounds.ubs(Callees)

    override def derivesEagerly: Set[PropertyBounds] = Set.empty

    override def register(
        p: SomeProject, ps: PropertyStore, unused: Null
    ): ConfiguredNativeMethodsCallGraphAnalysis = {
        val analysis = new ConfiguredNativeMethodsCallGraphAnalysis(p)
        // register the analysis for initial values for callers (i.e. methods becoming reachable)
        ps.registerTriggeredComputation(Callers.key, analysis.analyze)
        analysis
    }

    override def triggeredBy: PropertyKind = Callers
}
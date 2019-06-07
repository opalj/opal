/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package cg

import org.opalj.fpcf.EPS
import org.opalj.fpcf.FinalP
import org.opalj.fpcf.NoResult
import org.opalj.fpcf.PropertyBounds
import org.opalj.fpcf.PropertyComputationResult
import org.opalj.fpcf.PropertyKind
import org.opalj.fpcf.PropertyStore
import org.opalj.fpcf.Results
import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.FPCFAnalysis
import org.opalj.br.fpcf.FPCFTriggeredAnalysisScheduler
import org.opalj.br.DeclaredMethod
import org.opalj.br.analyses.DeclaredMethods
import org.opalj.br.analyses.DeclaredMethodsKey
import org.opalj.br.analyses.VirtualFormalParameters
import org.opalj.br.analyses.VirtualFormalParametersKey
import org.opalj.br.fpcf.properties.cg.Callees
import org.opalj.br.fpcf.properties.cg.Callers
import org.opalj.br.fpcf.properties.cg.NoCallers
import org.opalj.tac.fpcf.analyses.pointsto.ConfiguredNativeMethods
import org.opalj.tac.fpcf.analyses.pointsto.MethodDescription

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

    private[this] implicit val declaredMethods: DeclaredMethods = p.get(DeclaredMethodsKey)
    private[this] implicit val virtualFormalParameters: VirtualFormalParameters = p.get(VirtualFormalParametersKey)

    // TODO remove dependency to classes in pointsto package
    private[this] val nativeMethodData: Map[DeclaredMethod, Option[Array[MethodDescription]]] = {
        ConfiguredNativeMethods.reader.read(
            p.config, configKey
        ).nativeMethods.map { v ⇒ (v.method, v.methodInvocations) }.toMap
    }

    def analyze(dm: DeclaredMethod): PropertyComputationResult = {
        (propertyStore(dm, Callers.key): @unchecked) match {
            case FinalP(NoCallers) ⇒
                // nothing to do, since there is no caller
                return NoResult;

            case eps: EPS[_, _] ⇒
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

        val directCalls = new DirectCalls()
        for (tgt ← tgtsOpt.get) {
            val tgtMethod = tgt.entity
            directCalls.addCall(dm, tgtMethod, 0)
        }

        Results(directCalls.partialResults(dm))
    }
}

object ConfiguredNativeMethodsCallGraphAnalysisScheduler extends FPCFTriggeredAnalysisScheduler {
    override type InitializationData = Null

    override def uses: Set[PropertyBounds] = PropertyBounds.ubs(Callers)

    override def derivesCollaboratively: Set[PropertyBounds] = PropertyBounds.ubs(Callees)

    override def derivesEagerly: Set[PropertyBounds] = Set.empty

    override def init(p: SomeProject, ps: PropertyStore): Null = {
        null
    }

    override def beforeSchedule(p: SomeProject, ps: PropertyStore): Unit = {}

    override def register(
        p: SomeProject, ps: PropertyStore, unused: Null
    ): ConfiguredNativeMethodsCallGraphAnalysis = {
        val analysis = new ConfiguredNativeMethodsCallGraphAnalysis(p)
        // register the analysis for initial values for callers (i.e. methods becoming reachable)
        ps.registerTriggeredComputation(Callers.key, analysis.analyze)
        analysis
    }

    override def afterPhaseScheduling(ps: PropertyStore, analysis: FPCFAnalysis): Unit = {}

    override def afterPhaseCompletion(
        p:        SomeProject,
        ps:       PropertyStore,
        analysis: FPCFAnalysis
    ): Unit = {}

    /**
     * Specifies the kind of the properties that will trigger the analysis to be registered.
     */
    override def triggeredBy: PropertyKind = Callers
}
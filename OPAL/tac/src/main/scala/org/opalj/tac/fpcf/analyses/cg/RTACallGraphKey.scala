/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package cg

import org.opalj.fpcf.ComputationSpecification
import org.opalj.fpcf.PropertyStore
import org.opalj.br.analyses.DeclaredMethodsKey
import org.opalj.br.analyses.ProjectInformationKey
import org.opalj.br.analyses.ProjectInformationKeys
import org.opalj.br.analyses.SomeProject
import org.opalj.br.analyses.cg.InitialEntryPointsKey
import org.opalj.br.analyses.cg.InitialInstantiatedTypesKey
import org.opalj.br.analyses.DeclaredMethods
import org.opalj.br.fpcf.PropertyStoreKey
import org.opalj.br.fpcf.cg.properties.ReflectionRelatedCallees
import org.opalj.br.fpcf.cg.properties.StandardInvokeCallees
import org.opalj.br.fpcf.FPCFAnalysesManagerKey
import org.opalj.br.fpcf.cg.properties.Callees
import org.opalj.br.fpcf.cg.properties.SerializationRelatedCallees
import org.opalj.br.fpcf.cg.properties.ThreadRelatedIncompleteCallSites
import org.opalj.br.fpcf.FPCFAnalysis
import org.opalj.tac.fpcf.analyses.cg.reflection.TriggeredReflectionRelatedCallsAnalysis

/**
 * A [[ProjectInformationKey]] to compute a [[CallGraph]] based on RTA.
 *
 * @param handleStaticInitializer should the call-graph algorithm handle the invocation of static
 *                                initializers?
 * @param handleFinalizer should the call-graph algorithm handle the invocation of finalizers?
 * @param handleReflection should the call-graph algorithm handle calls to the reflection API?
 *                         See [[org.opalj.tac.fpcf.analyses.cg.reflection.ReflectionRelatedCallsAnalysis]]
 *                         for detailed information about its configuration.
 * @param handleSerialization should the call-graph algorithm handle the serialization API?
 * @param handleThreads should the call-graph algorithm handle the thread API?
 * @param handleConfiguredNativeMethods should the call-graph algorithm handle the invocation
 *                                      predefined native methods?
 *                                      See [[ConfiguredNativeMethodsAnalysis]] for details about
 *                                      the configuration.
 *
 * Note, that initial instantiated types ([[InitialInstantiatedTypesKey]]) and entry points
 * ([[InitialEntryPointsKey]]) can be configured before hand.
 * Furthermore, you can configure the analysis mode (Library or Application) in the configuration
 * of these keys.
 *
 * @author Florian Kuebler
 *
 */
case class RTACallGraphKey(
        handleStaticInitializer:       Boolean = true,
        handleFinalizer:               Boolean = true,
        handleReflection:              Boolean = true,
        handleSerialization:           Boolean = true,
        handleThreads:                 Boolean = true,
        handleConfiguredNativeMethods: Boolean = true
) extends ProjectInformationKey[CallGraph, Nothing] {

    override protected def requirements: ProjectInformationKeys = {
        Seq(
            DeclaredMethodsKey,
            InitialEntryPointsKey,
            InitialInstantiatedTypesKey,
            PropertyStoreKey,
            FPCFAnalysesManagerKey
        )
    }

    override protected def compute(project: SomeProject): CallGraph = {
        implicit val declaredMethods: DeclaredMethods = project.get(DeclaredMethodsKey)
        implicit val ps: PropertyStore = project.get(PropertyStoreKey)

        val manager = project.get(FPCFAnalysesManagerKey)

        var analyses: List[ComputationSpecification[FPCFAnalysis]] =
            List(
                RTACallGraphAnalysisScheduler,
                TriggeredInstantiatedTypesAnalysis,
                LazyCalleesAnalysis(
                    Set(
                        StandardInvokeCallees,
                        SerializationRelatedCallees,
                        ReflectionRelatedCallees,
                        ThreadRelatedIncompleteCallSites
                    )
                ),
                LazyTACAIProvider
            )

        if (handleStaticInitializer) {
            analyses ::= TriggeredStaticInitializerAnalysis
            analyses ::= TriggeredLoadedClassesAnalysis
        }

        if (handleFinalizer)
            analyses ::= TriggeredFinalizerAnalysisScheduler

        if (handleReflection) {
            analyses ::= TriggeredReflectionRelatedCallsAnalysis
            analyses ::= TriggeredSystemPropertiesAnalysis
        }

        if (handleSerialization)
            analyses ::= TriggeredSerializationRelatedCallsAnalysis

        if (handleThreads)
            analyses ::= TriggeredThreadRelatedCallsAnalysis

        if (handleConfiguredNativeMethods)
            analyses ::= TriggeredConfiguredNativeMethodsAnalysis

        manager.runAll(analyses)

        // force the computation of the callees, as they have to be aggregated first
        declaredMethods.declaredMethods.foreach { dm ⇒
            ps.force(dm, Callees.key)
        }
        ps.waitOnPhaseCompletion()

        new CallGraph()
    }
}

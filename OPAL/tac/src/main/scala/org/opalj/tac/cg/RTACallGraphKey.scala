/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package cg

import org.opalj.collection.immutable.Chain
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
import org.opalj.tac.fpcf.analyses.cg.LazyCalleesAnalysis
import org.opalj.tac.fpcf.analyses.cg.RTACallGraphAnalysisScheduler
import org.opalj.tac.fpcf.analyses.cg.TriggeredFinalizerAnalysisScheduler
import org.opalj.tac.fpcf.analyses.cg.TriggeredInstantiatedTypesAnalysis
import org.opalj.tac.fpcf.analyses.cg.TriggeredLoadedClassesAnalysis
import org.opalj.tac.fpcf.analyses.LazyTACAIProvider
import org.opalj.tac.fpcf.analyses.cg.TriggeredStaticInitializerAnalysis
import org.opalj.tac.fpcf.analyses.TriggeredSystemPropertiesAnalysis
import org.opalj.tac.fpcf.analyses.cg.EagerLibraryEntryPointsAnalysis
import org.opalj.tac.fpcf.analyses.cg.TriggeredConfiguredNativeMethodsAnalysis
import org.opalj.tac.fpcf.analyses.cg.TriggeredSerializationRelatedCallsAnalysis
import org.opalj.tac.fpcf.analyses.cg.TriggeredThreadRelatedCallsAnalysis

/**
 * A [[ProjectInformationKey]] to compute a [[CallGraph]] based on RTA.
 *
 * @param handleStaticInitializer should the call-graph algorithm handle the invocation of static
 *                                initializers?
 * @param handleFinalizer should the call-graph algorithm handle the invocation of finalizers?
 * @param handleReflection              should the call-graph algorithm handle calls to the reflection API?
 *                                      See [[org.opalj.tac.fpcf.analyses.cg.reflection.ReflectionRelatedCallsAnalysis]]
 *                                      for detailed information about its configuration.
 * @param handleSerialization           should the call-graph algorithm handle the serialization API?
 * @param handleThreads                 should the call-graph algorithm handle the thread API?
 * @param handleConfiguredNativeMethods should the call-graph algorithm handle the invocation
 *                                      predefined native methods?
 *                                      See [[org.opalj.tac.fpcf.analyses.cg.ConfiguredNativeMethodsAnalysis]] for details about
 *                                      the configuration.
 * @param isLibrary                     should the [[org.opalj.tac.fpcf.analyses.cg.EagerLibraryEntryPointsAnalysis]] be scheduled?
 *
 *                                      Note, that initial instantiated types ([[InitialInstantiatedTypesKey]]) and entry points
 *                                      ([[InitialEntryPointsKey]]) can be configured before hand.
 *                                      Furthermore, you can configure the analysis mode (Library or Application) in the configuration
 *                                      of these keys.
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
        handleConfiguredNativeMethods: Boolean = true,
        isLibrary:                     Boolean = true
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

        val calleesAnalysis = LazyCalleesAnalysis(
            Set(
                StandardInvokeCallees,
                SerializationRelatedCallees,
                ReflectionRelatedCallees,
                ThreadRelatedIncompleteCallSites
            )
        )

        var analyses: List[ComputationSpecification[FPCFAnalysis]] =
            List(
                RTACallGraphAnalysisScheduler,
                TriggeredInstantiatedTypesAnalysis,
                calleesAnalysis,
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

        if (isLibrary)
            analyses ::= EagerLibraryEntryPointsAnalysis

        manager.runAll(
            analyses,
            { css: Chain[ComputationSpecification[FPCFAnalysis]] ⇒
                if (css.contains(calleesAnalysis)) {
                    declaredMethods.declaredMethods.foreach { dm ⇒
                        ps.force(dm, Callees.key)
                    }
                }
            }
        )

        new CallGraph()
    }
}

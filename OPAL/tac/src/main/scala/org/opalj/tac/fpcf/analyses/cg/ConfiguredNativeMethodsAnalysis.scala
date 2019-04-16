/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package cg

import net.ceedubs.ficus.Ficus._
import net.ceedubs.ficus.readers.ArbitraryTypeReader._

import org.opalj.collection.immutable.IntTrieSet
import org.opalj.collection.immutable.UIDSet
import org.opalj.fpcf.EOptionP
import org.opalj.fpcf.EPS
import org.opalj.fpcf.FinalP
import org.opalj.fpcf.NoResult
import org.opalj.fpcf.PartialResult
import org.opalj.fpcf.ProperPropertyComputationResult
import org.opalj.fpcf.PropertyBounds
import org.opalj.fpcf.PropertyComputationResult
import org.opalj.fpcf.PropertyKey
import org.opalj.fpcf.PropertyStore
import org.opalj.fpcf.Result
import org.opalj.fpcf.Results
import org.opalj.br.fpcf.cg.properties.NoCallers
import org.opalj.br.fpcf.cg.properties.StandardInvokeCalleesImplementation
import org.opalj.br.DeclaredMethod
import org.opalj.br.Method
import org.opalj.br.MethodDescriptor
import org.opalj.br.ObjectType
import org.opalj.br.analyses.DeclaredMethods
import org.opalj.br.analyses.DeclaredMethodsKey
import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.cg.properties.CallersProperty
import org.opalj.br.fpcf.BasicFPCFTriggeredAnalysisScheduler
import org.opalj.br.fpcf.cg.properties.InstantiatedTypes
import org.opalj.br.fpcf.FPCFAnalysis

/**
 * Handles the effect of certain (configured native methods) to the call graph.
 *
 * @author Dominik Helm
 * @author Florian Kübler
 */
class ConfiguredNativeMethodsAnalysis private[analyses] (
        final val project: SomeProject
) extends FPCFAnalysis {

    private case class NativeMethodData(
            cf:                String,
            m:                 String,
            desc:              String,
            instantiatedTypes: Option[Seq[String]],
            reachableMethods:  Option[Seq[ReachableMethod]]
    )

    private case class ReachableMethod(cf: String, m: String, desc: String)

    private val nativeMethodData: Map[(String, String, String), (Option[Seq[String]], Option[Seq[ReachableMethod]])] = {
        project.config.as[Iterator[NativeMethodData]](
            "org.opalj.fpcf.analyses.ConfiguredNativeMethodsAnalysis.nativeMethods"
        ).map { action ⇒
                (action.cf, action.m, action.desc) →
                    ((action.instantiatedTypes, action.reachableMethods))
            }.toMap
    }

    private[this] implicit val declaredMethods: DeclaredMethods = project.get(DeclaredMethodsKey)

    def getInstantiatedTypesUB(
        instantiatedTypesEOptP: EOptionP[SomeProject, InstantiatedTypes]
    ): UIDSet[ObjectType] = {
        instantiatedTypesEOptP match {
            case eps: EPS[_, _] ⇒ eps.ub.types
            case _              ⇒ UIDSet.empty
        }
    }

    def analyze(declaredMethod: DeclaredMethod): PropertyComputationResult = {
        (propertyStore(declaredMethod, CallersProperty.key): @unchecked) match {
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
        if (!declaredMethod.hasSingleDefinedMethod)
            return NoResult;

        val method = declaredMethod.definedMethod

        if (method.isNative)
            return handleNativeMethod(declaredMethod, method);

        NoResult
    }

    /**
     * Handles configured calls and instantiations for a native method.
     */
    def handleNativeMethod(
        declaredMethod: DeclaredMethod,
        m:              Method
    ): PropertyComputationResult = {

        /**
         * Creates partial results for instantiated types given by their FQNs.
         */
        def instantiatedTypesResultOption(fqns: Seq[String]): Option[ProperPropertyComputationResult] = {
            val instantiatedTypesUB =
                getInstantiatedTypesUB(propertyStore(project, InstantiatedTypes.key))

            val newInstantiatedTypes =
                UIDSet(fqns.map(ObjectType(_)).filterNot(instantiatedTypesUB.contains): _*)

            if (newInstantiatedTypes.nonEmpty)
                Some(PartialResult(
                    p,
                    InstantiatedTypes.key,
                    InstantiatedTypesAnalysis.update(p, newInstantiatedTypes)
                ))
            else
                None
        }

        /**
         * Creates the results for callees and callers properties for the given methods.
         */
        def calleesResults(
            reachableMethods: Seq[ReachableMethod]
        ): List[ProperPropertyComputationResult] = {
            val calleesAndCallers = new CalleesAndCallers()
            for (reachableMethod ← reachableMethods) {
                val classType = ObjectType(reachableMethod.cf)
                val name = reachableMethod.m
                val descriptor = MethodDescriptor(reachableMethod.desc)
                val callee =
                    declaredMethods(classType, classType.packageName, classType, name, descriptor)
                calleesAndCallers.updateWithCall(declaredMethod, callee, 0)
            }
            val callees =
                new StandardInvokeCalleesImplementation(calleesAndCallers.callees, IntTrieSet.empty)
            Result(declaredMethod, callees) :: calleesAndCallers.partialResultsForCallers
        }

        val methodDataO =
            nativeMethodData.get((m.classFile.thisType.fqn, m.name, m.descriptor.toJVMDescriptor))

        if (methodDataO.isEmpty)
            return NoResult;

        val (instantiatedTypesO, reachableMethodsO) = methodDataO.get

        val instantiatedTypesResult =
            if (instantiatedTypesO.isDefined)
                instantiatedTypesResultOption(instantiatedTypesO.get)
            else
                None

        if (reachableMethodsO.isDefined) {
            val callResults = calleesResults(reachableMethodsO.get)
            if (instantiatedTypesO.isDefined) {
                Results(callResults ++ instantiatedTypesResult)
            } else Results(callResults)
        } else if (instantiatedTypesResult.isDefined) {
            instantiatedTypesResult.get
        } else {
            NoResult
        }
    }
}

object TriggeredConfiguredNativeMethodsAnalysis extends BasicFPCFTriggeredAnalysisScheduler {

    override def uses: Set[PropertyBounds] = Set(
        PropertyBounds.ub(CallersProperty),
        PropertyBounds.ub(InstantiatedTypes)
    )

    override def triggeredBy: PropertyKey[CallersProperty] = CallersProperty.key

    override def derivesCollaboratively: Set[PropertyBounds] = Set(
        PropertyBounds.ub(CallersProperty),
        PropertyBounds.ub(InstantiatedTypes)
    )

    override def derivesEagerly: Set[PropertyBounds] = Set.empty

    override def register(
        p: SomeProject, ps: PropertyStore, unused: Null
    ): ConfiguredNativeMethodsAnalysis = {
        val analysis = new ConfiguredNativeMethodsAnalysis(p)

        ps.registerTriggeredComputation(triggeredBy, analysis.analyze)

        analysis
    }
}

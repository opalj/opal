/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package cg

import net.ceedubs.ficus.Ficus._
import net.ceedubs.ficus.readers.ArbitraryTypeReader._

import org.opalj.collection.immutable.UIDSet
import org.opalj.fpcf.EOptionP
import org.opalj.fpcf.EPS
import org.opalj.fpcf.FinalP
import org.opalj.fpcf.NoResult
import org.opalj.fpcf.PartialResult
import org.opalj.fpcf.ProperPropertyComputationResult
import org.opalj.fpcf.PropertyBounds
import org.opalj.fpcf.PropertyComputationResult
import org.opalj.fpcf.PropertyKind
import org.opalj.fpcf.PropertyStore
import org.opalj.fpcf.Results
import org.opalj.br.fpcf.cg.properties.NoCallers
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
import org.opalj.br.fpcf.cg.properties.Callees

/**
 * Handles the effect of certain (configured native methods) to the set of instantiated types.
 *
 *
 * @author Dominik Helm
 * @author Florian Kuebler
 */
// TODO: rename this class as it only affects the instantiated types!
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
        ): TraversableOnce[ProperPropertyComputationResult] = {
            val calleesAndCallers = new DirectCalls()
            for (reachableMethod ← reachableMethods) {
                val classType = ObjectType(reachableMethod.cf)
                val name = reachableMethod.m
                val descriptor = MethodDescriptor(reachableMethod.desc)
                val callee =
                    declaredMethods(classType, classType.packageName, classType, name, descriptor)
                calleesAndCallers.addCall(declaredMethod, callee, 0)
            }
            calleesAndCallers.partialResults(declaredMethod)
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
                Results(instantiatedTypesResult.get, callResults)
            } else Results(callResults)
        } else if (instantiatedTypesResult.isDefined) {
            instantiatedTypesResult.get
        } else {
            NoResult
        }
    }
}

object TriggeredConfiguredNativeMethodsAnalysis extends BasicFPCFTriggeredAnalysisScheduler {
    override def uses: Set[PropertyBounds] =
        PropertyBounds.ubs(Callees, CallersProperty, InstantiatedTypes)

    override def derivesCollaboratively: Set[PropertyBounds] =
        PropertyBounds.ubs(Callees, CallersProperty, InstantiatedTypes)

    override def derivesEagerly: Set[PropertyBounds] = Set.empty

    override def register(
        p: SomeProject, ps: PropertyStore, unused: Null
    ): ConfiguredNativeMethodsAnalysis = {
        val analysis = new ConfiguredNativeMethodsAnalysis(p)

        ps.registerTriggeredComputation(CallersProperty.key, analysis.analyze)

        analysis
    }

    override def triggeredBy: PropertyKind = CallersProperty
}

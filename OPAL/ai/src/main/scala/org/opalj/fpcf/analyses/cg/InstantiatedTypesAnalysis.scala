/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package analyses
package cg

import scala.language.existentials

import org.opalj.br.DeclaredMethod
import org.opalj.br.Method
import org.opalj.br.ObjectType
import org.opalj.br.analyses.SomeProject
import org.opalj.br.analyses.cg.InitialInstantiatedTypesKey
import org.opalj.collection.immutable.UIDSet
import org.opalj.fpcf.cg.properties.CallersProperty
import org.opalj.fpcf.cg.properties.InstantiatedTypes
import org.opalj.fpcf.cg.properties.InstantiatedTypesFakePropertyFinal
import org.opalj.fpcf.cg.properties.InstantiatedTypesFakePropertyNonFinal
import org.opalj.fpcf.cg.properties.NoCallers
import org.opalj.tac.Assignment
import org.opalj.tac.ExprStmt
import org.opalj.tac.New
import org.opalj.tac.fpcf.properties.TACAI

class InstantiatedTypesAnalysis private[analyses] (
        final val project: SomeProject
) extends FPCFAnalysis {

    // todo maybe do this in before schedule
    private[this] val initialInstantiatedTypes: UIDSet[ObjectType] =
        UIDSet(project.get(InitialInstantiatedTypesKey).toSeq: _*)

    def analyze(declaredMethod: DeclaredMethod): PropertyComputationResult = {
        propertyStore(declaredMethod, CallersProperty.key) match {
            case FinalEP(_, NoCallers) ⇒
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

        // we only allow defined methods with declared type eq. to the class of the method
        if (method.classFile.thisType != declaredMethod.declaringClassType)
            return NoResult;

        if (method.body.isEmpty)
            // happens in particular for native methods
            return NoResult;

        val tacEP = propertyStore(method, TACAI.key)

        if (tacEP.hasProperty)
            processMethod(declaredMethod, tacEP)
        else {
            SimplePIntermediateResult(
                declaredMethod,
                InstantiatedTypesFakePropertyNonFinal,
                Seq(tacEP),
                continuationForTAC(declaredMethod)
            )
        }
    }

    private[this] def continuationForTAC(declaredMethod: DeclaredMethod)(
        someEPS: SomeEPS
    ): PropertyComputationResult = someEPS match {
        case ESimplePS(_, _: TACAI, _) ⇒
            processMethod(declaredMethod, someEPS.asInstanceOf[EPS[Method, TACAI]])
        case _                       ⇒ throw new RuntimeException(s"unexpected update $someEPS")
    }

    def getInstantiatedTypesUB(
        instantiatedTypesEOptP: EOptionP[SomeProject, InstantiatedTypes]
    ): UIDSet[ObjectType] = {
        instantiatedTypesEOptP match {
            case eps: EPS[_, _] ⇒ eps.ub.types
            case _              ⇒ initialInstantiatedTypes
        }
    }

    private[this] def processMethod(
        declaredMethod: DeclaredMethod, tacEP: EOptionP[Method, TACAI]
    ): PropertyComputationResult = {

        val tac = tacEP.ub.tac.get

        // the set of types that are definitely initialized at this point in time
        val instantiatedTypesEOptP = propertyStore(project, InstantiatedTypes.key)

        // the upper bound for type instantiations, seen so far
        // in case they are not yet computed, we use the initialTypes
        val instantiatedTypesUB: UIDSet[ObjectType] = getInstantiatedTypesUB(instantiatedTypesEOptP)

        var newInstantiatedTypes: UIDSet[ObjectType] = UIDSet.empty

        tac.stmts.foreach {
            case Assignment(_, _, New(_, allocatedType)) ⇒
                if (!instantiatedTypesUB.contains(allocatedType)) {
                    newInstantiatedTypes += allocatedType
                }

            case ExprStmt(_, New(_, allocatedType)) ⇒
                if (!instantiatedTypesUB.contains(allocatedType)) {
                    newInstantiatedTypes += allocatedType
                }

            case _ ⇒
        }

        val fakeResult = if (tacEP.isFinal)
            Result(declaredMethod, InstantiatedTypesFakePropertyFinal)
        else
            SimplePIntermediateResult(
                declaredMethod,
                InstantiatedTypesFakePropertyNonFinal,
                Seq(tacEP), continuationForTAC(declaredMethod)
            )

        if (newInstantiatedTypes.nonEmpty || instantiatedTypesEOptP.hasNoProperty)
            Results(
                fakeResult,
                InstantiatedTypesAnalysis.partialResultForInstantiatedTypes(
                    p, newInstantiatedTypes, initialInstantiatedTypes
                )
            )
        else fakeResult
    }
}

object InstantiatedTypesAnalysis {
    def partialResultForInstantiatedTypes(
        p:                        SomeProject,
        newInstantiatedTypes:     UIDSet[ObjectType],
        initialInstantiatedTypes: UIDSet[ObjectType]
    ): PartialResult[SomeProject, InstantiatedTypes] = {
        PartialResult[SomeProject, InstantiatedTypes](p, InstantiatedTypes.key,
            {
                case IntermediateESimpleP(_, ub) ⇒
                    Some(IntermediateESimpleP(
                        p,
                        ub.updated(newInstantiatedTypes)
                    ))

                case _: EPK[_, _] ⇒
                    Some(IntermediateESimpleP(
                        p,
                        InstantiatedTypes.initial(newInstantiatedTypes, initialInstantiatedTypes)
                    ))

                case r ⇒ throw new IllegalStateException(s"unexpected previous result $r")
            })
    }
}

object EagerInstantiatedTypesAnalysis extends FPCFEagerAnalysisScheduler {
    override type InitializationData = InstantiatedTypesAnalysis

    override def uses: Set[PropertyKind] = Set(InstantiatedTypes, CallersProperty)

    override def derives: Set[PropertyKind] = Set(InstantiatedTypes)

    override def start(
        p: SomeProject, ps: PropertyStore, analysis: InstantiatedTypesAnalysis
    ): FPCFAnalysis = {
        analysis
    }

    override def init(p: SomeProject, ps: PropertyStore): InstantiatedTypesAnalysis = {
        val analysis = new InstantiatedTypesAnalysis(p)

        ps.registerTriggeredComputation(CallersProperty.key, analysis.analyze)

        analysis
    }

    override def beforeSchedule(p: SomeProject, ps: PropertyStore): Unit = {}

    override def afterPhaseCompletion(p: SomeProject, ps: PropertyStore): Unit = {}
}

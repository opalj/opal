/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package cg

import scala.language.existentials

import org.opalj.collection.immutable.UIDSet
import org.opalj.fpcf.EOptionP
import org.opalj.fpcf.EPK
import org.opalj.fpcf.EPS
import org.opalj.fpcf.FinalP
import org.opalj.fpcf.InterimEUBP
import org.opalj.fpcf.InterimPartialResult
import org.opalj.fpcf.InterimUBP
import org.opalj.fpcf.NoResult
import org.opalj.fpcf.PartialResult
import org.opalj.fpcf.PropertyBounds
import org.opalj.fpcf.PropertyComputationResult
import org.opalj.fpcf.PropertyStore
import org.opalj.fpcf.SomeEPS
import org.opalj.fpcf.UBP
import org.opalj.br.DeclaredMethod
import org.opalj.br.Method
import org.opalj.br.ObjectType
import org.opalj.br.analyses.SomeProject
import org.opalj.br.analyses.cg.InitialInstantiatedTypesKey
import org.opalj.br.fpcf.cg.properties.CallersProperty
import org.opalj.br.fpcf.cg.properties.InstantiatedTypes
import org.opalj.br.fpcf.cg.properties.NoCallers
import org.opalj.br.fpcf.FPCFAnalysis
import org.opalj.br.fpcf.FPCFTriggeredAnalysisScheduler
import org.opalj.tac.fpcf.properties.TACAI

class InstantiatedTypesAnalysis private[analyses] (
        final val project: SomeProject
) extends FPCFAnalysis {

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

        // we only allow defined methods with declared type eq. to the class of the method
        if (method.classFile.thisType != declaredMethod.declaringClassType)
            return NoResult;

        if (method.body.isEmpty)
            // happens in particular for native methods
            return NoResult;

        val tacEP = propertyStore(method, TACAI.key)

        if (tacEP.hasUBP && tacEP.ub.tac.isDefined)
            processMethod(declaredMethod, tacEP)
        else {
            InterimPartialResult(
                None,
                Seq(tacEP),
                continuationForTAC(declaredMethod)
            )
        }
    }

    private[this] def continuationForTAC(declaredMethod: DeclaredMethod)(
        someEPS: SomeEPS
    ): PropertyComputationResult = someEPS match {
        case UBP(tac: TACAI) if tac.tac.isDefined ⇒
            processMethod(declaredMethod, someEPS.asInstanceOf[EPS[Method, TACAI]])
        case UBP(_: TACAI) ⇒
            InterimPartialResult(
                None,
                Seq(someEPS),
                continuationForTAC(declaredMethod)
            )
        case _ ⇒ throw new RuntimeException(s"unexpected update $someEPS")
    }

    def getInstantiatedTypesUB(
        instantiatedTypesEOptP: EOptionP[SomeProject, InstantiatedTypes]
    ): UIDSet[ObjectType] = {
        instantiatedTypesEOptP match {
            case eps: EPS[_, _] ⇒ eps.ub.types
            case _              ⇒ UIDSet.empty
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

        if (tacEP.isRefinable) {
            InterimPartialResult(
                if (newInstantiatedTypes.nonEmpty || instantiatedTypesEOptP.isEPK)
                    Some(PartialResult(
                    p,
                    InstantiatedTypes.key,
                    InstantiatedTypesAnalysis.update(
                        p, newInstantiatedTypes
                    )
                ))
                else
                    None,
                Some(tacEP),
                continuationForTAC(declaredMethod)
            )
        } else if (newInstantiatedTypes.nonEmpty || instantiatedTypesEOptP.isEPK) {
            PartialResult(
                project,
                InstantiatedTypes.key,
                InstantiatedTypesAnalysis.update(p, newInstantiatedTypes)
            )
        } else {
            NoResult
        }
    }
}

object InstantiatedTypesAnalysis {
    def update(
        p:                    SomeProject,
        newInstantiatedTypes: UIDSet[ObjectType]
    )(
        eop: EOptionP[SomeProject, InstantiatedTypes]
    ): Option[EPS[SomeProject, InstantiatedTypes]] = eop match {
        case InterimUBP(ub) ⇒
            Some(InterimEUBP(p, ub.updated(newInstantiatedTypes)))

        case _: EPK[_, _] ⇒
            throw new IllegalStateException(
                "the instantiated types property should be pre initialized"
            )

        case r ⇒ throw new IllegalStateException(s"unexpected previous result $r")
    }
}

object TriggeredInstantiatedTypesAnalysis extends FPCFTriggeredAnalysisScheduler {

    override type InitializationData = Null

    override def uses: Set[PropertyBounds] = PropertyBounds.ubs(InstantiatedTypes, CallersProperty)

    override def derivesCollaboratively: Set[PropertyBounds] = PropertyBounds.ubs(InstantiatedTypes)

    override def derivesEagerly: Set[PropertyBounds] = Set.empty

    override def register(p: SomeProject, ps: PropertyStore, unused: Null): FPCFAnalysis = {
        val analysis = new InstantiatedTypesAnalysis(p)
        ps.registerTriggeredComputation(CallersProperty.key, analysis.analyze)
        analysis
    }

    override def init(p: SomeProject, ps: PropertyStore): Null = {
        val initialInstantiatedTypes = UIDSet(p.get(InitialInstantiatedTypesKey).toSeq: _*)

        ps.preInitialize[SomeProject, InstantiatedTypes](p, InstantiatedTypes.key) {
            case _: EPK[_, _] ⇒ InterimEUBP(p, InstantiatedTypes(initialInstantiatedTypes))
            case eps          ⇒ throw new IllegalStateException(s"unexpected property: $eps")
        }

        null
    }

    override def beforeSchedule(p: SomeProject, ps: PropertyStore): Unit = {}

    override def afterPhaseScheduling(ps: PropertyStore, analysis: FPCFAnalysis): Unit = {}

    override def afterPhaseCompletion(
        p:        SomeProject,
        ps:       PropertyStore,
        analysis: FPCFAnalysis
    ): Unit = {}
}

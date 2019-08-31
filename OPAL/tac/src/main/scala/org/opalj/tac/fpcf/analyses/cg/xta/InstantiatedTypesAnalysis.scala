/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package cg
package xta

import org.opalj.br.DeclaredMethod
import org.opalj.br.ObjectType
import org.opalj.br.PCAndInstruction
import org.opalj.br.ReferenceType
import org.opalj.br.analyses.DeclaredMethods
import org.opalj.br.analyses.DeclaredMethodsKey
import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.FPCFAnalysis
import org.opalj.br.fpcf.FPCFTriggeredAnalysisScheduler
import org.opalj.br.fpcf.properties.cg.Callers
import org.opalj.br.fpcf.properties.cg.InstantiatedTypes
import org.opalj.br.fpcf.properties.cg.NoCallers
import org.opalj.br.instructions.INVOKESPECIAL
import org.opalj.br.instructions.NEW
import org.opalj.collection.immutable.UIDSet
import org.opalj.fpcf.EOptionP
import org.opalj.fpcf.EPK
import org.opalj.fpcf.EPS
import org.opalj.fpcf.Entity
import org.opalj.fpcf.FinalP
import org.opalj.fpcf.InterimEP
import org.opalj.fpcf.InterimEUBP
import org.opalj.fpcf.InterimPartialResult
import org.opalj.fpcf.InterimUBP
import org.opalj.fpcf.NoResult
import org.opalj.fpcf.PartialResult
import org.opalj.fpcf.PropertyBounds
import org.opalj.fpcf.PropertyComputationResult
import org.opalj.fpcf.PropertyKey
import org.opalj.fpcf.PropertyStore
import org.opalj.fpcf.Results
import org.opalj.fpcf.SomeEPS

import scala.collection.mutable.ListBuffer

/**
 * Marks types as instantiated if their constructor is invoked. Constructors invoked by subclass
 * constructors do not result in additional instantiated types.
 *
 * This analysis is adapted from the RTA version. Instead of adding the instantiations to the type
 * set of the Project, they are added to the type set of the calling method. Which entity the type
 * is attached to depends on the call graph variant used.
 *
 * @author Florian Kuebler
 * @author Andreas Bauer
 */
class InstantiatedTypesAnalysis private[analyses] (
        final val project:     SomeProject,
        val setEntitySelector: SetEntitySelector
) extends FPCFAnalysis {
    implicit private val declaredMethods: DeclaredMethods = project.get(DeclaredMethodsKey)

    def analyze(declaredMethod: DeclaredMethod): PropertyComputationResult = {

        // only constructors may initialize a class
        if (declaredMethod.name != "<init>")
            return NoResult;

        val callersEOptP = propertyStore(declaredMethod, Callers.key)

        val callersUB: Callers = (callersEOptP: @unchecked) match {
            case FinalP(NoCallers) ⇒
                // nothing to do, since there is no caller
                return NoResult;

            case eps: EPS[_, _] ⇒
                if (eps.ub eq NoCallers) {
                    // we can not create a dependency here, so the analysis is not allowed to create
                    // such a result
                    throw new IllegalStateException("illegal immediate result for callers")
                } else {
                    eps.ub
                }
            // the method is reachable, so we analyze it!
        }

        val declaredType = declaredMethod.declaringClassType.asObjectType

        val cfOpt = project.classFile(declaredType)

        // abstract classes can never be instantiated
        cfOpt.foreach { cf ⇒
            if (cf.isAbstract)
                return NoResult;
        }

        processCallers(declaredMethod, declaredType, callersEOptP, callersUB, Set.empty)
    }

    private[this] def processCallers(
        declaredMethod: DeclaredMethod,
        declaredType:   ObjectType,
        callersEOptP:   EOptionP[DeclaredMethod, Callers],
        callersUB:      Callers,
        seenCallers:    Set[DeclaredMethod]
    ): PropertyComputationResult = {
        var newSeenCallers = seenCallers
        val partialResults = new ListBuffer[PartialResult[SetEntity, InstantiatedTypes]]()
        for {
            (caller, _, _) ← callersUB.callers
            // if we already analyzed the caller, we do not need to do it twice
            // note, that this is only needed for the continuation
            if !seenCallers.contains(caller)
        } {
            processSingleCaller(declaredMethod, declaredType, caller, partialResults)

            // remember the caller so we don't process it again later
            newSeenCallers += caller
        }

        if (callersEOptP.isFinal) {
            NoResult
        } else {
            val reRegistration =
                InterimPartialResult(
                    Some(callersEOptP),
                    continuation(declaredMethod, declaredType, newSeenCallers)
                )

            Results(reRegistration, partialResults)
        }
    }

    private[this] def processSingleCaller(
        declaredMethod: DeclaredMethod,
        declaredType:   ObjectType,
        caller:         DeclaredMethod,
        partialResults: ListBuffer[PartialResult[SetEntity, InstantiatedTypes]]
    ): Unit = {
        if (caller.name != "<init>") {
            partialResults += partialResult(declaredType, caller);
            return ;
        }

        // the constructor is called from another constructor. it is only an new instantiated
        // type if it was no super call. Thus the caller must be a subtype
        if (!classHierarchy.isSubtypeOf(caller.declaringClassType, declaredType)) {
            partialResults += partialResult(declaredType, caller);
            return ;
        }

        // actually it must be the direct subtype! -- we did the first check to return early
        project.classFile(caller.declaringClassType.asObjectType).foreach { cf ⇒
            cf.superclassType.foreach { supertype ⇒
                if (supertype != declaredType) {
                    partialResults += partialResult(declaredType, caller);
                    return ;
                }
            }
        }

        // if the caller is not available, we have to assume that it was no super call
        if (!caller.hasSingleDefinedMethod) {
            partialResults += partialResult(declaredType, caller);
            return ;
        }

        val callerMethod = caller.definedMethod

        // if the caller has no body, we have to assume that it was no super call
        if (callerMethod.body.isEmpty) {
            partialResults += partialResult(declaredType, caller);
            return ;
        }

        val supercall = INVOKESPECIAL(
            declaredType,
            isInterface = false,
            "<init>",
            declaredMethod.descriptor
        )

        val pcsOfSuperCalls = callerMethod.body.get.collectInstructionsWithPC {
            case pcAndInstr @ PCAndInstruction(_, `supercall`) ⇒ pcAndInstr
        }

        assert(pcsOfSuperCalls.nonEmpty)

        // there can be only one super call, so there must be an explicit call
        if (pcsOfSuperCalls.size > 1) {
            partialResults += partialResult(declaredType, caller);
            return ;
        }

        // there is exactly the current call as potential super call, it still might no super
        // call if the class has another constructor that calls the super. In that case
        // there must either be a new of the `declaredType`
        val newInstr = NEW(declaredType)
        val hasNew = callerMethod.body.get.exists {
            case (_, i) ⇒ i == newInstr
        }
        if (hasNew) {
            partialResults += partialResult(declaredType, caller);
        }
    }

    private[this] def continuation(
        declaredMethod: DeclaredMethod,
        declaredType:   ObjectType,
        seenCallers:    Set[DeclaredMethod]
    )(someEPS: SomeEPS): PropertyComputationResult = {
        val eps = someEPS.asInstanceOf[EPS[DeclaredMethod, Callers]]
        processCallers(declaredMethod, declaredType, eps, eps.ub, seenCallers)
    }

    private def partialResult(
        declaredType: ObjectType,
        caller:       DeclaredMethod
    ): PartialResult[SetEntity, InstantiatedTypes] = {

        // Subtypes of Throwable are tracked globally.
        val setEntity =
            if (classHierarchy.isSubtypeOf(declaredType, ObjectType.Throwable))
                project
            else
                setEntitySelector(caller)

        PartialResult[SetEntity, InstantiatedTypes](
            setEntity,
            InstantiatedTypes.key,
            InstantiatedTypesAnalysis.update(setEntity, UIDSet(declaredType))
        )
    }

    def getInstantiatedTypesUB(
        instantiatedTypesEOptP: EOptionP[SomeProject, InstantiatedTypes]
    ): UIDSet[ReferenceType] = {
        instantiatedTypesEOptP match {
            case eps: EPS[_, _] ⇒ eps.ub.types
            case _              ⇒ UIDSet.empty
        }
    }
}

object InstantiatedTypesAnalysis {
    // TODO Duplication: Something like this appears in several places.
    def update[E >: Null <: Entity](
        entity:               E,
        newInstantiatedTypes: UIDSet[ReferenceType]
    )(
        eop: EOptionP[E, InstantiatedTypes]
    ): Option[InterimEP[E, InstantiatedTypes]] = eop match {
        case InterimUBP(ub: InstantiatedTypes) ⇒
            val newUB = ub.updated(newInstantiatedTypes)
            if (newUB.types.size > ub.types.size)
                Some(InterimEUBP(entity, newUB))
            else
                None

        case _: EPK[_, _] ⇒
            val newUB = InstantiatedTypes.apply(newInstantiatedTypes)
            Some(InterimEUBP(entity, newUB))

        case r ⇒ throw new IllegalStateException(s"unexpected previous result $r")
    }

}

class InstantiatedTypesAnalysisScheduler(
        val setEntitySelector: SetEntitySelector
) extends FPCFTriggeredAnalysisScheduler {

    override type InitializationData = Null

    override def uses: Set[PropertyBounds] = PropertyBounds.ubs(
        InstantiatedTypes,
        Callers
    )

    override def triggeredBy: PropertyKey[Callers] = Callers.key

    override def derivesCollaboratively: Set[PropertyBounds] = PropertyBounds.ubs(InstantiatedTypes)

    override def derivesEagerly: Set[PropertyBounds] = Set.empty

    override def register(p: SomeProject, ps: PropertyStore, unused: Null): FPCFAnalysis = {
        val analysis = new InstantiatedTypesAnalysis(p, setEntitySelector)
        ps.registerTriggeredComputation(triggeredBy, analysis.analyze)
        analysis
    }

    override def init(p: SomeProject, ps: PropertyStore): Null = {
        // TODO AB Is this needed or can this be removed?
        //val initialInstantiatedTypes = UIDSet(p.get(InitialInstantiatedTypesKey).toSeq: _*)

        //ps.preInitialize[SomeProject, InstantiatedTypes](p, InstantiatedTypes.key) {
        //    case _: EPK[_, _] ⇒ InterimEUBP(p, org.opalj.br.fpcf.properties.cg.InstantiatedTypes(initialInstantiatedTypes))
        //    case eps          ⇒ throw new IllegalStateException(s"unexpected property: $eps")
        //}

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

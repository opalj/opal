/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package cg
package rta

import scala.language.existentials

import org.opalj.collection.immutable.UIDSet
import org.opalj.fpcf.EOptionP
import org.opalj.fpcf.EPK
import org.opalj.fpcf.EPS
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
import org.opalj.fpcf.SomeEPS
import org.opalj.br.DeclaredMethod
import org.opalj.br.ObjectType
import org.opalj.br.analyses.SomeProject
import org.opalj.br.analyses.cg.InitialInstantiatedTypesKey
import org.opalj.br.analyses.DeclaredMethods
import org.opalj.br.analyses.DeclaredMethodsKey
import org.opalj.br.fpcf.FPCFAnalysis
import org.opalj.br.fpcf.FPCFTriggeredAnalysisScheduler
import org.opalj.br.instructions.INVOKESPECIAL
import org.opalj.br.PCAndInstruction
import org.opalj.br.analyses.ProjectInformationKeys
import org.opalj.br.fpcf.properties.cg.Callers
import org.opalj.br.fpcf.properties.cg.InstantiatedTypes
import org.opalj.br.fpcf.properties.cg.NoCallers
import org.opalj.br.instructions.NEW

/**
 * Marks types as instantiated if their constructor is invoked. Constructors invoked by subclass
 * constructors do not result in additional instantiated types.
 *
 * @author Florian Kuebler
 */
class InstantiatedTypesAnalysis private[analyses] (
        final val project: SomeProject
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

        // the set of types that are definitely initialized at this point in time
        val instantiatedTypesEOptP = propertyStore(project, InstantiatedTypes.key)
        val instantiatedTypesUB: UIDSet[ObjectType] = getInstantiatedTypesUB(instantiatedTypesEOptP)

        val declaredType = declaredMethod.declaringClassType

        // if the current type is already instantiated, no work is left
        if (instantiatedTypesUB.contains(declaredType))
            return NoResult;

        val cfOpt = project.classFile(declaredType)

        // abstract classes can never be instantiated
        cfOpt.foreach { cf ⇒
            if (cf.isAbstract)
                return NoResult;
        }

        processCallers(declaredMethod, declaredType, callersEOptP, callersUB, Set.empty)
    }

    private[this] def processCallers(
        declaredMethod:   DeclaredMethod,
        declaredType:     ObjectType,
        callersEOptP:     EOptionP[DeclaredMethod, Callers],
        callersUB:        Callers,
        seenSuperCallers: Set[DeclaredMethod]
    ): PropertyComputationResult = {
        var newSeenSuperCallers = seenSuperCallers
        for {
            (caller, _, _) ← callersUB.callers
            // if we already analyzed the caller, we do not need to do it twice
            // note, that this is only needed for the continuation
            if !newSeenSuperCallers.contains(caller)
        } {
            if (caller.name != "<init>") {
                return partialResult(declaredType);
            }

            // the constructor is called from another constructor. it is only an new instantiated
            // type if it was no super call. Thus the caller must be a subtype
            if (!classHierarchy.isSubtypeOf(caller.declaringClassType, declaredType))
                return partialResult(declaredType);

            // actually it must be the direct subtype! -- we did the first check to return early
            project.classFile(caller.declaringClassType).foreach { cf ⇒
                cf.superclassType.foreach { supertype ⇒
                    if (supertype != declaredType)
                        return partialResult(declaredType);
                }
            }

            // if the caller is not available, we have to assume that it was no super call
            if (!caller.hasSingleDefinedMethod) {
                return partialResult(declaredType);
            }

            val callerMethod = caller.definedMethod

            // if the caller has no body, we have to assume that it was no super call
            if (callerMethod.body.isEmpty)
                return partialResult(declaredType);

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
            if (pcsOfSuperCalls.size > 1)
                return partialResult(declaredType);

            // there is exactly the current call as potential super call, it still might no super
            // call if the class has another constructor that calls the super. In that case
            // there must either be a new of the `declaredType`
            val newInstr = NEW(declaredType)
            val hasNew = callerMethod.body.get.exists {
                case (_, i) ⇒ i == newInstr
            }
            if (hasNew)
                return partialResult(declaredType);

            // to call is a super call, we should remember the call, in order to not evaluate it
            // again, if there are new callers!
            newSeenSuperCallers += caller
        }

        if (callersEOptP.isFinal) {
            NoResult
        } else {
            InterimPartialResult(
                Some(callersEOptP),
                continuation(declaredMethod, declaredType, newSeenSuperCallers)
            )
        }
    }

    private[this] def continuation(
        declaredMethod:   DeclaredMethod,
        declaredType:     ObjectType,
        seenSuperCallers: Set[DeclaredMethod]
    )(someEPS: SomeEPS): PropertyComputationResult = {
        val eps = someEPS.asInstanceOf[EPS[DeclaredMethod, Callers]]
        processCallers(declaredMethod, declaredType, eps, eps.ub, seenSuperCallers)

    }

    private[this] def partialResult(
        declaredType: ObjectType
    ): PartialResult[SomeProject, InstantiatedTypes] = {
        PartialResult[SomeProject, InstantiatedTypes](
            project,
            InstantiatedTypes.key,
            InstantiatedTypesAnalysis.update(project, UIDSet(declaredType))
        )
    }

    def getInstantiatedTypesUB(
        instantiatedTypesEOptP: EOptionP[SomeProject, InstantiatedTypes]
    ): UIDSet[ObjectType] = {
        instantiatedTypesEOptP match {
            case eps: EPS[_, _] ⇒ eps.ub.types
            case _              ⇒ UIDSet.empty
        }
    }
}

object InstantiatedTypesAnalysis {
    def update(
        p:                    SomeProject,
        newInstantiatedTypes: UIDSet[ObjectType]
    )(
        eop: EOptionP[SomeProject, InstantiatedTypes]
    ): Option[InterimEP[SomeProject, InstantiatedTypes]] = eop match {
        case InterimUBP(ub: InstantiatedTypes) ⇒
            val newUB = ub.updated(newInstantiatedTypes)
            if (newUB.types.size > ub.types.size)
                Some(InterimEUBP(p, newUB))
            else
                None

        case _: EPK[_, _] ⇒
            throw new IllegalStateException(
                "the instantiated types property should be pre initialized"
            )

        case r ⇒ throw new IllegalStateException(s"unexpected previous result $r")
    }
}

object InstantiatedTypesAnalysisScheduler extends FPCFTriggeredAnalysisScheduler {

    override type InitializationData = Null

    override def requiredProjectInformation: ProjectInformationKeys = Seq(DeclaredMethodsKey)

    override def uses: Set[PropertyBounds] = PropertyBounds.ubs(
        InstantiatedTypes,
        Callers
    )

    override def triggeredBy: PropertyKey[Callers] = Callers.key

    override def derivesCollaboratively: Set[PropertyBounds] = PropertyBounds.ubs(InstantiatedTypes)

    override def derivesEagerly: Set[PropertyBounds] = Set.empty

    override def register(p: SomeProject, ps: PropertyStore, unused: Null): FPCFAnalysis = {
        val analysis = new InstantiatedTypesAnalysis(p)
        ps.registerTriggeredComputation(triggeredBy, analysis.analyze)
        analysis
    }

    override def init(p: SomeProject, ps: PropertyStore): Null = {
        val initialInstantiatedTypes = UIDSet(p.get(InitialInstantiatedTypesKey).toSeq: _*)

        ps.preInitialize[SomeProject, InstantiatedTypes](p, InstantiatedTypes.key) {
            case _: EPK[_, _] ⇒ InterimEUBP(p, org.opalj.br.fpcf.properties.cg.InstantiatedTypes(initialInstantiatedTypes))
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

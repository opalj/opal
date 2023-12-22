/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package cg
package rta

import scala.language.existentials

import org.opalj.br.DeclaredMethod
import org.opalj.br.ObjectType
import org.opalj.br.ReferenceType
import org.opalj.br.analyses.ProjectInformationKeys
import org.opalj.br.analyses.SomeProject
import org.opalj.br.analyses.cg.InitialInstantiatedTypesKey
import org.opalj.br.fpcf.BasicFPCFTriggeredAnalysisScheduler
import org.opalj.br.fpcf.ContextProviderKey
import org.opalj.br.fpcf.FPCFAnalysis
import org.opalj.br.fpcf.analyses.ContextProvider
import org.opalj.br.fpcf.properties.cg.Callers
import org.opalj.br.fpcf.properties.cg.InstantiatedTypes
import org.opalj.br.fpcf.properties.cg.NoCallers
import org.opalj.br.instructions.NEW
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

/**
 * Marks types as instantiated if their constructor is invoked. Constructors invoked by subclass
 * constructors do not result in additional instantiated types.
 * The analysis does not just looks for "new" instructions, in order to support reflection.
 *
 * @author Florian Kuebler
 * @author Dominik Helm
 */
class InstantiatedTypesAnalysis private[analyses] (
        final val project: SomeProject
) extends FPCFAnalysis {

    private[this] implicit val contextProvider: ContextProvider = project.get(ContextProviderKey)

    def analyze(declaredMethod: DeclaredMethod): PropertyComputationResult = {
        val callersEOptP = propertyStore(declaredMethod, Callers.key)

        val callersUB: Callers = (callersEOptP: @unchecked) match {
            case FinalP(NoCallers) =>
                // nothing to do, since there is no caller
                return NoResult;

            case eps: EPS[_, _] =>
                if (eps.ub eq NoCallers) {
                    // we can not create a dependency here, so the analysis is not allowed to create
                    // such a result
                    throw new IllegalStateException("illegal immediate result for callers")
                } else {
                    eps.ub
                }
            // the method is reachable, so we analyze it!
        }

        val declaredType = declaredMethod.declaringClassType
        val instantiatedTypes = getLoadConstantTypes(declaredMethod)

        val cfOpt = project.classFile(declaredType)

        // only constructors may initialize a class; abstract classes can never be instantiated
        if (declaredMethod.name != "<init>" || cfOpt.isDefined && cfOpt.get.isAbstract) {
            if (instantiatedTypes.isEmpty)
                return NoResult;
            else
                return partialResult(instantiatedTypes)
        }

        // the set of types that are definitely initialized at this point in time
        val instantiatedTypesEOptP = propertyStore(project, InstantiatedTypes.key)
        val instantiatedTypesUB: UIDSet[ReferenceType] = getInstantiatedTypesUB(instantiatedTypesEOptP)

        val newInstantiatedTypes = instantiatedTypes.diff(instantiatedTypesUB)

        // if the current type and all constants' types are already instantiated, no work is left
        if (instantiatedTypesUB.contains(declaredType) && newInstantiatedTypes.isEmpty)
            return NoResult;

        processCallers(declaredMethod, declaredType, newInstantiatedTypes, callersEOptP, callersUB, null)
    }

    private[this] def processCallers(
        declaredMethod:    DeclaredMethod,
        declaredType:      ObjectType,
        instantiatedTypes: UIDSet[ReferenceType],
        callersEOptP:      EOptionP[DeclaredMethod, Callers],
        callersUB:         Callers,
        seenCallers:       Callers
    ): PropertyComputationResult = {
        callersUB.forNewCallerContexts(seenCallers, callersEOptP.e) {
            (_, callerContext, _, isDirect) =>
                // unknown or VM level calls always have to be treated as instantiations
                if (!callerContext.hasContext) {
                    return partialResult(instantiatedTypes + declaredType);
                }

                // indirect calls, e.g. via reflection, are to be treated as instantiations as well
                if (!isDirect) {
                    return partialResult(instantiatedTypes + declaredType);
                }

                val caller = callerContext.method

                // a constructor is called by a non-constructor method, there will be an initialization.
                if (caller.name != "<init>") {
                    return partialResult(instantiatedTypes + declaredType);
                }

                // if the caller is not available, we have to assume that it was no super call
                if (!caller.hasSingleDefinedMethod) {
                    return partialResult(instantiatedTypes + declaredType);
                }

                // the constructor is called from another constructor. it is only an new instantiated
                // type if it was no super call. Thus the caller must be a direct subtype
                project.classFile(caller.declaringClassType).foreach { cf =>
                    cf.superclassType.foreach { supertype =>
                        if (supertype != declaredType)
                            return partialResult(instantiatedTypes + declaredType);
                    }
                }

                val body = caller.definedMethod.body.get

                // there must either be a new of the `declaredType` or it is a super call.
                // check if there is an explicit NEW that instantiates the type
                val newInstr = NEW(declaredType)
                val hasNew = body.exists(pcInst => pcInst.instruction == newInstr)
                if (hasNew)
                    return partialResult(instantiatedTypes + declaredType);
        }

        if (callersEOptP.isFinal) {
            if (instantiatedTypes.isEmpty)
                NoResult
            else
                partialResult(instantiatedTypes)
        } else {
            InterimPartialResult(
                Set(callersEOptP),
                continuation(declaredMethod, declaredType, instantiatedTypes, callersUB)
            )
        }
    }

    private[this] def continuation(
        declaredMethod:    DeclaredMethod,
        declaredType:      ObjectType,
        instantiatedTypes: UIDSet[ReferenceType],
        seenCallers:       Callers
    )(someEPS: SomeEPS): PropertyComputationResult = {
        val eps = someEPS.asInstanceOf[EPS[DeclaredMethod, Callers]]
        processCallers(declaredMethod, declaredType, instantiatedTypes, eps, eps.ub, seenCallers)
    }

    private[this] def partialResult(
        instantiatedTypes: UIDSet[ReferenceType]
    ): PartialResult[SomeProject, InstantiatedTypes] = {
        PartialResult[SomeProject, InstantiatedTypes](
            project,
            InstantiatedTypes.key,
            InstantiatedTypesAnalysis.update(project, instantiatedTypes)
        )
    }

    def getInstantiatedTypesUB(
        instantiatedTypesEOptP: EOptionP[SomeProject, InstantiatedTypes]
    ): UIDSet[ReferenceType] = {
        instantiatedTypesEOptP match {
            case eps: EPS[_, _] => eps.ub.types
            case _              => UIDSet.empty
        }
    }
}

object InstantiatedTypesAnalysis {
    def update(
        p:                    SomeProject,
        newInstantiatedTypes: UIDSet[ReferenceType]
    )(
        eop: EOptionP[SomeProject, InstantiatedTypes]
    ): Option[InterimEP[SomeProject, InstantiatedTypes]] = eop match {
        case InterimUBP(ub: InstantiatedTypes) =>
            val newUB = ub.updated(newInstantiatedTypes)
            if (newUB.types.size > ub.types.size)
                Some(InterimEUBP(p, newUB))
            else
                None

        case _: EPK[_, _] =>
            throw new IllegalStateException(
                "the instantiated types property should be pre initialized"
            )

        case r => throw new IllegalStateException(s"unexpected previous result $r")
    }
}

object InstantiatedTypesAnalysisScheduler extends BasicFPCFTriggeredAnalysisScheduler {

    override def requiredProjectInformation: ProjectInformationKeys = Seq(ContextProviderKey)

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
        val initialInstantiatedTypes = UIDSet[ReferenceType](p.get(InitialInstantiatedTypesKey).toSeq: _*)

        ps.preInitialize[SomeProject, InstantiatedTypes](p, InstantiatedTypes.key) {
            case _: EPK[_, _] => InterimEUBP(p, InstantiatedTypes(initialInstantiatedTypes))
            case eps          => throw new IllegalStateException(s"unexpected property: $eps")
        }

        null
    }
}

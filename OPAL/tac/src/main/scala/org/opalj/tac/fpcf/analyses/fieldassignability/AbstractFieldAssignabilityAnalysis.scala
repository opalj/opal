/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package fieldassignability

import org.opalj.br.DeclaredField
import org.opalj.br.DefinedMethod
import org.opalj.br.Field
import org.opalj.br.Method
import org.opalj.br.PC
import org.opalj.br.analyses.DeclaredFields
import org.opalj.br.analyses.DeclaredFieldsKey
import org.opalj.br.analyses.DeclaredMethods
import org.opalj.br.analyses.DeclaredMethodsKey
import org.opalj.br.analyses.ProjectInformationKeys
import org.opalj.br.analyses.SomeProject
import org.opalj.br.analyses.cg.ClosedPackagesKey
import org.opalj.br.analyses.cg.TypeExtensibilityKey
import org.opalj.br.fpcf.BasicFPCFEagerAnalysisScheduler
import org.opalj.br.fpcf.BasicFPCFLazyAnalysisScheduler
import org.opalj.br.fpcf.ContextProviderKey
import org.opalj.br.fpcf.FPCFAnalysis
import org.opalj.br.fpcf.FPCFAnalysisScheduler
import org.opalj.br.fpcf.analyses.ContextProvider
import org.opalj.br.fpcf.properties.Context
import org.opalj.br.fpcf.properties.fieldaccess.AccessReceiver
import org.opalj.br.fpcf.properties.fieldaccess.FieldReadAccessInformation
import org.opalj.br.fpcf.properties.fieldaccess.FieldWriteAccessInformation
import org.opalj.br.fpcf.properties.immutability.Assignable
import org.opalj.br.fpcf.properties.immutability.EffectivelyNonAssignable
import org.opalj.br.fpcf.properties.immutability.FieldAssignability
import org.opalj.br.fpcf.properties.immutability.NonAssignable
import org.opalj.fpcf.Entity
import org.opalj.fpcf.EOptionP
import org.opalj.fpcf.InterimResult
import org.opalj.fpcf.ProperPropertyComputationResult
import org.opalj.fpcf.PropertyBounds
import org.opalj.fpcf.PropertyStore
import org.opalj.fpcf.Result
import org.opalj.fpcf.SomeEOptionP
import org.opalj.fpcf.SomeEPS
import org.opalj.fpcf.UBP
import org.opalj.tac.common.DefinitionSitesKey
import org.opalj.tac.fpcf.analyses.cg.uVarForDefSites
import org.opalj.tac.fpcf.properties.TACAI

/**
 * TODO document
 *
 * @note This analysis is only ''soundy'' if the project does not contain native methods.
 *
 * @author Maximilian Rüsch
 * @author Dominik Helm
 */
trait AbstractFieldAssignabilityAnalysis extends FPCFAnalysis {

    trait AbstractFieldAssignabilityAnalysisState {

        val field: Field
        var fieldAssignability: FieldAssignability = NonAssignable

        var fieldWriteAccessDependee: Option[EOptionP[DeclaredField, FieldWriteAccessInformation]] = None
        var initializerWrites: Map[Context, Set[(PC, AccessReceiver)]] = Map.empty.withDefaultValue(Set.empty)
        var nonInitializerWrites: Map[Context, Set[(PC, AccessReceiver)]] = Map.empty.withDefaultValue(Set.empty)

        var fieldReadAccessDependee: Option[EOptionP[DeclaredField, FieldReadAccessInformation]] = None
        var initializerReads: Map[Context, Set[(PC, AccessReceiver)]] = Map.empty.withDefaultValue(Set.empty)
        var nonInitializerReads: Map[Context, Set[(PC, AccessReceiver)]] = Map.empty.withDefaultValue(Set.empty)

        var tacDependees: Map[DefinedMethod, EOptionP[Method, TACAI]] = Map.empty

        def hasDependees: Boolean = {
            fieldWriteAccessDependee.exists(_.isRefinable) ||
            fieldReadAccessDependee.exists(_.isRefinable) ||
            tacDependees.valuesIterator.exists(_.isRefinable)
        }

        def dependees: Set[SomeEOptionP] = {
            tacDependees.valuesIterator.filter(_.isRefinable).toSet ++
                fieldWriteAccessDependee.filter(_.isRefinable) ++
                fieldReadAccessDependee.filter(_.isRefinable)
        }
    }

    final val typeExtensibility = project.get(TypeExtensibilityKey)
    final val closedPackages = project.get(ClosedPackagesKey)
    final val definitionSites = project.get(DefinitionSitesKey)
    final val declaredFields: DeclaredFields = project.get(DeclaredFieldsKey)
    implicit final val declaredMethods: DeclaredMethods = project.get(DeclaredMethodsKey)
    implicit final val contextProvider: ContextProvider = project.get(ContextProviderKey)

    def doDetermineFieldAssignability(entity: Entity): ProperPropertyComputationResult = {
        entity match {
            case field: Field =>
                determineFieldAssignability(field)
            case _ =>
                val m = s"${entity.getClass.getSimpleName} is not a ${Field.getClass.getSimpleName}"
                throw new IllegalArgumentException(m)
        }
    }

    type AnalysisState <: AbstractFieldAssignabilityAnalysisState

    def createState(field: Field): AnalysisState

    private[analyses] def determineFieldAssignability(field: Field): ProperPropertyComputationResult = {
        val thisType = field.classFile.thisType
        if (field.isNotFinal) {
            if (field.isPublic) {
                return Result(field, Assignable);
            } else if (field.isProtected) {
                if (typeExtensibility(thisType).isYesOrUnknown || !closedPackages(thisType.packageName)) {
                    return Result(field, Assignable);
                }
            } else if (field.isPackagePrivate) {
                if (!closedPackages(thisType.packageName)) {
                    return Result(field, Assignable);
                }
            }
        }

        val state: AnalysisState = createState(field)
        state.fieldAssignability = if (field.isFinal) NonAssignable else EffectivelyNonAssignable

        handleWriteAccessInformation(propertyStore(declaredFields(field), FieldWriteAccessInformation.key))(using state)
    }

    def analyzeInitializerRead(
        context:  Context,
        tac:      TACode[TACMethodParameter, V],
        readPC:   PC,
        receiver: Option[V]
    )(implicit state: AnalysisState): FieldAssignability

    def analyzeNonInitializerRead(
        context:  Context,
        tac:      TACode[TACMethodParameter, V],
        readPC:   PC,
        receiver: Option[V]
    )(implicit state: AnalysisState): FieldAssignability

    def analyzeInitializerWrite(
        context:  Context,
        tac:      TACode[TACMethodParameter, V],
        writePC:  PC,
        receiver: Option[V]
    )(implicit state: AnalysisState): FieldAssignability

    def analyzeNonInitializerWrite(
        context:  Context,
        tac:      TACode[TACMethodParameter, V],
        writePC:  PC,
        receiver: Option[V]
    )(implicit state: AnalysisState): FieldAssignability

    def createResult()(implicit state: AnalysisState): ProperPropertyComputationResult = {
        if (state.hasDependees && (state.fieldAssignability ne Assignable))
            InterimResult(state.field, lb = Assignable, ub = state.fieldAssignability, state.dependees, continuation)
        else
            Result(state.field, state.fieldAssignability)
    }

    def continuation(eps: SomeEPS)(implicit state: AnalysisState): ProperPropertyComputationResult = {
        eps.pk match {
            case FieldWriteAccessInformation.key =>
                handleWriteAccessInformation(eps.asInstanceOf[EOptionP[DeclaredField, FieldWriteAccessInformation]])

            case FieldReadAccessInformation.key =>
                handleReadAccessInformation(eps.asInstanceOf[EOptionP[DeclaredField, FieldReadAccessInformation]])

            case TACAI.key =>
                val newEP = eps.asInstanceOf[EOptionP[Method, TACAI]]
                val method = declaredMethods(newEP.e)
                if (state.tacDependees(method).hasUBP)
                    throw IllegalStateException("True updates to the TAC (UB -> new UB) are not supported yet!")
                state.tacDependees += method -> newEP
                val tac = newEP.ub.tac.get

                def refreshAssignability(
                    accesses:    Map[Context, Set[(PC, AccessReceiver)]],
                    analyzeFunc: (Context, TACode[TACMethodParameter, V], PC, Option[V]) => FieldAssignability
                ): Unit = {
                    if (state.fieldAssignability != Assignable) {
                        accesses.iterator.filter(_._1.method eq method).foreach { case (context, accessesInContext) =>
                            accessesInContext.foreach { case (pc, receiver) =>
                                val receiverVar = receiver.map(uVarForDefSites(_, tac.pcToIndex))
                                if (state.fieldAssignability != Assignable)
                                    state.fieldAssignability = state.fieldAssignability.meet {
                                        analyzeFunc(context, tac, pc, receiverVar)
                                    }
                            }
                        }
                    }
                }

                refreshAssignability(state.initializerReads, analyzeInitializerRead)
                refreshAssignability(state.nonInitializerReads, analyzeNonInitializerRead)
                refreshAssignability(state.initializerWrites, analyzeInitializerWrite)
                refreshAssignability(state.nonInitializerWrites, analyzeNonInitializerWrite)

                createResult()
        }
    }

    private def handleWriteAccessInformation(
        newEP: EOptionP[DeclaredField, FieldWriteAccessInformation]
    )(implicit state: AnalysisState): ProperPropertyComputationResult = {
        if (newEP.hasUBP) {
            val (seenDirect, seenIndirect) = state.fieldWriteAccessDependee match {
                case Some(UBP(fai: FieldWriteAccessInformation)) => (fai.numDirectAccesses, fai.numIndirectAccesses)
                case _                                           => (0, 0)
            }
            val (newDirect, newIndirect) = (newEP.ub.numDirectAccesses, newEP.ub.numIndirectAccesses)
            if ((seenDirect + seenIndirect) == 0 && (newDirect + newIndirect) > 0) {
                // We crossed from no writes to at least one write, so we potentially have to ensure a safe interaction
                // with reads of the same field. Thus, they need to be analyzed.
                handleReadAccessInformation(propertyStore(declaredFields(state.field), FieldReadAccessInformation.key))
            }
            state.fieldWriteAccessDependee = Some(newEP)

            // Register all field accesses in the state first to enable cross access comparisons
            newEP.ub.getNewestAccesses(
                newDirect - seenDirect,
                newIndirect - seenIndirect
            ) foreach { case (contextID, pc, receiver, _) =>
                val context = contextProvider.contextFromId(contextID)
                val method = context.method.asDefinedMethod
                if (method.definedMethod.isInitializer) {
                    state.initializerWrites = state.initializerWrites.updatedWith(context) {
                        case None           => Some(Set((pc, receiver)))
                        case Some(accesses) => Some(accesses.+((pc, receiver)))
                    }
                } else {
                    state.nonInitializerWrites = state.nonInitializerWrites.updatedWith(context) {
                        case None           => Some(Set((pc, receiver)))
                        case Some(accesses) => Some(accesses.+((pc, receiver)))
                    }
                }

                val tacEP = state.tacDependees.get(method) match {
                    case Some(tacEP) => tacEP
                    case None        =>
                        val tacEP = propertyStore(method.definedMethod, TACAI.key)
                        state.tacDependees += method -> tacEP
                        tacEP
                }
                if (tacEP.hasUBP && state.fieldAssignability != Assignable) {
                    val tac = tacEP.ub.tac.get
                    val receiverVar = receiver.map(uVarForDefSites(_, tac.pcToIndex))
                    state.fieldAssignability = state.fieldAssignability.meet {
                        if (method.definedMethod.isInitializer)
                            analyzeInitializerWrite(context, tac, pc, receiverVar)
                        else
                            analyzeNonInitializerWrite(context, tac, pc, receiverVar)
                    }
                }
            }
        } else {
            state.fieldWriteAccessDependee = Some(newEP)
        }

        createResult()
    }

    private def handleReadAccessInformation(
        newEP: EOptionP[DeclaredField, FieldReadAccessInformation]
    )(implicit state: AnalysisState): ProperPropertyComputationResult = {
        if (newEP.hasUBP) {
            val (seenDirect, seenIndirect) = state.fieldReadAccessDependee match {
                case Some(UBP(fai: FieldReadAccessInformation)) => (fai.numDirectAccesses, fai.numIndirectAccesses)
                case _                                          => (0, 0)
            }
            val (newDirect, newIndirect) = (newEP.ub.numDirectAccesses, newEP.ub.numIndirectAccesses)
            state.fieldReadAccessDependee = Some(newEP)

            // Register all field accesses in the state first to enable cross access comparisons
            newEP.ub.getNewestAccesses(
                newDirect - seenDirect,
                newIndirect - seenIndirect
            ) foreach { case (contextID, pc, receiver, _) =>
                val context = contextProvider.contextFromId(contextID)
                val method = context.method.asDefinedMethod
                if (method.definedMethod.isInitializer) {
                    state.initializerReads = state.initializerReads.updatedWith(context) {
                        case None           => Some(Set((pc, receiver)))
                        case Some(accesses) => Some(accesses.+((pc, receiver)))
                    }
                } else {
                    state.nonInitializerReads = state.nonInitializerReads.updatedWith(context) {
                        case None           => Some(Set((pc, receiver)))
                        case Some(accesses) => Some(accesses.+((pc, receiver)))
                    }
                }

                val tacEP = state.tacDependees.get(method) match {
                    case Some(tacEP) => tacEP
                    case None        =>
                        val tacEP = propertyStore(method.definedMethod, TACAI.key)
                        state.tacDependees += method -> tacEP
                        tacEP
                }
                if (tacEP.hasUBP && state.fieldAssignability != Assignable) {
                    val tac = tacEP.ub.tac.get
                    val receiverVar = receiver.map(uVarForDefSites(_, tac.pcToIndex))
                    state.fieldAssignability = state.fieldAssignability.meet {
                        if (method.definedMethod.isInitializer)
                            analyzeInitializerRead(context, tac, pc, receiverVar)
                        else
                            analyzeNonInitializerRead(context, tac, pc, receiverVar)
                    }
                }
            }
        } else {
            state.fieldReadAccessDependee = Some(newEP)
        }

        createResult()
    }

    /**
     * Determines whether the basic block of a given index dominates the basic block of the other index or is executed
     * before the other index in the case of both indexes belonging to the same basic block.
     */
    protected def dominates(
        potentiallyDominatorIndex: Int,
        potentiallyDominatedIndex: Int,
        taCode:                    TACode[TACMethodParameter, V]
    ): Boolean = {
        val bbPotentiallyDominator = taCode.cfg.bb(potentiallyDominatorIndex)
        val bbPotentiallyDominated = taCode.cfg.bb(potentiallyDominatedIndex)
        taCode.cfg.dominatorTree
            .strictlyDominates(bbPotentiallyDominator.nodeId, bbPotentiallyDominated.nodeId) ||
            bbPotentiallyDominator == bbPotentiallyDominated && potentiallyDominatorIndex < potentiallyDominatedIndex
    }

    /**
     * Provided with two PCs, determines whether there exists a path from the first PC to the second PC in the context
     * of the provided TAC and attached CFG. This check is not reflexive.
     *
     * IMPROVE abort on path found instead of computing all reachable BBs
     */
    protected def pathExists(fromPC: Int, toPC: Int, tac: TACode[TACMethodParameter, V]): Boolean = {
        val firstBB = tac.cfg.bb(tac.pcToIndex(fromPC))
        val secondBB = tac.cfg.bb(tac.pcToIndex(toPC))

        if (firstBB == secondBB) fromPC < toPC
        else firstBB.reachable().contains(secondBB)
    }
}

sealed trait AbstractFieldAssignabilityAnalysisScheduler extends FPCFAnalysisScheduler {

    override def uses: Set[PropertyBounds] = PropertyBounds.ubs(
        TACAI,
        FieldWriteAccessInformation,
        FieldReadAccessInformation
    )

    override def requiredProjectInformation: ProjectInformationKeys = Seq(
        DeclaredMethodsKey,
        ClosedPackagesKey,
        TypeExtensibilityKey,
        DefinitionSitesKey,
        ContextProviderKey,
        DeclaredFieldsKey
    )

    final def derivedProperty: PropertyBounds = PropertyBounds.ub(FieldAssignability)
}

/**
 * Executor for the eager field assignability analysis.
 */
trait AbstractEagerFieldAssignabilityAnalysisScheduler
    extends AbstractFieldAssignabilityAnalysisScheduler
    with BasicFPCFEagerAnalysisScheduler {

    override def derivesEagerly: Set[PropertyBounds] = Set(derivedProperty)

    override def derivesCollaboratively: Set[PropertyBounds] = Set.empty

    override def start(p: SomeProject, ps: PropertyStore, unused: Null): FPCFAnalysis = {
        val analysis = newAnalysis(p)
        ps.scheduleEagerComputationsForEntities(p.allFields)(analysis.determineFieldAssignability)
        analysis
    }

    def newAnalysis(p: SomeProject): AbstractFieldAssignabilityAnalysis
}

/**
 * Executor for the lazy field assignability analysis.
 */
trait AbstractLazyFieldAssignabilityAnalysisScheduler
    extends AbstractFieldAssignabilityAnalysisScheduler
    with BasicFPCFLazyAnalysisScheduler {

    override def derivesLazily: Some[PropertyBounds] = Some(derivedProperty)

    override def register(p: SomeProject, ps: PropertyStore, unused: Null): FPCFAnalysis = {
        val analysis = newAnalysis(p)
        ps.registerLazyPropertyComputation(FieldAssignability.key, analysis.doDetermineFieldAssignability)
        analysis
    }

    def newAnalysis(p: SomeProject): AbstractFieldAssignabilityAnalysis
}

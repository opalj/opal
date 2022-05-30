/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package cg
package xta

import scala.jdk.CollectionConverters._

import org.opalj.br.Code
import org.opalj.br.DeclaredMethod
import org.opalj.br.Field
import org.opalj.br.Method
import org.opalj.br.ObjectType
import org.opalj.br.ReferenceType
import org.opalj.br.analyses.ProjectInformationKeys
import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.BasicFPCFTriggeredAnalysisScheduler
import org.opalj.br.fpcf.FPCFAnalysis
import org.opalj.tac.fpcf.properties.cg.Callees
import org.opalj.tac.fpcf.properties.cg.Callers
import org.opalj.tac.fpcf.properties.cg.InstantiatedTypes
import org.opalj.br.instructions.CHECKCAST
import org.opalj.collection.immutable.UIDSet
import org.opalj.fpcf.EPS
import org.opalj.fpcf.EUBP
import org.opalj.fpcf.Entity
import org.opalj.fpcf.InterimPartialResult
import org.opalj.fpcf.PartialResult
import org.opalj.fpcf.ProperPropertyComputationResult
import org.opalj.fpcf.PropertyBounds
import org.opalj.fpcf.PropertyKind
import org.opalj.fpcf.PropertyStore
import org.opalj.fpcf.Results
import org.opalj.fpcf.SomeEPS
import org.opalj.fpcf.SomePartialResult
import org.opalj.br.DefinedMethod
import org.opalj.tac.cg.TypeProviderKey
import org.opalj.tac.fpcf.properties.TACAI

import scala.collection.mutable.ArrayBuffer

/**
 * This analysis handles the type propagation of XTA, MTA, FTA and CTA call graph
 * algorithms.
 *
 * @param project         Project under analysis
 * @param selectTypeSetEntity Function which, for each entity, selects which entity its type set is attached to.
 * @author Andreas Bauer
 */
final class TypePropagationAnalysis private[analyses] (
        val project:         SomeProject,
        selectTypeSetEntity: TypeSetEntitySelector
) extends ReachableMethodAnalysis {

    private[this] val debug = false
    private[this] val _trace: TypePropagationTrace = new TypePropagationTrace()

    private type State = TypePropagationState[ContextType]

    override def processMethod(
        callContext: ContextType,
        tacEP:       EPS[Method, TACAI]
    ): ProperPropertyComputationResult = {

        val definedMethod = callContext.method.asDefinedMethod

        val typeSetEntity = selectTypeSetEntity(definedMethod)
        val instantiatedTypesEOptP = propertyStore(typeSetEntity, InstantiatedTypes.key)
        val calleesEOptP = propertyStore(definedMethod, Callees.key)

        if (debug) _trace.traceInit(definedMethod)

        implicit val state: TypePropagationState[ContextType] = new TypePropagationState(
            callContext, typeSetEntity, tacEP, instantiatedTypesEOptP, calleesEOptP
        )
        implicit val partialResults: ArrayBuffer[SomePartialResult] = ArrayBuffer.empty[SomePartialResult]

        if (calleesEOptP.hasUBP)
            processCallees(calleesEOptP.ub)
        processTACStatements
        processArrayTypes(state.ownInstantiatedTypes)

        returnResults(partialResults.iterator)
    }

    /**
     * Processes the method upon initialization. Finds field/array accesses and wires up dependencies accordingly.
     */
    private def processTACStatements(implicit state: State, partialResults: ArrayBuffer[SomePartialResult]): Unit = {
        val bytecode = state.callContext.method.definedMethod.body.get
        val tac = state.tac
        tac.stmts.foreach {
            case stmt @ Assignment(_, _, expr) if expr.isFieldRead =>
                val fieldRead = expr.asFieldRead
                if (fieldRead.declaredFieldType.isReferenceType) {
                    // Internally, generic fields have type "Object" due to type erasure. In many cases
                    // (but not all!), the Java compiler will place the "actual" return type within a checkcast
                    // instruction right after the field read instruction.
                    val nextInstruction = bytecode.instructions(bytecode.pcOfNextInstruction(stmt.pc))
                    val mostPreciseFieldType =
                        if (nextInstruction.isCheckcast)
                            nextInstruction.asInstanceOf[CHECKCAST].referenceType
                        else
                            fieldRead.declaredFieldType.asReferenceType

                    fieldRead.resolveField match {
                        case Some(f: Field) if project.isProjectType(f.classFile.thisType) =>
                            registerEntityForBackwardPropagation(f, mostPreciseFieldType)
                        case _ =>
                            val ef = ExternalField(fieldRead.declaringClass, fieldRead.name, fieldRead.declaredFieldType)
                            registerEntityForBackwardPropagation(ef, mostPreciseFieldType)
                    }
                }

            case fieldWrite: FieldWriteAccessStmt[_] =>
                if (fieldWrite.declaredFieldType.isReferenceType) {
                    fieldWrite.resolveField match {
                        case Some(f: Field) if project.isProjectType(f.classFile.thisType) =>
                            registerEntityForForwardPropagation(f, UIDSet(f.fieldType.asReferenceType))
                        case _ =>
                            val ef = ExternalField(fieldWrite.declaringClass, fieldWrite.name, fieldWrite.declaredFieldType)
                            registerEntityForForwardPropagation(ef, UIDSet(ef.declaredFieldType.asReferenceType))
                    }
                }

            case Assignment(_, _, expr) if expr.astID == ArrayLoad.ASTID =>
                state.methodReadsArrays = true

            case stmt: Stmt[_] if stmt.astID == ArrayStore.ASTID =>
                state.methodWritesArrays = true

            case _ =>
        }
    }

    private def c(state: State)(eps: SomeEPS): ProperPropertyComputationResult = eps match {

        case EUBP(e: DefinedMethod, _: Callees) =>
            if (debug) {
                assert(e == state.callContext.method)
                _trace.traceCalleesUpdate(e)
            }
            handleUpdateOfCallees(eps.asInstanceOf[EPS[DeclaredMethod, Callees]])(state)

        case EUBP(e: TypeSetEntity, t: InstantiatedTypes) if e == state.typeSetEntity =>
            if (debug) _trace.traceTypeUpdate(state.callContext.method, e, t.types)
            handleUpdateOfOwnTypeSet(eps.asInstanceOf[EPS[TypeSetEntity, InstantiatedTypes]])(state)

        case EUBP(e: TypeSetEntity, t: InstantiatedTypes) =>
            if (debug) _trace.traceTypeUpdate(state.callContext.method, e, t.types)
            handleUpdateOfBackwardPropagationTypeSet(eps.asInstanceOf[EPS[TypeSetEntity, InstantiatedTypes]])(state)

        case _ =>
            sys.error("received unexpected update")
    }

    private def handleUpdateOfCallees(
        eps: EPS[DeclaredMethod, Callees]
    )(
        implicit
        state: State
    ): ProperPropertyComputationResult = {
        state.updateCalleeDependee(eps)
        implicit val partialResults: ArrayBuffer[SomePartialResult] = ArrayBuffer.empty[SomePartialResult]
        processCallees(eps.ub)
        returnResults(partialResults.iterator)
    }

    private def handleUpdateOfOwnTypeSet(
        eps: EPS[TypeSetEntity, InstantiatedTypes]
    )(
        implicit
        state: State
    ): ProperPropertyComputationResult = {
        val previouslySeenTypes = state.ownInstantiatedTypes.size
        state.updateOwnInstantiatedTypesDependee(eps)
        val unseenTypes = UIDSet(eps.ub.dropOldest(previouslySeenTypes).toSeq: _*)

        implicit val partialResults: ArrayBuffer[SomePartialResult] = ArrayBuffer.empty[SomePartialResult]
        for (fpe <- state.forwardPropagationEntities.iterator().asScala) {
            val filters = state.forwardPropagationFilters(fpe)
            val propagation = propagateTypes(fpe, unseenTypes, filters)
            if (propagation.isDefined)
                partialResults += propagation.get
        }

        processArrayTypes(unseenTypes)

        returnResults(partialResults.iterator)
    }

    private def handleUpdateOfBackwardPropagationTypeSet(
        eps: EPS[TypeSetEntity, InstantiatedTypes]
    )(
        implicit
        state: State
    ): ProperPropertyComputationResult = {
        val typeSetEntity = eps.e
        val previouslySeenTypes = state.seenTypes(typeSetEntity)
        state.updateBackwardPropagationDependee(eps)
        val unseenTypes = UIDSet(eps.ub.dropOldest(previouslySeenTypes).toSeq: _*)

        val filters = state.backwardPropagationFilters(typeSetEntity)
        val propagationResult = propagateTypes(state.typeSetEntity, unseenTypes, filters)

        returnResults(propagationResult)
    }

    private def processArrayTypes(
        unseenTypes: UIDSet[ReferenceType]
    )(
        implicit
        state:          State,
        partialResults: ArrayBuffer[SomePartialResult]
    ): Unit = {
        for (t <- unseenTypes if t.isArrayType; at = t.asArrayType if at.elementType.isReferenceType) {
            if (state.methodWritesArrays) {
                registerEntityForForwardPropagation(at, UIDSet(at.componentType.asReferenceType))
            }
            if (state.methodReadsArrays) {
                registerEntityForBackwardPropagation(at, at.componentType.asReferenceType)
            }
        }
    }

    private def isIgnoredCallee(callee: DeclaredMethod): Boolean = {
        // Special case: Object.<init> is implicitly called as a super call by any method X.<init> when X does
        // not have a supertype.
        // The "this" type X will flow to the type set of Object.<init>. Since Object.<init> is usually
        // part of the external world, the external world type set is then polluted with any type which
        // was constructed anywhere in the program.
        callee.declaringClassType == ObjectType.Object && callee.name == "<init>"
    }

    private def processCallees(
        callees: Callees
    )(
        implicit
        state:          State,
        partialResults: ArrayBuffer[SomePartialResult]
    ): Unit = {
        val bytecode = state.callContext.method.definedMethod.body.get
        for {
            pc <- callees.callSitePCs(state.callContext)
            calleeContext <- callees.callees(state.callContext, pc)
            callee = calleeContext.method
            if !state.isSeenCallee(pc, callee) && !isIgnoredCallee(callee)
        } {
            // Some sanity checks ...
            // Methods with multiple defined methods should never appear as callees.
            assert(!callee.hasMultipleDefinedMethods)
            // Instances of DefinedMethod we see should only be those where the method is defined in the class file of
            // the declaring class type (i.e., it is not a DefinedMethod instance of some inherited method).
            assert(!callee.hasSingleDefinedMethod ||
                (callee.declaringClassType == callee.asDefinedMethod.definedMethod.classFile.thisType))

            // Remember callee (with PC) so we don't have to process it again later.
            state.addSeenCallee(pc, callee)

            maybeRegisterMethodForForwardPropagation(callee, pc, bytecode)

            maybeRegisterMethodForBackwardPropagation(callee, pc, bytecode)
        }
    }

    private def maybeRegisterMethodForForwardPropagation(
        callee:   DeclaredMethod,
        pc:       Int,
        bytecode: Code
    )(
        implicit
        state:          State,
        partialResults: ArrayBuffer[SomePartialResult]
    ): Unit = {
        val params = UIDSet.newBuilder[ReferenceType]

        for (param <- callee.descriptor.parameterTypes) {
            if (param.isReferenceType) {
                params += param.asReferenceType
            }
        }

        // If the call is not static, we need to take the implicit "this" parameter into account.
        if (callee.hasSingleDefinedMethod && !callee.definedMethod.isStatic ||
            !callee.hasSingleDefinedMethod && !bytecode.instructions(pc).isInvokeStatic) {
            params += callee.declaringClassType
        }

        // If we do not have any params at this point, there is no forward propagation!
        val typeFilters = params.result()
        if (typeFilters.isEmpty) {
            return ;
        }

        registerEntityForForwardPropagation(callee, typeFilters)
    }

    private def maybeRegisterMethodForBackwardPropagation(
        callee:   DeclaredMethod,
        pc:       Int,
        bytecode: Code
    )(
        implicit
        state:          State,
        partialResults: ArrayBuffer[SomePartialResult]
    ): Unit = {
        val returnValueIsUsed = {
            val tacIndex = state.tac.properStmtIndexForPC(pc)
            val tacInstr = state.tac.instructions(tacIndex)
            tacInstr.isAssignment
        }

        if (returnValueIsUsed) {
            // Internally, generic methods have return type "Object" due to type erasure. In many cases
            // (but not all!), the Java compiler will place the "actual" return type within a checkcast
            // instruction right after the call.
            val mostPreciseReturnType = {
                val nextPc = bytecode.pcOfNextInstruction(pc)
                val nextInstruction = bytecode.instructions(nextPc)
                if (nextInstruction.isCheckcast) {
                    nextInstruction.asInstanceOf[CHECKCAST].referenceType
                } else {
                    callee.descriptor.returnType
                }
            }

            // Return type could also be a basic type (i.e., int). We don't care about those.
            if (mostPreciseReturnType.isReferenceType) {
                registerEntityForBackwardPropagation(callee, mostPreciseReturnType.asReferenceType)
            }
        }
    }

    private def registerEntityForForwardPropagation(
        e:       Entity,
        filters: UIDSet[ReferenceType]
    )(
        implicit
        state:          State,
        partialResults: ArrayBuffer[SomePartialResult]
    ): Unit = {
        // Propagation from and to the same entity can be ignored.
        val typeSetEntity = selectTypeSetEntity(e)
        if (typeSetEntity == state.typeSetEntity) {
            return ;
        }

        val filterSetHasChanged = state.registerForwardPropagationEntity(typeSetEntity, filters)
        if (filterSetHasChanged) {
            val propagationResult = propagateTypes(typeSetEntity, state.ownInstantiatedTypes, state.forwardPropagationFilters(typeSetEntity))
            if (propagationResult.isDefined)
                partialResults += propagationResult.get
        }
    }

    private def registerEntityForBackwardPropagation(
        e:                     Entity,
        mostPreciseUpperBound: ReferenceType
    )(
        implicit
        state:          State,
        partialResults: ArrayBuffer[SomePartialResult]
    ): Unit = {
        val typeSetEntity = selectTypeSetEntity(e)
        if (typeSetEntity == state.typeSetEntity) {
            return ;
        }

        val filter = UIDSet(mostPreciseUpperBound)

        if (!state.backwardPropagationDependeeIsRegistered(typeSetEntity)) {
            val dependee = propertyStore(typeSetEntity, InstantiatedTypes.key)

            state.updateBackwardPropagationDependee(dependee)
            state.updateBackwardPropagationFilters(typeSetEntity, filter)

            if (dependee.hasNoUBP) {
                return ;
            }

            val propagation = propagateTypes(state.typeSetEntity, dependee.ub.types, filter)
            if (propagation.isDefined) {
                partialResults += propagation.get
            }
        } else {
            val filterSetHasChanged = state.updateBackwardPropagationFilters(typeSetEntity, filter)
            if (filterSetHasChanged) {
                // Since the filters were updated, it is possible that types which were previously seen but not
                // propagated are now relevant for back propagation. Therefore, we need to propagate from the
                // entire dependee type set.
                val allDependeeTypes = state.backwardPropagationDependeeInstantiatedTypes(typeSetEntity)
                val propagation = propagateTypes(state.typeSetEntity, allDependeeTypes, filter)
                if (propagation.isDefined) {
                    partialResults += propagation.get
                }
            }
        }
    }

    private def candidateMatchesTypeFilter(candidateType: ReferenceType, filterType: ReferenceType): Boolean = {
        val answer = classHierarchy.isASubtypeOf(candidateType, filterType)

        if (answer.isYesOrNo) {
            // Here, we know for sure that the candidate type is or is not a subtype of the filter type.
            answer.isYes
        } else {
            // If the answer is Unknown, we don't know for sure whether the candidate is a subtype of the filter type.
            // However, ClassHierarchy returns Unknown even for cases where it is very unlikely that this is the case.
            // Therefore, we take some more features into account to make the filtering more precise.

            // Important: This decision is a possible but unlikely cause of unsoundness in the call graph!

            // If the filter type is not a project type (i.e., it is external), we assume that any candidate type
            // is a subtype. This can be any external type or project types for which we have incomplete supertype
            // information.
            // If the filter type IS a project type, we consider the candidate type not to be a subtype since this is
            // very likely to be not the case. For the candidate type, there are two options: Either it is an external
            // type, in which case the candidate type could only be a subtype if project types are available in the
            // external type's project at compile time. This is very unlikely since external types are almost always
            // from libraries (like the JDK) which are not available in the analysis context, and which were almost
            // certainly compiled separately ("Separate Compilation Assumption").
            // The other option is that the candidate is also a project type, in which case we should have gotten a
            // definitive Yes/No answer before. Since we didn't get one, the candidate type probably has a supertype
            // which is not a project type. In that case, the above argument applies similarly.

            val filterTypeIsProjectType = if (filterType.isObjectType) {
                project.isProjectType(filterType.asObjectType)
            } else {
                val at = filterType.asArrayType
                project.isProjectType(at.elementType.asObjectType)
            }

            !filterTypeIsProjectType
        }
    }

    private def propagateTypes[E >: Null <: TypeSetEntity](
        targetSetEntity: E,
        newTypes:        UIDSet[ReferenceType],
        filters:         Set[ReferenceType]
    ): Option[PartialResult[E, InstantiatedTypes]] = {

        if (newTypes.isEmpty) {
            return None;
        }

        val filteredTypes = newTypes.foldLeft(UIDSet.newBuilder[ReferenceType]) { (builder, nt) =>
            val fitr = filters.iterator
            var canditateMatches = false
            while (!canditateMatches && fitr.hasNext) {
                val tf = fitr.next()
                if (candidateMatchesTypeFilter(nt, tf)) {
                    canditateMatches = true
                    builder += nt
                }
            }
            builder
        }.result()

        if (filteredTypes.nonEmpty) {
            if (debug) _trace.traceTypePropagation(targetSetEntity, filteredTypes)
            val partialResult = PartialResult[E, InstantiatedTypes](
                targetSetEntity,
                InstantiatedTypes.key,
                InstantiatedTypes.update(targetSetEntity, filteredTypes)
            )

            Some(partialResult)
        } else {
            None
        }
    }

    private def returnResults(
        partialResults: IterableOnce[SomePartialResult]
    )(implicit state: State): ProperPropertyComputationResult = {
        // Always re-register the continuation. It is impossible for all dependees to be final in XTA/...
        Results(
            InterimPartialResult(state.dependees, c(state)),
            partialResults
        )
    }
}

final class TypePropagationAnalysisScheduler(
        val selectSetEntity: TypeSetEntitySelector
) extends BasicFPCFTriggeredAnalysisScheduler {

    override def requiredProjectInformation: ProjectInformationKeys = Seq(TypeProviderKey)

    override type InitializationData = Null

    override def triggeredBy: PropertyKind = Callers.key

    override def init(p: SomeProject, ps: PropertyStore): Null = null

    override def register(project: SomeProject, propertyStore: PropertyStore, i: Null): FPCFAnalysis = {
        val analysis = new TypePropagationAnalysis(project, selectSetEntity)
        propertyStore.registerTriggeredComputation(Callers.key, analysis.analyze)
        analysis
    }

    override def uses: Set[PropertyBounds] = PropertyBounds.ubs(InstantiatedTypes, Callees, TACAI)

    override def derivesEagerly: Set[PropertyBounds] = Set.empty

    override def derivesCollaboratively: Set[PropertyBounds] = PropertyBounds.ubs(InstantiatedTypes)
}
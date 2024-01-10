/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package pointsto

import scala.collection.mutable.ArrayBuffer

import org.opalj.br.ArrayType
import org.opalj.br.DeclaredField
import org.opalj.br.DeclaredMethod
import org.opalj.br.FieldType
import org.opalj.br.Method
import org.opalj.br.ObjectType
import org.opalj.br.PC
import org.opalj.br.ReferenceType
import org.opalj.br.analyses.DeclaredMethodsKey
import org.opalj.br.analyses.ProjectInformationKeys
import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.FPCFAnalysis
import org.opalj.br.fpcf.FPCFTriggeredAnalysisScheduler
import org.opalj.br.fpcf.analyses.SimpleContextProvider
import org.opalj.br.fpcf.properties.Context
import org.opalj.br.fpcf.properties.cg.Callees
import org.opalj.br.fpcf.properties.cg.Callers
import org.opalj.br.fpcf.properties.cg.NoCallees
import org.opalj.br.fpcf.properties.fieldaccess.MethodFieldAccessInformation
import org.opalj.br.fpcf.properties.fieldaccess.MethodFieldReadAccessInformation
import org.opalj.br.fpcf.properties.fieldaccess.MethodFieldWriteAccessInformation
import org.opalj.br.fpcf.properties.fieldaccess.NoMethodFieldReadAccessInformation
import org.opalj.br.fpcf.properties.fieldaccess.NoMethodFieldWriteAccessInformation
import org.opalj.br.fpcf.properties.pointsto.PointsToSetLike
import org.opalj.collection.immutable.IntTrieSet
import org.opalj.fpcf.Entity
import org.opalj.fpcf.EOptionP
import org.opalj.fpcf.EPK
import org.opalj.fpcf.EPS
import org.opalj.fpcf.InterimPartialResult
import org.opalj.fpcf.ProperPropertyComputationResult
import org.opalj.fpcf.PropertyBounds
import org.opalj.fpcf.PropertyKey
import org.opalj.fpcf.PropertyKind
import org.opalj.fpcf.PropertyMetaInformation
import org.opalj.fpcf.PropertyStore
import org.opalj.fpcf.Results
import org.opalj.fpcf.SomeEOptionP
import org.opalj.fpcf.SomeEPK
import org.opalj.fpcf.SomeEPS
import org.opalj.fpcf.UBP
import org.opalj.log.OPALLogger.logOnce
import org.opalj.log.Warn
import org.opalj.tac.common.DefinitionSite
import org.opalj.tac.fpcf.analyses.cg.ReachableMethodAnalysis
import org.opalj.tac.fpcf.analyses.cg.valueOriginsOfPCs
import org.opalj.tac.fpcf.properties.TACAI
import org.opalj.value.IsMultipleReferenceValue
import org.opalj.value.IsReferenceValue
import org.opalj.value.IsSArrayValue
import org.opalj.value.ValueInformation

/**
 * A context-insensitive points-to analysis, that uses an abstract [[PointsToSetLike]] in order to
 * manage points-to sets.
 *
 * @author Florian Kuebler
 */
trait AbstractPointsToAnalysis extends PointsToAnalysisBase with ReachableMethodAnalysis {

    override def processMethod(
        callContext: ContextType,
        tacEP:       EPS[Method, TACAI]
    ): ProperPropertyComputationResult = {
        doProcessMethod(
            new PointsToAnalysisState[ElementType, PointsToSet, ContextType](callContext, tacEP)
        )
    }

    private[this] def doProcessMethod(
        implicit state: State
    ): ProperPropertyComputationResult = {
        if (state.hasTACDependee)
            throw new IllegalStateException("points to analysis does not support refinement based tac")
        val tac = state.tac
        val method = state.callContext.method.definedMethod

        if (method.returnType.isReferenceType) {
            state.includeSharedPointsToSet(
                state.callContext,
                emptyPointsToSet,
                t => classHierarchy.isSubtypeOf(t, method.returnType.asReferenceType)
            )
        }

        tac.cfg.abnormalReturnNode.predecessors.foreach { throwingBB =>
            val throwingStmt = tac.stmts(throwingBB.asBasicBlock.endPC)
            throwingStmt match {
                case Throw(_, UVar(_, defSites)) =>
                    val entity = MethodExceptions(state.callContext)
                    val filter = (t: ReferenceType) =>
                        classHierarchy.isSubtypeOf(t, ObjectType.Throwable)
                    state.includeSharedPointsToSets(
                        entity,
                        currentPointsToOfDefSites(entity, defSites, filter),
                        filter
                    )

                case Assignment(_, _, _: Call[_]) | ExprStmt(_, _: Call[_]) | _: Call[_] =>
                    val entity = MethodExceptions(state.callContext)
                    val callSite = getCallExceptions(throwingStmt.pc)
                    val filter = (t: ReferenceType) =>
                        classHierarchy.isSubtypeOf(t, ObjectType.Throwable)
                    state.includeSharedPointsToSet(
                        entity,
                        currentPointsTo(entity, callSite, filter),
                        filter
                    )

                case _ =>
                // TODO handle implicit exceptions (do that for Throw and Calls, too (NPE!))
            }
        }

        // Process indirect field accesses in this method
        handleIndirectFieldAccesses

        for (stmt <- tac.stmts) stmt match {
            case Assignment(pc, _, New(_, tpe)) =>
                handleAllocation(pc, tpe)

            case Assignment(pc, _, NewArray(_, counts, tpe)) =>
                handleArrayAllocation(pc, counts, tpe)

            case Assignment(pc, targetVar, const: Const) if targetVar.value.isReferenceValue =>
                val defSite = getDefSite(pc)
                state.setAllocationSitePointsToSet(
                    defSite,
                    if (const.isNullExpr)
                        emptyPointsToSet
                    // note, this is wrong for alias analyses
                    else
                        createPointsToSet(
                            pc,
                            state.callContext,
                            const.tpe.asObjectType,
                            isConstant = true
                        )
                )

            // that case should not happen
            case Assignment(pc, DVar(_: IsReferenceValue, _), UVar(_, defSites)) =>
                val defSiteObject = getDefSite(pc)
                val index = tac.properStmtIndexForPC(pc)
                val nextStmt = tac.stmts(index + 1)
                val filter = nextStmt match {
                    case Checkcast(_, value, cmpTpe) if value.asVar.definedBy.contains(index) =>
                        (t: ReferenceType) => classHierarchy.isSubtypeOf(t, cmpTpe)
                    case _ =>
                        PointsToSetLike.noFilter
                }
                state.includeSharedPointsToSets(
                    defSiteObject,
                    currentPointsToOfDefSites(defSiteObject, defSites, filter),
                    filter
                )

            case Assignment(
                    pc,
                    _,
                    GetField(_, declaringClass, name, fieldType: ReferenceType, UVar(_, objRefDefSites))
                ) =>
                handleGetField(Some(declaredFields(declaringClass, name, fieldType)), pc, objRefDefSites)

            case Assignment(pc, _, GetStatic(_, declaringClass, name, fieldType: ReferenceType)) =>
                handleGetStatic(declaredFields(declaringClass, name, fieldType), pc)

            case Assignment(
                    pc,
                    DVar(_: IsReferenceValue, _),
                    ArrayLoad(_, _, UVar(av: IsSArrayValue, arrayDefSites))
                ) =>
                val arrayType = av.theUpperTypeBound
                handleArrayLoad(arrayType, pc, arrayDefSites)

            case Assignment(
                    pc,
                    DVar(_: IsReferenceValue, _),
                    ArrayLoad(_, _, UVar(av: IsMultipleReferenceValue, arrayDefSites))
                ) =>
                val arrayType = av.leastUpperType.get.asArrayType
                handleArrayLoad(arrayType, pc, arrayDefSites)

            case Assignment(pc, _, call: FunctionCall[DUVar[ValueInformation]]) =>
                val callees: Callees = state.callees(ps)
                val targets = callees.callees(state.callContext, pc)
                val defSiteObject = getDefSite(pc)

                if (call.descriptor.returnType.isReferenceType) {
                    val index = tac.properStmtIndexForPC(pc)
                    val nextStmt = tac.stmts(index + 1)
                    val filter = nextStmt match {
                        case Checkcast(_, value, cmpTpe) if value.asVar.definedBy.contains(index) =>
                            (t: ReferenceType) => classHierarchy.isSubtypeOf(t, cmpTpe)
                        case _ =>
                            PointsToSetLike.noFilter
                    }
                    if (state.hasCalleesDepenedee) {
                        state.includeSharedPointsToSet(defSiteObject, emptyPointsToSet, filter)
                        state.addDependee(defSiteObject, state.calleesDependee, filter)
                    }
                    state.includeSharedPointsToSets(
                        defSiteObject,
                        targets.collect {
                            case target if target.method.descriptor.returnType.isReferenceType =>
                                currentPointsTo(defSiteObject, target, filter)
                        },
                        filter
                    )
                }
                handleCall(call, pc)

            case Assignment(pc, _, idc: InvokedynamicFunctionCall[_]) =>
                state.addIncompletePointsToInfo(pc)
                logOnce(
                    Warn("analysis - points-to analysis", s"unresolved invokedynamic: $idc")
                )

            case ExprStmt(pc, idc: InvokedynamicFunctionCall[V]) =>
                state.addIncompletePointsToInfo(pc)
                logOnce(
                    Warn("analysis - points-to analysis", s"unresolved invokedynamic: $idc")
                )

            case idc: InvokedynamicMethodCall[_] =>
                state.addIncompletePointsToInfo(idc.pc)
                logOnce(
                    Warn("analysis - points-to analysis", s"unresolved invokedynamic: $idc")
                )

            case Assignment(_, DVar(_: IsReferenceValue, _), _) =>
                throw new IllegalArgumentException(s"unexpected assignment: $stmt")

            case call: Call[_] =>
                handleCall(call.asInstanceOf[Call[DUVar[ValueInformation]]], call.pc)

            case ExprStmt(pc, call: Call[_]) =>
                handleCall(call.asInstanceOf[Call[DUVar[ValueInformation]]], pc)

            case PutField(
                    _,
                    declaringClass,
                    name,
                    fieldType: ReferenceType,
                    UVar(_, objRefDefSites),
                    UVar(_, defSites)
                ) =>
                handlePutField(Some(declaredFields(declaringClass, name, fieldType)), objRefDefSites, defSites)

            case PutStatic(_, declaringClass, name, fieldType: ReferenceType, UVar(_, defSites)) =>
                handlePutStatic(declaredFields(declaringClass, name, fieldType), defSites)

            case ArrayStore(_, UVar(av: IsSArrayValue, arrayDefSites), _, UVar(_: IsReferenceValue, defSites)) =>
                val arrayType = av.theUpperTypeBound
                handleArrayStore(arrayType, arrayDefSites, defSites)

            case ArrayStore(
                    _,
                    UVar(av: IsMultipleReferenceValue, arrayDefSites),
                    _,
                    UVar(_: IsReferenceValue, defSites)
                ) =>
                val arrayType = av.leastUpperType.get.asArrayType
                handleArrayStore(arrayType, arrayDefSites, defSites)

            case ReturnValue(_, UVar(_: IsReferenceValue, defSites)) =>
                val filter = (t: ReferenceType) =>
                    classHierarchy.isSubtypeOf(t, state.callContext.method.descriptor.returnType.asReferenceType)
                state.includeSharedPointsToSets(
                    state.callContext,
                    currentPointsToOfDefSites(state.callContext, defSites, filter),
                    filter
                )

            case _ =>
        }

        // TODO: we have to handle the exceptions that might implicitly be thrown by this method

        Results(createResults(state))
    }

    @inline private[this] def getCallExceptions(pc: Int)(implicit state: State): Entity = {
        val exceptions = CallExceptions(definitionSites(state.callContext.method.definedMethod, pc))
        typeIterator match {
            case _: SimpleContextProvider => exceptions
            case _                        => (state.callContext, exceptions)
        }
    }

    @inline private[this] def handleAllocation(
        pc:  Int,
        tpe: ReferenceType
    )(implicit state: State): Unit = {
        val defSite = getDefSite(pc)
        if (!state.hasAllocationSitePointsToSet(defSite)) {
            state.setAllocationSitePointsToSet(
                defSite,
                createPointsToSet(pc, state.callContext, tpe, isConstant = false)
            )
        }
    }

    @inline private[this] def handleArrayAllocation(
        pc:     Int,
        counts: Seq[Expr[V]],
        tpe:    ArrayType
    )(implicit state: State): Unit = {
        val defSite = getDefSite(pc)
        if (!state.hasAllocationSitePointsToSet(defSite)) {
            @inline def countIsZero(theCounts: Seq[Expr[V]]): Boolean = {
                theCounts.head.asVar.definedBy.forall { ds =>
                    ds >= 0 &&
                    state.tac.stmts(ds).asAssignment.expr.isIntConst &&
                    state.tac.stmts(ds).asAssignment.expr.asIntConst.value == 0
                }
            }

            val isEmptyArray = countIsZero(counts)
            var arrayReferencePTS = createPointsToSet(pc, state.callContext, tpe, isConstant = false, isEmptyArray)
            state.setAllocationSitePointsToSet(
                defSite,
                arrayReferencePTS
            )
            var remainingCounts = counts.tail
            var allocatedType: FieldType = tpe.componentType
            var continue = !isEmptyArray
            while (remainingCounts.nonEmpty && allocatedType.isArrayType && continue) {
                val theType = allocatedType.asArrayType
                val arrayEntity = ArrayEntity(arrayReferencePTS.getNewestElement())

                if (countIsZero(remainingCounts))
                    continue = false

                arrayReferencePTS = createPointsToSet(
                    pc,
                    state.callContext,
                    theType,
                    isConstant = false,
                    isEmptyArray = !continue
                )
                state.includeSharedPointsToSet(
                    arrayEntity,
                    arrayReferencePTS,
                    (t: ReferenceType) => classHierarchy.isSubtypeOf(t, theType)
                )

                remainingCounts = remainingCounts.tail
                allocatedType = theType.componentType
            }
        }
    }

    // maps the points-to set of actual parameters (including *this*) the the formal parameters
    private[this] def handleCall(
        call: Call[DUVar[ValueInformation]],
        pc:   Int
    )(implicit state: State): Unit = {
        val tac = state.tac
        val callees: Callees = state.callees(ps)

        for (target <- callees.directCallees(state.callContext, pc)) {
            handleDirectCall(call, pc, target)
        }

        for (target <- callees.indirectCallees(state.callContext, pc)) {
            handleIndirectCall(pc, target, callees, tac)
        }

        val callExceptions = getCallExceptions(pc)
        val filter = (t: ReferenceType) => classHierarchy.isSubtypeOf(t, ObjectType.Throwable)
        for (target <- callees.callees(state.callContext, pc)) {
            val targetExceptions = MethodExceptions(target)
            state.includeSharedPointsToSet(
                callExceptions,
                currentPointsTo(callExceptions, targetExceptions, filter),
                filter
            )
        }
        if (state.hasCalleesDepenedee) {
            state.addDependee(callExceptions, state.calleesDependee, filter)
            state.includeSharedPointsToSet(
                callExceptions,
                emptyPointsToSet,
                filter
            )
        }
    }

    private[this] def handleDirectCall(
        call:   Call[V],
        pc:     Int,
        target: Context
    )(implicit state: State): Unit = {
        val receiverOpt: Option[Expr[DUVar[ValueInformation]]] = call.receiverOption
        val fps = formalParameters(target.method)

        if (fps != null) {
            // handle receiver for non static methods
            if (receiverOpt.isDefined) {
                val isNonVirtualCall = call match {
                    case _: NonVirtualFunctionCall[V] | _: NonVirtualMethodCall[V] => true
                    case _                                                         => false
                }
                handleCallReceiver(receiverOpt.get.asVar.definedBy, target, isNonVirtualCall)
            }

            val descriptor = target.method.descriptor
            // in case of signature polymorphic methods, we give up
            if (call.params.size == descriptor.parametersCount) {
                // handle params
                for (i <- 0 until descriptor.parametersCount) {
                    handleCallParameter(call.params(i).asVar.definedBy, i, target)
                }
            } else {
                // it is not needed to mark it as incomplete here
            }
        } else {
            state.addIncompletePointsToInfo(pc)
        }
    }

    // TODO reduce code duplication
    private def handleIndirectCall(
        pc:      Int,
        target:  Context,
        callees: Callees,
        tac:     TACode[TACMethodParameter, DUVar[ValueInformation]]
    )(implicit state: State): Unit = {
        val targetMethod = target.method
        val fps = formalParameters(targetMethod)

        val indirectParams = callees.indirectCallParameters(state.callContext, pc, target)
        val descriptor = targetMethod.descriptor

        // Prevent spuriously matched targets (e.g. from tamiflex with unknown source line number)
        // from interfering with the points-to analysis
        // TODO That rather is a responsibility of the reflection analysis though
        if (indirectParams.isEmpty || descriptor.parametersCount == indirectParams.size) {
            if (fps != null) {
                // handle receiver for non static methods
                val receiverOpt = callees.indirectCallReceiver(state.callContext, pc, target)
                if (receiverOpt.isDefined && !targetMethod.definedMethod.isStatic) {
                    val receiverDefSites = valueOriginsOfPCs(receiverOpt.get._2, tac.pcToIndex)
                    handleCallReceiver(
                        receiverDefSites,
                        target,
                        isNonVirtualCall = targetMethod.definedMethod.isConstructor,
                        indirectConstructorPCAndType =
                            if (targetMethod.definedMethod.isConstructor)
                                Some((pc, receiverOpt.get._1.asReferenceValue.asReferenceType))
                            else None
                    )
                } else if (targetMethod.definedMethod.isConstructor) {
                    handleCallReceiver(
                        IntTrieSet(tac.properStmtIndexForPC(pc)),
                        target,
                        isNonVirtualCall = true,
                        indirectConstructorPCAndType = Some((pc, targetMethod.declaringClassType))
                    )
                } else {
                    // TODO distinguish between static methods and unavailable info
                }

                for (i <- indirectParams.indices) {
                    val paramType = descriptor.parameterType(i)
                    if (paramType.isReferenceType) {
                        val indirectParam = indirectParams(i)
                        if (indirectParam.isDefined) {
                            handleCallParameter(
                                valueOriginsOfPCs(indirectParam.get._2, tac.pcToIndex),
                                i,
                                target
                            )
                        } else {
                            state.addIncompletePointsToInfo(pc)
                        }
                    }
                }
            } else {
                state.addIncompletePointsToInfo(pc)
            }
        }
    }

    private[this] def handleIndirectFieldAccesses(implicit state: State): Unit = {
        val tac = state.tac
        val readAccesses = state.readAccesses(ps)
        val writeAccesses = state.writeAccesses(ps)

        for {
            pc <- readAccesses.getAccessSites(state.callContext);
            target <- readAccesses.getIndirectAccessedFields(state.callContext, pc)
        } {
            handleIndirectFieldReadAccess(pc, target, readAccesses, tac)
        }

        for {
            pc <- writeAccesses.getAccessSites(state.callContext);
            target <- writeAccesses.getIndirectAccessedFields(state.callContext, pc)
        } {
            handleIndirectFieldWriteAccess(pc, target, writeAccesses, tac)
        }
    }

    private def handleIndirectFieldReadAccess(
        pc:           Int,
        target:       DeclaredField,
        readAccesses: MethodFieldReadAccessInformation,
        tac:          TACode[TACMethodParameter, DUVar[ValueInformation]]
    )(implicit state: State): Unit = {
        // IMPROVE add static field information for virtual declared fields
        if (target.isDefinedField) {
            val receiverOpt = readAccesses.indirectAccessReceiver(state.callContext, pc, target)
            if (target.definedField.isStatic) {
                handleGetStatic(target, pc)
            } else if (receiverOpt.isDefined) {
                // handle receiver for non static fields if available
                handleGetField(
                    Some(target),
                    pc,
                    valueOriginsOfPCs(receiverOpt.get._2, tac.pcToIndex)
                )
            }
        }
    }

    private def handleIndirectFieldWriteAccess(
        pc:            Int,
        target:        DeclaredField,
        writeAccesses: MethodFieldWriteAccessInformation,
        tac:           TACode[TACMethodParameter, DUVar[ValueInformation]]
    )(implicit state: State): Unit = {
        val indirectParam = writeAccesses.indirectAccessParameter(state.callContext, pc, target)
        val rhsDefSites = if (target.fieldType.isReferenceType) {
            if (indirectParam.isDefined) {
                valueOriginsOfPCs(indirectParam.get._2, tac.pcToIndex)
            } else {
                state.addIncompletePointsToInfo(pc)
                IntTrieSet.empty
            }
        } else IntTrieSet.empty

        // IMPROVE add static field information for virtual declared fields
        if (target.isDefinedField) {
            val receiverOpt = writeAccesses.indirectAccessReceiver(state.callContext, pc, target)
            if (target.definedField.isStatic) {
                handlePutStatic(target, rhsDefSites)
            } else if (receiverOpt.isDefined) {
                // handle receiver for non static fields if available
                handlePutField(
                    Some(target),
                    valueOriginsOfPCs(receiverOpt.get._2, tac.pcToIndex),
                    rhsDefSites
                )
            }
        }
    }

    override protected[this] def createResults(
        implicit state: State
    ): ArrayBuffer[ProperPropertyComputationResult] = {
        val results = super.createResults(state)

        if (state.hasCalleesDepenedee) {
            val calleesDependee = state.calleesDependee
            results += InterimPartialResult(
                Set(calleesDependee),
                continuationForCallees(
                    calleesDependee,
                    new PointsToAnalysisState[ElementType, PointsToSet, ContextType](
                        state.callContext,
                        state.tacDependee
                    )
                )
            )
        }

        if (state.hasReadAccessDependee) {
            val readAccessDependee = state.readAccessDependee
            results += InterimPartialResult(
                Set(readAccessDependee),
                continuationForFieldAccesses[MethodFieldReadAccessInformation](
                    readAccessDependee,
                    NoMethodFieldReadAccessInformation,
                    MethodFieldReadAccessInformation.key,
                    (pc, target, newAccesses, tac, state) =>
                        handleIndirectFieldReadAccess(pc, target, newAccesses, tac)(state),
                    (currentState, newDependee) => currentState.setReadAccessDependee(newDependee),
                    new PointsToAnalysisState[ElementType, PointsToSet, ContextType](
                        state.callContext,
                        state.tacDependee
                    )
                )
            )
        }

        if (state.hasWriteAccessDependee) {
            val writeAccessDependee = state.writeAccessDependee
            results += InterimPartialResult(
                Set(writeAccessDependee),
                continuationForFieldAccesses[MethodFieldWriteAccessInformation](
                    writeAccessDependee,
                    NoMethodFieldWriteAccessInformation,
                    MethodFieldWriteAccessInformation.key,
                    (pc, target, newAccesses, tac, state) =>
                        handleIndirectFieldWriteAccess(pc, target, newAccesses, tac)(state),
                    (currentState, newDependee) => currentState.setWriteAccessDependee(newDependee),
                    new PointsToAnalysisState[ElementType, PointsToSet, ContextType](
                        state.callContext,
                        state.tacDependee
                    )
                )
            )
        }

        results
    }

    override protected[this] def continuationForShared(
        e:         Entity,
        dependees: Map[SomeEPK, (SomeEOptionP, ReferenceType => Boolean)],
        state:     State
    )(eps: SomeEPS): ProperPropertyComputationResult = {
        // The shared entities are not affected by changes of the tac and use partial results.
        // Thus, we could simply recompute them on updates for the tac
        eps match {
            case UBP(callees: Callees) =>
                // this will never happen for method return values or method exceptions
                val (defSite, dependeeIsExceptions) = e match {
                    case ds: DefinitionSite               => (ds, false)
                    case (_: Context, ds: DefinitionSite) => (ds, false)
                    case ce: CallExceptions               => (ce.defSite, true)
                    case (_: Context, ce: CallExceptions) => (ce.defSite, true)
                }
                // TODO: Only handle new callees
                val tgts = callees.callees(state.callContext, defSite.pc)

                val typeFilter = dependees(eps.toEPK)._2

                var newDependees = updatedDependees(eps, dependees)
                var newPointsToSet = emptyPointsToSet
                tgts.foreach { target =>
                    val entity = if (dependeeIsExceptions) MethodExceptions(target) else target
                    // if we already have a dependency to that method, we do not need to process it
                    // otherwise, it might still be the case that we processed it before but it is
                    // final and thus not part of dependees anymore
                    if (dependeeIsExceptions ||
                        target.method.descriptor.returnType.isReferenceType
                    ) {
                        if (!dependees.contains(EPK(entity, pointsToPropertyKey))) {
                            val p2s = ps(entity, pointsToPropertyKey)
                            if (p2s.isRefinable) {
                                newDependees += (p2s.toEPK -> ((p2s, typeFilter)))
                            }
                            newPointsToSet = newPointsToSet.included(pointsToUB(p2s), typeFilter)
                        }
                    }
                }

                val results = createPartialResults(
                    e,
                    newPointsToSet,
                    newDependees,
                    { old => old.included(newPointsToSet, typeFilter) },
                    true
                )(state)

                Results(results)

            case UBP(fai: MethodFieldReadAccessInformation) =>
                val defSite = e match {
                    case ds: DefinitionSite               => ds
                    case (_: Context, ds: DefinitionSite) => ds
                }
                // IMPROVE: Only handle new accesses
                val fields = fai.getAccessedFields(state.callContext, defSite.pc)

                val typeFilter = dependees(eps.toEPK)._2

                var newDependees = updatedDependees(eps, dependees)
                var newPointsToSet = emptyPointsToSet
                fields.foreach { field =>
                    // if we already have a dependency to that field, we do not need to process it
                    // otherwise, it might still be the case that we processed it before but it is
                    // final and thus not part of dependees anymore
                    if (field.fieldType.isReferenceType) {
                        if (!dependees.contains(EPK(field, pointsToPropertyKey))) {
                            val p2s = ps(field, pointsToPropertyKey)
                            if (p2s.isRefinable) {
                                newDependees += (p2s.toEPK -> ((p2s, typeFilter)))
                            }
                            newPointsToSet = newPointsToSet.included(pointsToUB(p2s), typeFilter)
                        }
                    }
                }

                val results = createPartialResults(
                    e,
                    newPointsToSet,
                    newDependees,
                    { old => old.included(newPointsToSet, typeFilter) },
                    true
                )(state)

                Results(results)

            case _ => super.continuationForShared(e, dependees, state)(eps)
        }
    }

    private def continuationForCallees(
        oldCalleeEOptP: EOptionP[DeclaredMethod, Callees],
        state:          State
    )(eps: SomeEPS): ProperPropertyComputationResult = {
        eps match {
            case UBP(newCallees: Callees) =>
                val tac = state.tac
                val oldCallees = if (oldCalleeEOptP.hasUBP) oldCalleeEOptP.ub else NoCallees
                for {
                    (pc, targets) <- newCallees.directCallSites(state.callContext)
                    target <- targets
                } {
                    if (!oldCallees.containsDirectCall(state.callContext, pc, target)) {
                        val call = tac.stmts(tac.properStmtIndexForPC(pc)) match {
                            case call: Call[DUVar[ValueInformation]] @unchecked =>
                                call
                            case Assignment(_, _, call: Call[DUVar[ValueInformation]] @unchecked) =>
                                call
                            case ExprStmt(_, call: Call[DUVar[ValueInformation]] @unchecked) =>
                                call
                            case e =>
                                throw new IllegalArgumentException(s"unexpected stmt $e")
                        }
                        handleDirectCall(call, pc, target)(state)
                    }
                }
                for {
                    (pc, targets) <- newCallees.indirectCallSites(state.callContext)
                    target <- targets
                } {
                    if (!oldCallees.containsIndirectCall(state.callContext, pc, target)) {
                        handleIndirectCall(pc, target, newCallees, tac)(state)
                    }
                }

                state.setCalleesDependee(eps.asInstanceOf[EPS[DeclaredMethod, Callees]])

                Results(createResults(state))
            case _ => throw new IllegalArgumentException(s"unexpected eps $eps")
        }
    }

    private def continuationForFieldAccesses[P <: MethodFieldAccessInformation[P]](
        oldAccessEOptP: EOptionP[Method, P],
        noAccesses:     P,
        key:            PropertyKey[P],
        handleIndirectFieldAccess: (
            PC,
            DeclaredField,
            P,
            TACode[TACMethodParameter, DUVar[ValueInformation]],
            State
        ) => Unit,
        setDependee: (State, EOptionP[Method, P]) => Unit,
        state:       State
    )(eps: SomeEPS): ProperPropertyComputationResult = {
        eps.pk match {
            case `key` =>
                val newAccesses = if (eps.hasUBP) eps.ub.asInstanceOf[P] else noAccesses
                val oldAccesses = if (oldAccessEOptP.hasUBP) oldAccessEOptP.ub else noAccesses

                val tac = state.tac
                for {
                    pc <- newAccesses.getIndirectAccessSites(state.callContext)
                    target <- newAccesses.getNewestNIndirectAccessedFields(
                        state.callContext,
                        pc,
                        newAccesses.numIndirectAccesses(state.callContext, pc) -
                            oldAccesses.numIndirectAccesses(state.callContext, pc)
                    )
                } {
                    handleIndirectFieldAccess(pc, target, newAccesses, tac, state)
                }

                setDependee(state, eps.asInstanceOf[EPS[Method, P]])

                Results(createResults(state))
            case _ => throw new IllegalArgumentException(s"unexpected eps $eps")
        }
    }
}

trait AbstractPointsToAnalysisScheduler extends FPCFTriggeredAnalysisScheduler {
    def propertyKind: PropertyMetaInformation
    def createAnalysis: SomeProject => AbstractPointsToAnalysis

    override type InitializationData = Null

    override def requiredProjectInformation: ProjectInformationKeys =
        AbstractPointsToBasedAnalysis.requiredProjectInformation :+ DeclaredMethodsKey

    override def uses: Set[PropertyBounds] = PropertyBounds.ubs(
        Callers,
        Callees,
        TACAI,
        propertyKind,
        MethodFieldReadAccessInformation,
        MethodFieldWriteAccessInformation
    )

    override def derivesCollaboratively: Set[PropertyBounds] = Set(PropertyBounds.ub(propertyKind))

    override def derivesEagerly: Set[PropertyBounds] = Set.empty

    override def init(p: SomeProject, ps: PropertyStore): Null = {
        null
    }

    override def beforeSchedule(p: SomeProject, ps: PropertyStore): Unit = {}

    override def register(
        p:      SomeProject,
        ps:     PropertyStore,
        unused: Null
    ): AbstractPointsToAnalysis = {
        val analysis = createAnalysis(p)
        // register the analysis for initial values for callers (i.e. methods becoming reachable)
        ps.registerTriggeredComputation(Callers.key, analysis.analyze)
        analysis
    }

    override def afterPhaseScheduling(ps: PropertyStore, analysis: FPCFAnalysis): Unit = {}

    override def afterPhaseCompletion(
        p:        SomeProject,
        ps:       PropertyStore,
        analysis: FPCFAnalysis
    ): Unit = {}

    override def triggeredBy: PropertyKind = Callers
}

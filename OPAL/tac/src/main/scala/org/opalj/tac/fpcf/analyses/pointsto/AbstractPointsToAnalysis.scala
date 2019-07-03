/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package pointsto

import scala.collection.mutable.ArrayBuffer

import org.opalj.log.OPALLogger.logOnce
import org.opalj.log.Warn
import org.opalj.collection.immutable.IntTrieSet
import org.opalj.collection.immutable.UIDSet
import org.opalj.fpcf.Entity
import org.opalj.fpcf.EOptionP
import org.opalj.fpcf.EPK
import org.opalj.fpcf.EPS
import org.opalj.fpcf.InterimEUBP
import org.opalj.fpcf.InterimPartialResult
import org.opalj.fpcf.InterimResult
import org.opalj.fpcf.PartialResult
import org.opalj.fpcf.ProperPropertyComputationResult
import org.opalj.fpcf.Property
import org.opalj.fpcf.Result
import org.opalj.fpcf.Results
import org.opalj.fpcf.SomeEOptionP
import org.opalj.fpcf.SomeEPK
import org.opalj.fpcf.SomeEPS
import org.opalj.fpcf.UBP
import org.opalj.value.IsReferenceValue
import org.opalj.value.IsSArrayValue
import org.opalj.value.ValueInformation
import org.opalj.br.DefinedMethod
import org.opalj.br.fpcf.properties.cg.Callees
import org.opalj.br.DeclaredMethod
import org.opalj.br.Method
import org.opalj.br.ObjectType
import org.opalj.br.analyses.VirtualFormalParameters
import org.opalj.br.analyses.VirtualFormalParametersKey
import org.opalj.br.fpcf.properties.cg.NoCallees
import org.opalj.br.fpcf.properties.pointsto.PointsToSetLike
import org.opalj.br.Field
import org.opalj.br.analyses.VirtualFormalParameter
import org.opalj.tac.common.DefinitionSite
import org.opalj.tac.common.DefinitionSites
import org.opalj.tac.common.DefinitionSitesKey
import org.opalj.tac.fpcf.analyses.cg.ReachableMethodAnalysis
import org.opalj.tac.fpcf.analyses.cg.valueOriginsOfPCs
import org.opalj.tac.fpcf.analyses.cg.V
import org.opalj.tac.fpcf.properties.TACAI

/**
 *
 * @author Florian Kuebler
 */
trait AbstractPointsToAnalysis[ElementType, PointsToSet >: Null <: PointsToSetLike[ElementType, _, PointsToSet]]
        extends AbstractPointsToBasedAnalysis[Entity, PointsToSet]
        with ReachableMethodAnalysis {

    protected[this] implicit val formalParameters: VirtualFormalParameters = {
        p.get(VirtualFormalParametersKey)
    }
    protected[this] implicit val definitionSites: DefinitionSites = {
        p.get(DefinitionSitesKey)
    }

    def createPointsToSet(
        pc: Int, declaredMethod: DeclaredMethod, allocatedType: ObjectType
    ): PointsToSet

    type State = PointsToAnalysisState[ElementType, PointsToSet]

    override def processMethod(
        definedMethod: DefinedMethod, tacEP: EPS[Method, TACAI]
    ): ProperPropertyComputationResult = {
        doProcessMethod(new PointsToAnalysisState[ElementType, PointsToSet](definedMethod, tacEP))
    }

    private[this] def doProcessMethod(
        implicit
        state: State
    ): ProperPropertyComputationResult = {

        if (state.hasTACDependee)
            throw new IllegalStateException("points to analysis does not support refinement based tac")
        val tac = state.tac
        val method = state.method.definedMethod
        if (method.returnType.isReferenceType) {
            state.setLocalPointsToSet(state.method, emptyPointsToSet)
        }

        for (stmt ← tac.stmts) stmt match {
            case Assignment(pc, _, New(_, t)) ⇒
                val defSite = definitionSites(method, pc)
                state.setAllocationSitePointsToSet(defSite, createPointsToSet(pc, state.method, t))

            case Assignment(pc, _, NewArray(_, _, tpe)) if tpe.elementType.isObjectType ⇒
                val defSite = definitionSites(method, pc)
                state.setAllocationSitePointsToSet(
                    defSite,
                    createPointsToSet(pc, state.method, tpe.elementType.asObjectType)
                )

            case Assignment(pc, targetVar, const: Const) if targetVar.value.isReferenceValue ⇒
                val defSite = definitionSites(method, pc)
                state.setAllocationSitePointsToSet(
                    defSite,
                    if (const.isNullExpr) emptyPointsToSet
                    // note, this is wrong for alias analyses
                    else createPointsToSet(pc, state.method, const.tpe.asObjectType)
                )

            // that case should not happen
            case Assignment(pc, DVar(_: IsReferenceValue, _), UVar(_, defSites)) ⇒
                val defSiteObject = definitionSites(method, pc)
                state.includeLocalPointsToSets(
                    defSiteObject, currentPointsToOfDefSites(defSiteObject, defSites)
                )

            case Assignment(pc, DVar(_: IsReferenceValue, _), GetField(_, declaringClass, name, fieldType, UVar(_, objRefDefSites))) ⇒
                val fieldOpt = p.resolveFieldReference(declaringClass, name, fieldType)
                if (fieldOpt.isDefined) {
                    val defSiteObject = definitionSites(method, pc)
                    val fakeEntity = (defSiteObject, fieldOpt.get)
                    state.addGetFieldEntity(fakeEntity)
                    currentPointsToOfDefSites(fakeEntity, objRefDefSites).foreach { pts ⇒
                        pts.forNewestNElements(pts.numElements) { as ⇒
                            state.includeSharedPointsToSet(
                                defSiteObject,
                                currentPointsTo(defSiteObject, (as, fieldOpt.get))
                            )
                        }
                    }
                } else {
                    state.addIncompletePointsToInfo(pc)
                }

            case Assignment(pc, DVar(_: IsReferenceValue, _), GetStatic(_, declaringClass, name, fieldType)) ⇒
                val defSiteObject = definitionSites(method, pc)
                val fieldOpt = p.resolveFieldReference(declaringClass, name, fieldType)
                if (fieldOpt.isDefined) {
                    state.includeLocalPointsToSet(
                        defSiteObject,
                        currentPointsTo(defSiteObject, fieldOpt.get)
                    )
                } else {
                    state.addIncompletePointsToInfo(pc)
                }

            case Assignment(pc, _, ArrayLoad(_, _, UVar(av: IsSArrayValue, _))) if av.theUpperTypeBound.elementType.isObjectType ⇒
                val defSiteObject = definitionSites(method, pc)
                val arrayBaseType = av.theUpperTypeBound.elementType
                state.includeLocalPointsToSet(
                    defSiteObject,
                    currentPointsTo(defSiteObject, arrayBaseType)
                )

            case Assignment(pc, targetVar, call: FunctionCall[DUVar[ValueInformation]]) ⇒
                val callees: Callees = state.callees(ps)
                val targets = callees.callees(pc)
                val defSiteObject = definitionSites(method, pc)

                if (targetVar.value.isReferenceValue) {
                    if (state.hasCalleesDepenedee) {
                        state.includeLocalPointsToSet(defSiteObject, emptyPointsToSet)
                        state.addDependee(defSiteObject, state.calleesDependee)
                    }
                    state.includeLocalPointsToSets(
                        defSiteObject,
                        targets.map(currentPointsTo(defSiteObject, _))
                    )
                }
                handleCall(call, pc)

            case Assignment(pc, _, idc: InvokedynamicFunctionCall[_]) ⇒
                state.addIncompletePointsToInfo(pc)
                logOnce(
                    Warn("analysis - points-to analysis", s"unresolved invokedynamic: $idc")
                )

            case ExprStmt(pc, idc: InvokedynamicFunctionCall[V]) ⇒
                state.addIncompletePointsToInfo(pc)
                logOnce(
                    Warn("analysis - points-to analysis", s"unresolved invokedynamic: $idc")
                )

            case idc: InvokedynamicMethodCall[_] ⇒
                state.addIncompletePointsToInfo(idc.pc)
                logOnce(
                    Warn("analysis - points-to analysis", s"unresolved invokedynamic: $idc")
                )

            case Assignment(_, DVar(av: IsSArrayValue, _), _) if av.theUpperTypeBound.elementType.isObjectType ⇒
                throw new IllegalArgumentException(s"unexpected assignment: $stmt")

            case Assignment(_, DVar(rv: IsReferenceValue, _), _) if !rv.isInstanceOf[IsSArrayValue] ⇒
                throw new IllegalArgumentException(s"unexpected assignment: $stmt")

            case call: Call[_] ⇒
                handleCall(call.asInstanceOf[Call[DUVar[ValueInformation]]], call.pc)

            case ExprStmt(pc, call: Call[_]) ⇒
                handleCall(call.asInstanceOf[Call[DUVar[ValueInformation]]], pc)

            case ArrayStore(_, UVar(av: IsSArrayValue, _), _, UVar(_, defSites)) if av.theUpperTypeBound.elementType.isReferenceType ⇒
                val arrayBaseType = av.theUpperTypeBound.elementType
                state.includeSharedPointsToSets(
                    arrayBaseType, currentPointsToOfDefSites(arrayBaseType, defSites)
                )

            case PutField(pc, declaringClass, name, fieldType: ObjectType, UVar(_, objRefDefSites), UVar(_, defSites)) ⇒
                val fieldOpt = p.resolveFieldReference(declaringClass, name, fieldType)
                if (fieldOpt.isDefined) {
                    val entity = (defSites, fieldOpt.get)
                    state.addPutFieldEntity(entity)
                    currentPointsToOfDefSites(entity, objRefDefSites).foreach { pts ⇒
                        pts.forNewestNElements(pts.numElements) { as ⇒
                            state.includeSharedPointsToSets(
                                (as, fieldOpt.get),
                                currentPointsToOfDefSites((as, fieldOpt.get), defSites)
                            )
                        }
                    }
                } else {
                    state.addIncompletePointsToInfo(pc)
                }

            case PutStatic(pc, declaringClass, name, fieldType: ObjectType, UVar(_, defSites)) ⇒
                val fieldOpt = p.resolveFieldReference(declaringClass, name, fieldType)
                if (fieldOpt.isDefined)
                    state.includeSharedPointsToSets(
                        fieldOpt.get, currentPointsToOfDefSites(fieldOpt.get, defSites)
                    )
                else {
                    state.addIncompletePointsToInfo(pc)
                }

            case ReturnValue(_, UVar(_: IsReferenceValue, defSites)) ⇒
                state.includeLocalPointsToSets(
                    state.method, currentPointsToOfDefSites(state.method, defSites)
                )

            case _ ⇒
        }

        // todo: we have to handle the exceptions that might be thrown by this method

        returnResult(state)
    }

    // maps the points-to set of actual parameters (including *this*) the the formal parameters
    private[this] def handleCall(
        call: Call[DUVar[ValueInformation]], pc: Int
    )(implicit state: State): Unit = {
        val tac = state.tac
        val callees: Callees = state.callees(ps)

        for (target ← callees.directCallees(pc)) {
            handleDirectCall(call, pc, target)
        }

        for (target ← callees.indirectCallees(pc)) {
            handleIndirectCall(pc, target, callees, tac)
        }
    }

    private[this] def handleDirectCall(
        call: Call[DUVar[ValueInformation]], pc: Int, target: DeclaredMethod
    )(implicit state: State): Unit = {
        val receiverOpt: Option[Expr[DUVar[ValueInformation]]] = call.receiverOption
        val fps = formalParameters(target)

        if (fps != null) {
            // handle receiver for non static methods
            if (receiverOpt.isDefined) {
                val fp = fps(0)
                // IMPROVE: Here we copy all points-to entries of the receiver into the *this*
                // of the target methods. It would be only needed to do so for the ones that led
                // to the call.
                val ptss = currentPointsToOfDefSites(fp, receiverOpt.get.asVar.definedBy)

                ptss.foreach { pts ⇒
                    // IMPROVE: add a method state.includeSharedPointsToSetWithFilter
                    // IMPROVE: use instantsmethods instead of the subtypeOf check
                    val possibleTypes = pts.types.filter(classHierarchy.isSubtypeOf(_, target.declaringClassType))
                    state.includeSharedPointsToSet(fp, pts, possibleTypes)
                }
            }

            // in case of signature polymorphic methods, we give up
            if (call.params.size == target.descriptor.parametersCount) {
                // handle params
                for (i ← 0 until target.descriptor.parametersCount) {
                    val fp = fps(i + 1)
                    state.includeSharedPointsToSets(
                        fp, currentPointsToOfDefSites(fp, call.params(i).asVar.definedBy)
                    )
                }
            } else {
                // it is not needed to mark it as incomplete here
            }
        } else {
            state.addIncompletePointsToInfo(pc)
        }
    }

    // todo: reduce code duplication
    private def handleIndirectCall(
        pc:      Int,
        target:  DeclaredMethod,
        callees: Callees,
        tac:     TACode[TACMethodParameter, DUVar[ValueInformation]]
    )(implicit state: State): Unit = {
        val fps = formalParameters(target)

        if (fps != null) {
            // handle receiver for non static methods
            val receiverOpt = callees.indirectCallReceiver(pc, target)
            if (receiverOpt.isDefined) {
                val fp = fps(0)
                val receiverDefSites = valueOriginsOfPCs(receiverOpt.get._2, tac.pcToIndex)
                // IMPROVE: Here we copy all points-to entries of the receiver into the *this*
                // of the target methods. It would be only needed to do so for the ones that led
                // to the call.
                val ptss = currentPointsToOfDefSites(fp, receiverDefSites)

                ptss.foreach { pts ⇒
                    // IMPROVE: add a method state.includeSharedPointsToSetWithFilter
                    // IMPROVE: use instantsmethods instead of the subtypeOf check
                    val possibleTypes = pts.types.filter(classHierarchy.isSubtypeOf(_, target.declaringClassType))
                    state.includeSharedPointsToSet(fp, pts, possibleTypes)
                }
            } else {
                // todo: distinguish between static methods and unavailable info
            }

            val indirectParams = callees.indirectCallParameters(pc, target)
            for (i ← 0 until target.descriptor.parametersCount) {
                val fp = fps(i + 1)
                val indirectParam = indirectParams(i)
                if (indirectParam.isDefined) {
                    state.includeSharedPointsToSets(
                        fp,
                        currentPointsToOfDefSites(
                            fp, valueOriginsOfPCs(indirectParam.get._2, tac.pcToIndex)
                        )
                    )
                } else {
                    state.addIncompletePointsToInfo(pc)
                }
            }
        } else {
            state.addIncompletePointsToInfo(pc)
        }
    }

    private[this] def returnResult(state: State): ProperPropertyComputationResult = {
        val results = ArrayBuffer.empty[ProperPropertyComputationResult]

        for ((e, pointsToSet) ← state.allocationSitePointsToSetsIterator) {
            results += Result(e, pointsToSet)
        }

        for ((e, pointsToSet) ← state.localPointsToSetsIterator) {
            if (state.hasDependees(e)) {
                val dependees = state.dependeesOf(e)
                results += InterimResult.forUB(
                    e,
                    pointsToSet,
                    dependees.values,
                    continuationForLocal(e, dependees, pointsToSet)
                )
            } else {
                results += Result(e, pointsToSet)
            }
        }

        for ((e, pointsToSet) ← state.sharedPointsToSetsIterator) {
            // The shared entities are not affected by changes of the tac and use partial results.
            // Thus, we could simply recompute them on updates for the tac
            if (state.hasDependees(e)) {
                val dependees = state.dependeesOf(e)
                results += InterimPartialResult(
                    dependees.values, continuationForShared(e, dependees)
                )
            }
            if (pointsToSet ne emptyPointsToSet) {
                results += PartialResult[Entity, PointsToSetLike[_, _, PointsToSet]](e, pointsToPropertyKey, {
                    case _: EPK[Entity, _] ⇒
                        Some(InterimEUBP(e, pointsToSet))

                    case UBP(ub: PointsToSet @unchecked) ⇒
                        val newPointsTo = ub.included(pointsToSet, 0)
                        if (newPointsTo ne ub) {
                            Some(InterimEUBP(e, newPointsTo))
                        } else {
                            None
                        }

                    case eOptP ⇒
                        throw new IllegalArgumentException(s"unexpected eOptP: $eOptP")
                })
            }
        }

        for (fakeEntity ← state.getFieldsIterator) {
            if (state.hasDependees(fakeEntity)) {
                val (defSite, field) = fakeEntity
                val dependees = state.dependeesOf(fakeEntity)
                assert(dependees.nonEmpty)
                results += InterimPartialResult(
                    dependees.values,
                    continuationForNewAllocationSitesAtGetField(defSite, field, dependees)
                )
            }
        }

        for (fakeEntity ← state.putFieldsIterator) {
            if (state.hasDependees(fakeEntity)) {
                val (defSites, field) = fakeEntity
                val defSitesWithoutExceptions = defSites.iterator.filterNot(ai.isImplicitOrExternalException)
                val defSitesEPKs = defSitesWithoutExceptions.map[EPK[Entity, Property]] { ds ⇒
                    val e = EPK(toEntity(ds, state.method, state.tac.stmts), pointsToPropertyKey)
                    // otherwise it might be the case that the property store does not know the epk
                    ps(e)
                    e
                }.toTraversable

                val dependees = state.dependeesOf(fakeEntity)
                assert(dependees.nonEmpty)
                if (defSitesEPKs.nonEmpty)
                    results += InterimPartialResult(
                        dependees.values,
                        continuationForNewAllocationSitesAtPutField(defSitesEPKs, field, dependees)
                    )
            }
        }

        if (state.hasCalleesDepenedee) {
            val calleesDependee = state.calleesDependee
            results += InterimPartialResult(
                Some(calleesDependee),
                continuationForCallees(
                    calleesDependee,
                    new PointsToAnalysisState[ElementType, PointsToSet](state.method, state.tacDependee)
                )
            )
        }

        Results(results)
    }

    private[this] def updatedDependees(
        eps: SomeEPS, oldDependees: Map[SomeEPK, SomeEOptionP]
    ): Map[SomeEPK, SomeEOptionP] = {
        val epk = eps.toEPK
        if (eps.isRefinable) {
            oldDependees + (epk → eps)
        } else {
            oldDependees - epk
        }
    }

    private[this] def updatedPointsToSet(
        oldPointsToSet:         PointsToSet,
        newDependeePointsToSet: PointsToSet,
        dependee:               SomeEPS,
        oldDependees:           Map[SomeEPK, SomeEOptionP]
    ): PointsToSet = {
        val oldDependeePointsTo = oldDependees(dependee.toEPK) match {
            case UBP(ub: PointsToSet @unchecked) ⇒ ub
            case _: EPK[_, PointsToSet]          ⇒ emptyPointsToSet
            case _                               ⇒ throw new IllegalArgumentException(s"unexpected dependee")
        }

        val newPointsToUB: PointsToSet = if (oldDependeePointsTo eq oldPointsToSet) {
            newDependeePointsToSet
        } else {
            val seenElements = oldDependeePointsTo.numElements
            oldPointsToSet.included(newDependeePointsToSet, seenElements)
        }
        newPointsToUB
    }

    private[this] def updatedPointsToSet(
        oldPointsToSet:         PointsToSet,
        newDependeePointsToSet: PointsToSet,
        dependee:               SomeEPS,
        oldDependees:           Map[SomeEPK, SomeEOptionP],
        allowedTypes:           UIDSet[ObjectType]
    ): PointsToSet = {
        val oldDependeePointsTo = oldDependees(dependee.toEPK) match {
            case UBP(ub: PointsToSet @unchecked) ⇒ ub
            case _: EPK[_, PointsToSet]          ⇒ emptyPointsToSet
            case _                               ⇒ throw new IllegalArgumentException(s"unexpected dependee")
        }

        val newPointsToUB: PointsToSet =
            oldPointsToSet.included(
                newDependeePointsToSet, oldDependeePointsTo.numElements, allowedTypes
            )

        newPointsToUB
    }

    private[this] def continuationForLocal(
        e: Entity, dependees: Map[SomeEPK, SomeEOptionP], pointsToSetUB: PointsToSet
    )(eps: SomeEPS): ProperPropertyComputationResult = {
        eps match {
            case UBP(newDependeePointsTo: PointsToSet @unchecked) ⇒
                val newDependees = updatedDependees(eps, dependees)
                val newPointsToUB = updatedPointsToSet(
                    pointsToSetUB,
                    newDependeePointsTo,
                    eps,
                    dependees
                )

                if (newDependees.isEmpty) {
                    Result(e, newPointsToUB)
                } else {
                    InterimResult.forUB(
                        e,
                        newPointsToUB,
                        newDependees.values,
                        continuationForLocal(e, newDependees, newPointsToUB)
                    )
                }
            case UBP(callees: Callees) ⇒
                // this will never happen for method return values
                val defSite = e.asInstanceOf[DefinitionSite]
                // TODO: Only handle new callees
                val results = ArrayBuffer.empty[ProperPropertyComputationResult]
                val tgts = callees.callees(defSite.pc)

                var newDependees = updatedDependees(eps, dependees)
                var newPointsToSet = pointsToSetUB
                tgts.foreach { target ⇒
                    // if we already have a dependency to that method, we do not need to process it
                    // otherwise, it might still be the case that we processed it before but it is
                    // final and thus not part of dependees anymore
                    if (!dependees.contains(EPK(target, pointsToPropertyKey))) {
                        val p2s = ps(target, pointsToPropertyKey)
                        if (p2s.isRefinable) {
                            newDependees += (p2s.toEPK → p2s)
                        }
                        newPointsToSet = newPointsToSet.included(pointsToUB(p2s))
                    }
                }

                if (newDependees.nonEmpty) {
                    results += InterimResult.forUB(
                        defSite,
                        newPointsToSet,
                        newDependees.values,
                        continuationForLocal(defSite, newDependees, newPointsToSet)
                    )
                } else {
                    results += Result(defSite, newPointsToSet)
                }

                Results(results)
            case _ ⇒ throw new IllegalArgumentException(s"unexpected update: $eps")
        }
    }

    private def getNumElements(eopt: SomeEOptionP): Int = {
        if (eopt.isEPK) 0
        else eopt.ub.asInstanceOf[PointsToSet].numElements
    }

    private[this] def continuationForNewAllocationSitesAtPutField(
        rhsDefSitesEPS: Traversable[SomeEPK], field: Field, dependees: Map[SomeEPK, SomeEOptionP]
    )(eps: SomeEPS): ProperPropertyComputationResult = {
        eps match {
            case UBP(newDependeePointsTo: PointsToSet @unchecked) ⇒
                val newDependees = updatedDependees(eps, dependees)
                var results: List[ProperPropertyComputationResult] = List.empty

                newDependeePointsTo.forNewestNElements(newDependeePointsTo.numElements - getNumElements(dependees(eps.toEPK))) { as ⇒
                    results ::= InterimPartialResult(
                        rhsDefSitesEPS, continuationForShared((as, field), rhsDefSitesEPS.toIterator.map(d ⇒ d → d).toMap)
                    )
                }
                if (newDependees.nonEmpty) {
                    results ::= InterimPartialResult(
                        newDependees.values,
                        continuationForNewAllocationSitesAtPutField(rhsDefSitesEPS, field, newDependees)
                    )
                }
                Results(
                    results
                )
        }
    }

    // todo name
    private[this] def continuationForNewAllocationSitesAtGetField(
        defSiteObject: DefinitionSite,
        field:         Field,
        dependees:     Map[SomeEPK, SomeEOptionP]
    )(eps: SomeEPS): ProperPropertyComputationResult = {
        eps match {
            case UBP(newDependeePointsTo: PointsToSet @unchecked) ⇒
                val newDependees = updatedDependees(eps, dependees)
                var nextDependees: List[SomeEPK] = Nil
                newDependeePointsTo.forNewestNElements(newDependeePointsTo.numElements - getNumElements(dependees(eps.toEPK))) { as ⇒
                    val epk = EPK((as, field), pointsToPropertyKey)
                    ps(epk)
                    nextDependees ::= epk
                }

                var results: List[ProperPropertyComputationResult] = Nil
                if (newDependees.nonEmpty) {
                    results ::= InterimPartialResult(
                        newDependees.values,
                        continuationForNewAllocationSitesAtGetField(defSiteObject, field, newDependees)
                    )
                }
                if (nextDependees.nonEmpty) {
                    results ::= InterimPartialResult(
                        nextDependees, continuationForShared(defSiteObject, nextDependees.iterator.map(d ⇒ d → d).toMap)
                    )
                }
                Results(results)
            case _ ⇒ throw new IllegalArgumentException(s"unexpected update: $eps")
        }
    }

    private[this] def continuationForShared(
        e: Entity, dependees: Map[SomeEPK, SomeEOptionP]
    )(eps: SomeEPS): ProperPropertyComputationResult = {
        eps match {
            case UBP(newDependeePointsTo: PointsToSet @unchecked) ⇒
                val newDependees = updatedDependees(eps, dependees)
                if (newDependeePointsTo ne emptyPointsToSet) {
                    val pr = PartialResult[Entity, PointsToSetLike[_, _, PointsToSet]](
                        e, pointsToPropertyKey, {
                        case UBP(ub: PointsToSet @unchecked) ⇒
                            val newPointsToSet = e match {
                                case VirtualFormalParameter(target, -1) ⇒
                                    val allowedTypes = newDependeePointsTo.types.filter(classHierarchy.isSubtypeOf(_, target.declaringClassType))
                                    updatedPointsToSet(
                                        ub,
                                        newDependeePointsTo,
                                        eps,
                                        dependees,
                                        allowedTypes
                                    )

                                case _ ⇒ updatedPointsToSet(
                                    ub,
                                    newDependeePointsTo,
                                    eps,
                                    dependees
                                )
                            }

                            if (newPointsToSet ne ub) {
                                Some(InterimEUBP(e, newPointsToSet))
                            } else {
                                None
                            }

                        case _: EPK[Entity, _] ⇒
                            e match {
                                case VirtualFormalParameter(target, -1) ⇒
                                    Some(InterimEUBP(
                                        e,
                                        newDependeePointsTo.filter(newDependeePointsTo.types.filter(classHierarchy.isSubtypeOf(_, target.declaringClassType)))
                                    ))
                                case _ ⇒
                                    Some(InterimEUBP(e, newDependeePointsTo))
                            }

                        case eOptP ⇒
                            throw new IllegalArgumentException(s"unexpected eOptP: $eOptP")
                    }
                    )

                    if (newDependees.nonEmpty) {
                        val ipr = InterimPartialResult(
                            newDependees.values, continuationForShared(e, newDependees)
                        )
                        Results(pr, ipr)
                    } else {
                        pr
                    }
                } else if (newDependees.nonEmpty) {
                    InterimPartialResult(
                        newDependees.values,
                        continuationForShared(e, newDependees)
                    )
                } else {
                    Results()
                }

            case _ ⇒ throw new IllegalArgumentException(s"unexpected update: $eps")
        }
    }

    def continuationForCallees(
        oldCalleeEOptP: EOptionP[DeclaredMethod, Callees],
        state:          State
    )(eps: SomeEPS): ProperPropertyComputationResult = {
        eps match {
            case UBP(newCallees: Callees) ⇒
                val tac = state.tac
                val oldCallees = if (oldCalleeEOptP.hasUBP) oldCalleeEOptP.ub else NoCallees
                for {
                    (pc, targets) ← newCallees.directCallSites()
                    target ← targets
                } {
                    if (!oldCallees.containsDirectCall(pc, target)) {
                        val call = tac.stmts(tac.pcToIndex(pc)) match {
                            case call: Call[DUVar[ValueInformation]] @unchecked ⇒
                                call
                            case Assignment(_, _, call: Call[DUVar[ValueInformation]] @unchecked) ⇒
                                call
                            case ExprStmt(_, call: Call[DUVar[ValueInformation]] @unchecked) ⇒
                                call
                            case e ⇒ throw new IllegalArgumentException(s"unexpected stmt $e")
                        }
                        handleDirectCall(call, pc, target)(state)
                    }
                }
                for {
                    (pc, targets) ← newCallees.indirectCallSites()
                    target ← targets
                } {
                    if (!oldCallees.containsIndirectCall(pc, target)) {
                        handleIndirectCall(pc, target, newCallees, tac)(state)
                    }
                }

                state.setCalleesDependee(eps.asInstanceOf[EPS[DeclaredMethod, Callees]])

                returnResult(state)
            case _ ⇒ throw new IllegalArgumentException(s"unexpected eps $eps")
        }
    }

    @inline private[this] def currentPointsTo(
        depender: Entity, dependee: Entity
    )(implicit state: State): PointsToSet = {
        dependee match {
            case ds: DefinitionSite if state.hasAllocationSitePointsToSet(ds) ⇒
                state.allocationSitePointsToSet(ds)

            case ds: DefinitionSite if state.hasLocalPointsToSet(ds) ⇒
                if (!state.hasDependency(depender, EPK(dependee, pointsToPropertyKey))) {
                    val p2s = propertyStore(dependee, pointsToPropertyKey)
                    assert(p2s.isEPK)
                    state.addDependee(depender, p2s)
                }

                /*
                // TODO: It might be even more efficient to forward the dependencies.
                // However, this requires the continuation functions to handle Callees
                if (state.hasDependees(ds))
                    state.plainDependeesOf(ds).foreach(state.addDependee(depender, _))
                 */

                state.localPointsToSet(ds)
            case _ ⇒
                val epk = EPK(dependee, pointsToPropertyKey)
                val p2s = if (state.hasDependency(depender, epk)) {
                    // IMPROVE: add a method to the state
                    state.dependeesOf(depender)(epk)
                } else {
                    val p2s = propertyStore(dependee, pointsToPropertyKey)
                    if (p2s.isRefinable) {
                        state.addDependee(depender, p2s)
                    }
                    p2s
                }
                pointsToUB(p2s.asInstanceOf[EOptionP[Entity, PointsToSet]])
        }
    }

    @inline private[this] def currentPointsToOfDefSites(
        depender: Entity,
        defSites: IntTrieSet
    )(
        implicit
        state: State
    ): Iterator[PointsToSet] = {
        defSites.iterator.map[PointsToSet](currentPointsToDefSite(depender, _))
    }

    @inline private[this] def currentPointsToDefSite(
        depender: Entity, dependeeDefSite: Int
    )(implicit state: State): PointsToSet = {
        if (ai.isMethodExternalExceptionOrigin(dependeeDefSite)) {
            // FIXME ask what exception has been thrown
            emptyPointsToSet
        } else if (ai.isImmediateVMException(dependeeDefSite)) {
            // FIXME -  we need to get the actual exception type here
            emptyPointsToSet
        } else {
            currentPointsTo(depender, toEntity(dependeeDefSite, state.method, state.tac.stmts))
        }
    }

    @inline private[this] def pointsToUB(eOptP: EOptionP[Entity, PointsToSet]): PointsToSet = {
        if (eOptP.hasUBP)
            eOptP.ub
        else
            emptyPointsToSet
    }
}

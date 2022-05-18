/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package pointsto

import scala.collection.mutable.ArrayBuffer
import org.opalj.collection.immutable.IntTrieSet
import org.opalj.fpcf.Entity
import org.opalj.fpcf.EOptionP
import org.opalj.fpcf.EPK
import org.opalj.fpcf.InterimEUBP
import org.opalj.fpcf.InterimPartialResult
import org.opalj.fpcf.PartialResult
import org.opalj.fpcf.ProperPropertyComputationResult
import org.opalj.fpcf.Property
import org.opalj.fpcf.Result
import org.opalj.fpcf.Results
import org.opalj.fpcf.SomeEOptionP
import org.opalj.fpcf.SomeEPK
import org.opalj.fpcf.SomeEPS
import org.opalj.fpcf.UBP
import org.opalj.br.ArrayType
import org.opalj.br.Field
import org.opalj.br.ReferenceType
import org.opalj.br.fpcf.properties.pointsto.PointsToSetLike
import org.opalj.br.ObjectType
import org.opalj.br.analyses.VirtualFormalParameter
import org.opalj.br.fpcf.properties.Context
import org.opalj.tac.fpcf.analyses.cg.SimpleContextProvider
import org.opalj.tac.fpcf.analyses.cg.TypeConsumerAnalysis

import scala.collection.immutable.ArraySeq

/**
 * Base class for handling instructions in points-to analysis scenarios.
 *
 * @author Dominik Helm
 * @author Florian Kuebler
 */
trait PointsToAnalysisBase extends AbstractPointsToBasedAnalysis with TypeConsumerAnalysis {

    override protected[this] type State = PointsToAnalysisState[ElementType, PointsToSet, ContextType]
    override protected[this] type DependerType = Entity
    @inline protected[this] def currentPointsToOfDefSites(
        depender:   DependerType,
        defSites:   IntTrieSet,
        typeFilter: ReferenceType => Boolean = PointsToSetLike.noFilter
    )(implicit state: State): Iterator[PointsToSet] = {
        defSites.iterator.map[PointsToSet](currentPointsToOfDefSite(depender, _, typeFilter))
    }

    @inline protected[this] def currentPointsToOfDefSite(
        depender:        DependerType,
        dependeeDefSite: Int,
        typeFilter:      ReferenceType => Boolean = PointsToSetLike.noFilter
    )(implicit state: State): PointsToSet = {
        if (ai.isImmediateVMException(dependeeDefSite)) {
            // FIXME -  we need to get the actual exception type here
            createPointsToSet(
                state.tac.stmts(ai.pcOfImmediateVMException(dependeeDefSite)).pc,
                state.callContext,
                ObjectType.Throwable,
                isConstant = false
            )
        } else {
            // IMPROVE can we get points to sets of local allocation sites here directly?
            // If they are not yet set, could we still do better than registering a dependency?
            currentPointsTo(depender, toEntity(dependeeDefSite), typeFilter)
        }
    }

    @inline protected[this] def toEntity(defSite: Int)(implicit state: State): Entity = {
        pointsto.toEntity(defSite, state.callContext, state.tac.stmts)
    }

    @inline protected[this] def getDefSite(pc: Int)(implicit state: State): Entity = {
        val defSite = definitionSites(state.callContext.method.definedMethod, pc)
        typeProvider match {
            case _: SimpleContextProvider => defSite
            case _                        => (state.callContext, defSite)
        }
    }

    @inline protected[this] def getFormalParameter(
        index: Int, formalParameters: ArraySeq[VirtualFormalParameter], context: Context
    ): Entity = {
        val fp = formalParameters(index)
        typeProvider match {
            case _: SimpleContextProvider => fp
            case _                        => (context, fp)
        }
    }

    protected[this] def handleCallReceiver(
        receiverDefSites:             IntTrieSet,
        target:                       Context,
        isNonVirtualCall:             Boolean,
        indirectConstructorPCAndType: Option[(Int, ReferenceType)] = None
    )(implicit state: State): Unit = {
        val targetMethod = target.method
        val fps = formalParameters(targetMethod)
        val declClassType = targetMethod.declaringClassType
        val tgtMethod = targetMethod.definedMethod
        val filter = if (isNonVirtualCall) {
            t: ReferenceType => classHierarchy.isSubtypeOf(t, declClassType)
        } else {
            val overrides =
                if (project.overridingMethods.contains(tgtMethod))
                    project.overridingMethods(tgtMethod).map(_.classFile.thisType) -
                        declClassType
                else
                    Set.empty
            // TODO this might not be 100% correct in some corner cases
            t: ReferenceType =>
                classHierarchy.isSubtypeOf(t, declClassType) &&
                    !overrides.exists(st => classHierarchy.isSubtypeOf(t, st))
        }
        val fp = getFormalParameter(0, fps, target)
        val ptss =
            if (indirectConstructorPCAndType.isDefined)
                Iterator(
                    createPointsToSet(
                        indirectConstructorPCAndType.get._1,
                        state.callContext,
                        indirectConstructorPCAndType.get._2,
                        isConstant = false
                    )
                )
            else currentPointsToOfDefSites(fp, receiverDefSites, filter)
        state.includeSharedPointsToSets(
            fp,
            ptss,
            filter
        )
    }

    protected[this] def handleCallParameter(
        paramDefSites: IntTrieSet,
        paramIndex:    Int,
        target:        Context
    )(implicit state: State): Unit = {
        val fps = formalParameters(target.method)
        val paramType = target.method.descriptor.parameterType(paramIndex)
        if (paramType.isReferenceType) {
            val fp = getFormalParameter(paramIndex + 1, fps, target)
            val filter = { t: ReferenceType =>
                classHierarchy.isSubtypeOf(t, paramType.asReferenceType)
            }
            state.includeSharedPointsToSets(
                fp,
                currentPointsToOfDefSites(fp, paramDefSites, filter),
                filter
            )
        }
    }

    private[this] def getFilter(
        pc: Int, checkForCast: Boolean
    )(implicit state: State): ReferenceType => Boolean = {
        if (checkForCast) {
            val tac = state.tac
            val index = tac.properStmtIndexForPC(pc)
            val nextStmt = tac.stmts(index + 1)
            nextStmt match {
                case Checkcast(_, value, cmpTpe) if value.asVar.definedBy.contains(index) =>
                    t: ReferenceType => classHierarchy.isSubtypeOf(t, cmpTpe)
                case _ =>
                    PointsToSetLike.noFilter
            }
        } else {
            PointsToSetLike.noFilter
        }
    }

    protected[this] def handleGetField(
        fieldOpt: Option[Field], pc: Int, objRefDefSites: IntTrieSet, checkForCast: Boolean = true
    )(implicit state: State): Unit = {
        val filter = getFilter(pc, checkForCast)
        val defSiteObject = getDefSite(pc)
        val fakeEntity = (defSiteObject, fieldOpt, filter)
        state.addGetFieldEntity(fakeEntity)
        state.includeSharedPointsToSet(defSiteObject, emptyPointsToSet, PointsToSetLike.noFilter)
        currentPointsToOfDefSites(fakeEntity, objRefDefSites).foreach { pts =>
            pts.forNewestNElements(pts.numElements) { as =>
                val tpe = getTypeOf(as)
                if (tpe.isObjectType && (fieldOpt.isEmpty ||
                    classHierarchy.isSubtypeOf(tpe, fieldOpt.get.classFile.thisType))) {
                    val fieldEntities =
                        if (fieldOpt.isDefined) Iterator((as, fieldOpt.get))
                        else project.classHierarchy.allSuperclassesIterator(tpe.asObjectType, true).flatMap(_.fields.iterator).map((as, _))
                    for (fieldEntity <- fieldEntities)
                        state.includeSharedPointsToSet(
                            defSiteObject,
                            // IMPROVE: Use LongRefPair to avoid boxing
                            currentPointsTo(defSiteObject, fieldEntity, filter),
                            filter
                        )
                }
            }
        }
    }

    protected[this] def handleGetStatic(
        field: Field, pc: Int, checkForCast: Boolean = true
    )(implicit state: State): Unit = {
        val filter = getFilter(pc, checkForCast)
        val defSiteObject = getDefSite(pc)
        state.includeSharedPointsToSet(
            defSiteObject,
            currentPointsTo(defSiteObject, field, filter),
            filter
        )
    }

    protected[this] def handleArrayLoad(
        arrayType: ArrayType, pc: Int, arrayDefSites: IntTrieSet, checkForCast: Boolean = true
    )(implicit state: State): Unit = {
        val filter = getFilter(pc, checkForCast)
        val defSiteObject = getDefSite(pc)
        val fakeEntity = (defSiteObject, arrayType, filter)
        state.addArrayLoadEntity(fakeEntity)
        state.includeSharedPointsToSet(defSiteObject, emptyPointsToSet, PointsToSetLike.noFilter)
        currentPointsToOfDefSites(fakeEntity, arrayDefSites).foreach { pts =>
            pts.forNewestNElements(pts.numElements) { as =>
                val typeId = getTypeIdOf(as)
                if (typeId < 0 &&
                    classHierarchy.isSubtypeOf(ArrayType.lookup(typeId), arrayType)) {
                    state.includeSharedPointsToSet(
                        defSiteObject,
                        currentPointsTo(defSiteObject, ArrayEntity(as), filter),
                        filter
                    )
                }
            }
        }
    }

    protected[this] def handlePutField(
        fieldOpt: Option[Field], objRefDefSites: IntTrieSet, rhsDefSites: IntTrieSet
    )(implicit state: State): Unit = {
        val fakeEntity = (rhsDefSites, fieldOpt)
        state.addPutFieldEntity(fakeEntity)

        val filter = if (fieldOpt.isDefined) { t: ReferenceType =>
            classHierarchy.isSubtypeOf(t, fieldOpt.get.fieldType.asReferenceType)
        } else
            PointsToSetLike.noFilter

        currentPointsToOfDefSites(fakeEntity, objRefDefSites).foreach { pts =>
            pts.forNewestNElements(pts.numElements) { as =>
                val tpe = getTypeOf(as)
                if (tpe.isObjectType && (fieldOpt.isEmpty ||
                    classHierarchy.isSubtypeOf(tpe, fieldOpt.get.classFile.thisType))) {
                    val fieldEntities =
                        if (fieldOpt.isDefined) Iterator((as, fieldOpt.get))
                        else project.classHierarchy.allSuperclassesIterator(tpe.asObjectType, true).flatMap(_.fields.iterator).map((as, _))
                    for (fieldEntity <- fieldEntities)
                        state.includeSharedPointsToSets(
                            fieldEntity,
                            currentPointsToOfDefSites(fieldEntity, rhsDefSites, filter),
                            filter
                        )
                }
            }
        }
    }

    protected[this] def handlePutStatic(field: Field, rhsDefSites: IntTrieSet)(implicit state: State): Unit = {
        val filter = { t: ReferenceType =>
            classHierarchy.isSubtypeOf(t, field.fieldType.asReferenceType)
        }
        state.includeSharedPointsToSets(
            field,
            currentPointsToOfDefSites(field, rhsDefSites, filter),
            filter
        )
    }

    protected[this] def handleArrayStore(
        arrayType: ArrayType, arrayDefSites: IntTrieSet, rhsDefSites: IntTrieSet
    )(implicit state: State): Unit = {
        val fakeEntity = (rhsDefSites, arrayType)
        state.addArrayStoreEntity(fakeEntity)
        currentPointsToOfDefSites(fakeEntity, arrayDefSites).foreach { pts =>
            pts.forNewestNElements(pts.numElements) { as =>
                val typeId = getTypeIdOf(as)
                if (typeId < 0 &&
                    classHierarchy.isSubtypeOf(ArrayType.lookup(typeId), arrayType) &&
                    !isEmptyArray(as)) {
                    val arrayEntity = ArrayEntity(as)
                    val componentType = ArrayType.lookup(typeId).componentType.asReferenceType
                    val filter = { t: ReferenceType =>
                        classHierarchy.isSubtypeOf(t, componentType)
                    }
                    state.includeSharedPointsToSets(
                        arrayEntity,
                        currentPointsToOfDefSites(arrayEntity, rhsDefSites, filter),
                        filter
                    )
                }
            }
        }
    }

    @inline protected[this] def currentPointsTo(
        depender: Entity, dependee: Entity, typeFilter: ReferenceType => Boolean
    )(implicit state: State): PointsToSet = {
        val epk = EPK(dependee, pointsToPropertyKey)

        val p2s = if (state.hasDependee(epk)) state.getProperty(epk) else propertyStore(epk)

        if (p2s.isRefinable && !state.hasDependency(depender, epk)) {
            state.addDependee(depender, p2s, typeFilter)
        }

        pointsToUB(p2s.asInstanceOf[EOptionP[Entity, PointsToSet]])
    }

    @inline protected[this] def updatedDependees(
        eps: SomeEPS, oldDependees: Map[SomeEPK, (SomeEOptionP, ReferenceType => Boolean)]
    ): Map[SomeEPK, (SomeEOptionP, ReferenceType => Boolean)] = {
        val epk = eps.toEPK
        val typeFilter = oldDependees(epk)._2
        if (eps.isRefinable) {
            oldDependees + (epk -> ((eps, typeFilter)))
        } else {
            oldDependees - epk
        }
    }

    @inline protected[this] def updatedPointsToSet(
        oldPointsToSet:         PointsToSet,
        newDependeePointsToSet: PointsToSet,
        dependee:               SomeEPS,
        oldDependees:           Map[SomeEPK, (SomeEOptionP, ReferenceType => Boolean)]
    ): PointsToSet = {
        val (oldDependee, typeFilter) = oldDependees(dependee.toEPK)
        val oldDependeePointsTo = oldDependee match {
            case UBP(ub: PointsToSet @unchecked)   => ub
            case _: EPK[_, PointsToSet @unchecked] => emptyPointsToSet
            case d =>
                throw new IllegalArgumentException(s"unexpected dependee $d")
        }

        oldPointsToSet.included(
            newDependeePointsToSet,
            oldDependeePointsTo.numElements,
            typeFilter
        )
    }

    @inline private[this] def getNumElements(eopt: SomeEOptionP): Int = {
        if (eopt.isEPK) 0
        else eopt.ub.asInstanceOf[PointsToSet].numElements
    }

    protected[this] def continuationForNewAllocationSitesAtPutField(
        knownPointsTo:  PointsToSet,
        rhsDefSitesEPS: Map[SomeEPK, SomeEOptionP],
        fieldOpt:       Option[Field],
        dependees:      Map[SomeEPK, (SomeEOptionP, ReferenceType => Boolean)],
        state:          State
    )(eps: SomeEPS): ProperPropertyComputationResult = {
        (eps: @unchecked) match {
            case UBP(newDependeePointsTo: PointsToSet @unchecked) =>
                val newDependees = updatedDependees(eps, dependees)
                var results: List[ProperPropertyComputationResult] = List.empty

                newDependeePointsTo.forNewestNElements(newDependeePointsTo.numElements - getNumElements(dependees(eps.toEPK)._1)) { as =>
                    val tpe = getTypeOf(as)
                    if (tpe.isObjectType && (fieldOpt.isEmpty ||
                        classHierarchy.isSubtypeOf(tpe, fieldOpt.get.classFile.thisType))) {

                        val typeFilter = if (fieldOpt.isDefined) { t: ReferenceType =>
                            classHierarchy.isSubtypeOf(t, fieldOpt.get.fieldType.asReferenceType)
                        } else
                            PointsToSetLike.noFilter

                        val fieldEntities =
                            if (fieldOpt.isDefined) Iterator((as, fieldOpt.get))
                            else project.classHierarchy.allSuperclassesIterator(tpe.asObjectType, true).flatMap(_.fields.iterator).map((as, _))
                        for (fieldEntity <- fieldEntities)
                            results = results ++ createPartialResults(
                                fieldEntity,
                                knownPointsTo,
                                rhsDefSitesEPS.view.mapValues((_, typeFilter)).toMap,
                                { _.included(knownPointsTo, typeFilter) }
                            )(state)
                    }
                }
                if (newDependees.nonEmpty) {
                    results ::= InterimPartialResult(
                        newDependees.valuesIterator.map(_._1).toSet,
                        continuationForNewAllocationSitesAtPutField(
                            knownPointsTo, rhsDefSitesEPS, fieldOpt, newDependees, state
                        )
                    )
                }
                Results(
                    results
                )
        }
    }

    protected[this] def continuationForNewAllocationSitesAtArrayStore(
        knownPointsTo:  PointsToSet,
        rhsDefSitesEPS: Map[SomeEPK, SomeEOptionP],
        arrayType:      ArrayType,
        dependees:      Map[SomeEPK, (SomeEOptionP, ReferenceType => Boolean)],
        state:          State
    )(eps: SomeEPS): ProperPropertyComputationResult = {
        (eps: @unchecked) match {
            case UBP(newDependeePointsTo: PointsToSet @unchecked) =>
                val newDependees = updatedDependees(eps, dependees)
                var results: List[ProperPropertyComputationResult] = List.empty
                newDependeePointsTo.forNewestNElements(newDependeePointsTo.numElements - getNumElements(dependees(eps.toEPK)._1)) { as =>
                    val typeId = getTypeIdOf(as)
                    if (typeId < 0 &&
                        classHierarchy.isSubtypeOf(ArrayType.lookup(typeId), arrayType) &&
                        !isEmptyArray(as)) {
                        val componentType = ArrayType.lookup(typeId).componentType.asReferenceType
                        val typeFilter = { t: ReferenceType =>
                            classHierarchy.isSubtypeOf(t, componentType)
                        }
                        results = results ++ createPartialResults(
                            ArrayEntity(as),
                            knownPointsTo,
                            rhsDefSitesEPS.view.mapValues((_, typeFilter)).toMap,
                            { _.included(knownPointsTo, typeFilter) }
                        )(state)
                    }
                }
                if (newDependees.nonEmpty) {
                    results ::= InterimPartialResult(
                        newDependees.valuesIterator.map(_._1).toSet,
                        continuationForNewAllocationSitesAtArrayStore(
                            knownPointsTo, rhsDefSitesEPS, arrayType, newDependees, state
                        )
                    )
                }
                Results(
                    results
                )
        }
    }

    // todo name
    protected[this] def continuationForNewAllocationSitesAtGetField(
        defSiteObject: Entity,
        fieldOpt:      Option[Field],
        filter:        ReferenceType => Boolean,
        dependees:     Map[SomeEPK, (SomeEOptionP, ReferenceType => Boolean)],
        state:         State
    )(eps: SomeEPS): ProperPropertyComputationResult = {
        eps match {
            case UBP(newDependeePointsTo: PointsToSet @unchecked) =>
                val newDependees = updatedDependees(eps, dependees)
                var nextDependees: List[SomeEOptionP] = Nil
                var newPointsTo = emptyPointsToSet
                newDependeePointsTo.forNewestNElements(newDependeePointsTo.numElements - getNumElements(dependees(eps.toEPK)._1)) { as =>
                    val tpe = getTypeOf(as)
                    if (tpe.isObjectType && (fieldOpt.isEmpty ||
                        classHierarchy.isSubtypeOf(tpe, fieldOpt.get.classFile.thisType))) {
                        val fieldEntities =
                            if (fieldOpt.isDefined) Iterator((as, fieldOpt.get))
                            else project.classHierarchy.allSuperclassesIterator(tpe.asObjectType, true).flatMap(_.fields.iterator).map((as, _))
                        for (fieldEntity <- fieldEntities) {
                            val fieldEntries = ps(fieldEntity, pointsToPropertyKey)
                            newPointsTo = newPointsTo.included(pointsToUB(fieldEntries), filter)
                            if (fieldEntries.isRefinable)
                                nextDependees ::= fieldEntries
                        }
                    }
                }

                var results: Seq[ProperPropertyComputationResult] = createPartialResults(
                    defSiteObject,
                    newPointsTo,
                    nextDependees.iterator.map(d => d.toEPK -> ((d, filter))).toMap,
                    { _.included(newPointsTo, filter) }
                )(state)

                if (newDependees.nonEmpty) {
                    results +:= InterimPartialResult(
                        newDependees.valuesIterator.map(_._1).toSet,
                        continuationForNewAllocationSitesAtGetField(
                            defSiteObject, fieldOpt, filter, newDependees, state
                        )
                    )
                }

                Results(results)
            case _ => throw new IllegalArgumentException(s"unexpected update: $eps")
        }
    }

    // todo name
    protected[this] def continuationForNewAllocationSitesAtArrayLoad(
        defSiteObject: Entity,
        arrayType:     ArrayType,
        filter:        ReferenceType => Boolean,
        dependees:     Map[SomeEPK, (SomeEOptionP, ReferenceType => Boolean)],
        state:         State
    )(eps: SomeEPS): ProperPropertyComputationResult = {
        eps match {
            case UBP(newDependeePointsTo: PointsToSet @unchecked) =>
                val newDependees = updatedDependees(eps, dependees)
                var nextDependees: List[SomeEOptionP] = Nil
                var newPointsTo = emptyPointsToSet
                newDependeePointsTo.forNewestNElements(newDependeePointsTo.numElements - getNumElements(dependees(eps.toEPK)._1)) { as =>
                    val typeId = getTypeIdOf(as)
                    if (typeId < 0 && classHierarchy.isSubtypeOf(ArrayType.lookup(typeId), arrayType)) {
                        val arrayEntries = ps(ArrayEntity(as), pointsToPropertyKey)
                        newPointsTo = newPointsTo.included(pointsToUB(arrayEntries), filter)
                        if (arrayEntries.isRefinable)
                            nextDependees ::= arrayEntries
                    }
                }

                var results: Seq[ProperPropertyComputationResult] =
                    createPartialResults(
                        defSiteObject,
                        newPointsTo,
                        nextDependees.iterator.map(d => d.toEPK -> ((d, filter))).toMap,
                        { _.included(newPointsTo, filter) }
                    )(state)

                if (newDependees.nonEmpty) {
                    results +:= InterimPartialResult(
                        newDependees.valuesIterator.map(_._1).toSet,
                        continuationForNewAllocationSitesAtArrayLoad(
                            defSiteObject, arrayType, filter, newDependees, state
                        )
                    )
                }

                Results(results)
            case _ => throw new IllegalArgumentException(s"unexpected update: $eps")
        }
    }

    protected[this] def continuationForShared(
        e: Entity, dependees: Map[SomeEPK, (SomeEOptionP, ReferenceType => Boolean)], state: State
    )(eps: SomeEPS): ProperPropertyComputationResult = {
        eps match {
            case UBP(newDependeePointsTo: PointsToSet @unchecked) =>
                val newDependees = updatedDependees(eps, dependees)

                val results = createPartialResults(
                    e,
                    newDependeePointsTo,
                    newDependees,
                    { old =>
                        updatedPointsToSet(
                            old,
                            newDependeePointsTo,
                            eps,
                            dependees
                        )
                    },
                    true
                )(state)

                Results(results)

            case _ => throw new IllegalArgumentException(s"unexpected update: $eps")
        }
    }

    @inline protected[this] def createPartialResults(
        e:              Entity,
        newPointsToSet: PointsToSet,
        newDependees:   Map[SomeEPK, (SomeEOptionP, ReferenceType => Boolean)],
        updatePointsTo: PointsToSet => PointsToSet,
        isUpdate:       Boolean                                                = false
    )(implicit state: State): Seq[ProperPropertyComputationResult] = {
        var results: Seq[ProperPropertyComputationResult] = Seq.empty

        if (newDependees.nonEmpty) {
            results +:= InterimPartialResult(
                newDependees.valuesIterator.map(_._1).toSet,
                continuationForShared(e, newDependees, state)
            )
        }

        if (!isUpdate || (newPointsToSet ne emptyPointsToSet)) {
            results +:= PartialResult[Entity, PointsToSetLike[_, _, PointsToSet]](
                e,
                pointsToPropertyKey,
                (eoptp: EOptionP[Entity, PointsToSetLike[_, _, PointsToSet]]) => eoptp match {
                    case UBP(ub: PointsToSet @unchecked) =>
                        val newPointsToSet = updatePointsTo(ub)
                        if (newPointsToSet ne ub) {
                            Some(InterimEUBP(e, newPointsToSet))
                        } else {
                            None
                        }

                    case _: EPK[Entity, _] =>
                        val newPointsToSet = updatePointsTo(emptyPointsToSet)
                        if (isUpdate && (newPointsToSet eq emptyPointsToSet))
                            None
                        else
                            Some(InterimEUBP(e, newPointsToSet))

                    case eOptP =>
                        throw new IllegalArgumentException(s"unexpected eOptP: $eOptP")
                }
            )
        }

        results
    }

    protected[this] def createResults(
        implicit
        state: State
    ): ArrayBuffer[ProperPropertyComputationResult] = {
        val results = ArrayBuffer.empty[ProperPropertyComputationResult]

        for ((e, pointsToSet) <- state.allocationSitePointsToSetsIterator) {
            results += Result(e, pointsToSet)
        }

        for ((e, pointsToSet) <- state.sharedPointsToSetsIterator) {
            results ++= createPartialResults(
                e,
                pointsToSet,
                if (state.hasDependees(e)) state.dependeesOf(e) else Map.empty,
                { _.included(pointsToSet) }
            )
        }

        for (fakeEntity <- state.getFieldsIterator) {
            if (state.hasDependees(fakeEntity)) {
                val (defSite, fieldOpt, filter) = fakeEntity
                val dependees = state.dependeesOf(fakeEntity)
                results += InterimPartialResult(
                    dependees.valuesIterator.map(_._1).toSet,
                    continuationForNewAllocationSitesAtGetField(
                        defSite, fieldOpt, filter, dependees, state
                    )
                )
            }
        }

        for (fakeEntity <- state.putFieldsIterator) {
            if (state.hasDependees(fakeEntity)) {
                val (defSites, fieldOpt) = fakeEntity
                val defSitesWithoutExceptions =
                    defSites.iterator.filterNot(ai.isImmediateVMException)
                var knownPointsTo = emptyPointsToSet
                val defSitesEPSs =
                    defSitesWithoutExceptions.map[(EPK[Entity, Property], EOptionP[Entity, Property])] { ds =>
                        val defSiteEntity = toEntity(ds)(state)
                        val rhsPTS = ps(defSiteEntity, pointsToPropertyKey)
                        knownPointsTo = knownPointsTo.included(pointsToUB(rhsPTS))
                        rhsPTS.toEPK -> rhsPTS
                    }.filter(_._2.isRefinable).toMap

                val dependees = state.dependeesOf(fakeEntity)
                if (defSitesEPSs.nonEmpty || (knownPointsTo ne emptyPointsToSet))
                    results += InterimPartialResult(
                        dependees.valuesIterator.map(_._1).toSet,
                        continuationForNewAllocationSitesAtPutField(
                            knownPointsTo, defSitesEPSs, fieldOpt, dependees, state
                        )
                    )
            }
        }

        for (fakeEntity <- state.arrayLoadsIterator) {
            if (state.hasDependees(fakeEntity)) {
                val (defSite, arrayType, filter) = fakeEntity
                val dependees = state.dependeesOf(fakeEntity)
                results += InterimPartialResult(
                    dependees.valuesIterator.map(_._1).toSet,
                    continuationForNewAllocationSitesAtArrayLoad(
                        defSite, arrayType, filter, dependees, state
                    )
                )
            }
        }

        for (fakeEntity <- state.arrayStoresIterator) {
            if (state.hasDependees(fakeEntity)) {
                val (defSites, arrayType) = fakeEntity
                val defSitesWithoutExceptions =
                    defSites.iterator.filterNot(ai.isImmediateVMException)
                var knownPointsTo = emptyPointsToSet
                val defSitesEPSs =
                    defSitesWithoutExceptions.map[(EPK[Entity, Property], EOptionP[Entity, Property])] { ds =>
                        val defSiteEntity = toEntity(ds)(state)
                        val rhsPTS = ps(defSiteEntity, pointsToPropertyKey)
                        knownPointsTo = knownPointsTo.included(pointsToUB(rhsPTS))
                        rhsPTS.toEPK -> rhsPTS
                    }.filter(_._2.isRefinable).toMap

                val dependees = state.dependeesOf(fakeEntity)
                if (defSitesEPSs.nonEmpty || (knownPointsTo ne emptyPointsToSet))
                    results += InterimPartialResult(
                        dependees.valuesIterator.map(_._1).toSet,
                        continuationForNewAllocationSitesAtArrayStore(
                            knownPointsTo, defSitesEPSs, arrayType, dependees, state
                        )
                    )
            }
        }

        results
    }
}

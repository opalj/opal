/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package pointsto

import org.opalj.collection.immutable.IntTrieSet
import org.opalj.fpcf.Entity
import org.opalj.fpcf.EOptionP
import org.opalj.fpcf.EPK
import org.opalj.fpcf.InterimEUBP
import org.opalj.fpcf.InterimPartialResult
import org.opalj.fpcf.PartialResult
import org.opalj.fpcf.ProperPropertyComputationResult
import org.opalj.fpcf.Results
import org.opalj.fpcf.SomeEOptionP
import org.opalj.fpcf.SomeEPK
import org.opalj.fpcf.SomeEPS
import org.opalj.fpcf.UBP
import org.opalj.br.ArrayType
import org.opalj.br.fpcf.properties.pointsto.allocationSiteLongToTypeId
import org.opalj.br.Field
import org.opalj.br.ReferenceType
import org.opalj.br.fpcf.properties.pointsto.isEmptyArrayAllocationSite
import org.opalj.br.fpcf.properties.pointsto.PointsToSetLike
import org.opalj.tac.common.DefinitionSite

trait PointsToAnalysisBase extends AbstractPointsToBasedAnalysis {

    override type State = PointsToAnalysisState[ElementType, PointsToSet]
    override type DependerType = Entity

    protected[this] def handleGetField(
        field: AField, pc: Int, objRefDefSites: IntTrieSet
    )(implicit state: State): Unit = {
        val tac = state.tac
        val index = tac.pcToIndex(pc)
        val nextStmt = tac.stmts(index + 1)
        val filter = nextStmt match {
            case Checkcast(_, value, cmpTpe) if value.asVar.definedBy.contains(index) ⇒
                t: ReferenceType ⇒ classHierarchy.isSubtypeOf(t, cmpTpe)
            case _ ⇒
                PointsToSetLike.noFilter
        }
        val defSiteObject = definitionSites(state.method.definedMethod, pc)
        val fakeEntity = (defSiteObject, field, filter)
        state.addGetFieldEntity(fakeEntity)
        currentPointsToOfDefSites(fakeEntity, objRefDefSites).foreach { pts ⇒
            pts.forNewestNElements(pts.numElements) { as ⇒
                // TODO: Refactor
                val fieldClassType = field.classType
                val asTypeId = allocationSiteLongToTypeId(as.asInstanceOf[Long])
                if (fieldClassType.id == asTypeId || classHierarchy.subtypeInformation(fieldClassType).get.containsId(asTypeId)) {
                    state.includeSharedPointsToSet(
                        defSiteObject,
                        // IMPROVE: Use LongRefPair to avoid boxing
                        currentPointsTo(defSiteObject, (as, field), filter),
                        filter
                    )
                }
            }
        }
    }

    protected[this] def handleGetStatic(field: Field, pc: Int)(implicit state: State): Unit = {
        val defSiteObject = definitionSites(state.method.definedMethod, pc)
        val tac = state.tac
        val index = tac.pcToIndex(pc)
        val nextStmt = tac.stmts(index + 1)
        val filter = nextStmt match {
            case Checkcast(_, value, cmpTpe) if value.asVar.definedBy.contains(index) ⇒
                t: ReferenceType ⇒ classHierarchy.isSubtypeOf(t, cmpTpe)
            case _ ⇒
                PointsToSetLike.noFilter
        }
        state.includeSharedPointsToSet(
            defSiteObject,
            currentPointsTo(defSiteObject, field, filter),
            filter
        )
    }

    protected[this] def handleArrayLoad(
        arrayType: ArrayType, pc: Int, arrayDefSites: IntTrieSet
    )(implicit state: State): Unit = {
        val defSiteObject = definitionSites(state.method.definedMethod, pc)
        val tac = state.tac
        val index = tac.pcToIndex(pc)
        val nextStmt = tac.stmts(index + 1)
        val filter = nextStmt match {
            case Checkcast(_, value, cmpTpe) if value.asVar.definedBy.contains(index) ⇒
                t: ReferenceType ⇒ {
                    classHierarchy.isSubtypeOf(t, cmpTpe)
                }
            case _ ⇒
                PointsToSetLike.noFilter
        }
        val fakeEntity = (defSiteObject, arrayType, filter)
        state.addArrayLoadEntity(fakeEntity)
        currentPointsToOfDefSites(fakeEntity, arrayDefSites).foreach { pts ⇒
            pts.forNewestNElements(pts.numElements) { as ⇒
                val typeId = allocationSiteLongToTypeId(as.asInstanceOf[Long])
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
        field: AField, objRefDefSites: IntTrieSet, rhsDefSites: IntTrieSet
    )(implicit state: State): Unit = {
        val fakeEntity = (rhsDefSites, field)
        state.addPutFieldEntity(fakeEntity)
        val filter = { t: ReferenceType ⇒
            classHierarchy.isSubtypeOf(t, field.fieldType.asReferenceType)
        }
        currentPointsToOfDefSites(fakeEntity, objRefDefSites).foreach { pts ⇒
            pts.forNewestNElements(pts.numElements) { as ⇒
                // TODO: Refactor
                val fieldClassType = field.classType
                val asTypeId = allocationSiteLongToTypeId(as.asInstanceOf[Long])
                if (fieldClassType.id == asTypeId || classHierarchy.subtypeInformation(fieldClassType).get.containsId(asTypeId)) {
                    val fieldEntity = (as, field)
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
        val filter = { t: ReferenceType ⇒
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
        currentPointsToOfDefSites(fakeEntity, arrayDefSites).foreach { pts ⇒
            pts.forNewestNElements(pts.numElements) { as ⇒
                val typeId = allocationSiteLongToTypeId(as.asInstanceOf[Long])
                if (typeId < 0 &&
                    classHierarchy.isSubtypeOf(ArrayType.lookup(typeId), arrayType) &&
                    !isEmptyArrayAllocationSite(as.asInstanceOf[Long])) {
                    val arrayEntity = ArrayEntity(as)
                    val componentType = ArrayType.lookup(typeId).componentType.asReferenceType
                    val filter = { t: ReferenceType ⇒
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
        depender: Entity, dependee: Entity, typeFilter: ReferenceType ⇒ Boolean
    )(implicit state: State): PointsToSet = {
        dependee match {
            case ds: DefinitionSite if state.hasAllocationSitePointsToSet(ds) ⇒
                state.allocationSitePointsToSet(ds)

            case _ ⇒
                val epk = EPK(dependee, pointsToPropertyKey)
                val p2s = if (state.hasDependency(depender, epk)) {
                    // IMPROVE: add a method to the state
                    state.dependeesOf(depender)(epk)._1
                } else {
                    val p2s = propertyStore(dependee, pointsToPropertyKey)
                    if (p2s.isRefinable) {
                        state.addDependee(depender, p2s, typeFilter)
                    }
                    p2s
                }
                pointsToUB(p2s.asInstanceOf[EOptionP[Entity, PointsToSet]])
        }
    }

    @inline protected[this] def updatedDependees(
        eps: SomeEPS, oldDependees: Map[SomeEPK, (SomeEOptionP, ReferenceType ⇒ Boolean)]
    ): Map[SomeEPK, (SomeEOptionP, ReferenceType ⇒ Boolean)] = {
        val epk = eps.toEPK
        val typeFilter = oldDependees(epk)._2
        if (eps.isRefinable) {
            oldDependees + (epk → ((eps, typeFilter)))
        } else {
            oldDependees - epk
        }
    }

    @inline protected[this] def updatedPointsToSet(
        oldPointsToSet:         PointsToSet,
        newDependeePointsToSet: PointsToSet,
        dependee:               SomeEPS,
        oldDependees:           Map[SomeEPK, (SomeEOptionP, ReferenceType ⇒ Boolean)]
    ): PointsToSet = {
        val (oldDependee, typeFilter) = oldDependees(dependee.toEPK)
        val oldDependeePointsTo = oldDependee match {
            case UBP(ub: PointsToSet @unchecked)   ⇒ ub
            case _: EPK[_, PointsToSet @unchecked] ⇒ emptyPointsToSet
            case d ⇒
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
        rhsDefSitesEPS: Traversable[SomeEPK],
        field:          AField,
        dependees:      Map[SomeEPK, (SomeEOptionP, ReferenceType ⇒ Boolean)]
    )(eps: SomeEPS): ProperPropertyComputationResult = {
        eps match {
            case UBP(newDependeePointsTo: PointsToSet @unchecked) ⇒
                val newDependees = updatedDependees(eps, dependees)
                var results: List[ProperPropertyComputationResult] = List.empty

                newDependeePointsTo.forNewestNElements(newDependeePointsTo.numElements - getNumElements(dependees(eps.toEPK)._1)) { as ⇒
                    // TODO: Refactor
                    val fieldClassType = field.classType
                    val asTypeId = allocationSiteLongToTypeId(as.asInstanceOf[Long])
                    if (fieldClassType.id == asTypeId || classHierarchy.subtypeInformation(fieldClassType).get.containsId(asTypeId)) {
                        val typeFilter = { t: ReferenceType ⇒
                            classHierarchy.isSubtypeOf(t, field.fieldType.asReferenceType)
                        }
                        results ::= InterimPartialResult(
                            rhsDefSitesEPS,
                            continuationForShared(
                                (as, field),
                                rhsDefSitesEPS.toIterator.map(d ⇒ d → ((d, typeFilter))).toMap
                            )
                        )
                    }
                }
                if (newDependees.nonEmpty) {
                    results ::= InterimPartialResult(
                        newDependees.values.map(_._1),
                        continuationForNewAllocationSitesAtPutField(
                            rhsDefSitesEPS, field, newDependees
                        )
                    )
                }
                Results(
                    results
                )
        }
    }

    protected[this] def continuationForNewAllocationSitesAtArrayStore(
        rhsDefSitesEPS: Traversable[SomeEPK],
        arrayType:      ArrayType,
        dependees:      Map[SomeEPK, (SomeEOptionP, ReferenceType ⇒ Boolean)]
    )(eps: SomeEPS): ProperPropertyComputationResult = {
        eps match {
            case UBP(newDependeePointsTo: PointsToSet @unchecked) ⇒
                val newDependees = updatedDependees(eps, dependees)
                var results: List[ProperPropertyComputationResult] = List.empty

                newDependeePointsTo.forNewestNElements(newDependeePointsTo.numElements - getNumElements(dependees(eps.toEPK)._1)) { as ⇒
                    val typeId = allocationSiteLongToTypeId(as.asInstanceOf[Long])
                    if (typeId < 0 &&
                        classHierarchy.isSubtypeOf(ArrayType.lookup(typeId), arrayType) &&
                        !isEmptyArrayAllocationSite(as.asInstanceOf[Long])) {
                        val componentType = ArrayType.lookup(typeId).componentType.asReferenceType
                        val typeFilter = { t: ReferenceType ⇒
                            classHierarchy.isSubtypeOf(t, componentType)
                        }
                        results ::= InterimPartialResult(
                            rhsDefSitesEPS,
                            continuationForShared(
                                ArrayEntity(as),
                                rhsDefSitesEPS.toIterator.map(d ⇒ d → ((d, typeFilter))).toMap
                            )
                        )
                    }
                }
                if (newDependees.nonEmpty) {
                    results ::= InterimPartialResult(
                        newDependees.values.map(_._1),
                        continuationForNewAllocationSitesAtArrayStore(
                            rhsDefSitesEPS, arrayType, newDependees
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
        defSiteObject: DefinitionSite,
        field:         AField,
        filter:        ReferenceType ⇒ Boolean,
        dependees:     Map[SomeEPK, (SomeEOptionP, ReferenceType ⇒ Boolean)]
    )(eps: SomeEPS): ProperPropertyComputationResult = {
        eps match {
            case UBP(newDependeePointsTo: PointsToSet @unchecked) ⇒
                val newDependees = updatedDependees(eps, dependees)
                var nextDependees: List[SomeEPK] = Nil
                newDependeePointsTo.forNewestNElements(newDependeePointsTo.numElements - getNumElements(dependees(eps.toEPK)._1)) { as ⇒
                    // TODO: Refactor
                    val fieldClassType = field.classType
                    val asTypeId = allocationSiteLongToTypeId(as.asInstanceOf[Long])
                    if (fieldClassType.id == asTypeId || classHierarchy.subtypeInformation(fieldClassType).get.containsId(asTypeId)) {
                        val epk = EPK((as, field), pointsToPropertyKey)
                        ps(epk)
                        nextDependees ::= epk
                    }
                }

                var results: List[ProperPropertyComputationResult] = Nil
                if (newDependees.nonEmpty) {
                    results ::= InterimPartialResult(
                        newDependees.values.map(_._1),
                        continuationForNewAllocationSitesAtGetField(
                            defSiteObject, field, filter, newDependees
                        )
                    )
                }
                if (nextDependees.nonEmpty) {
                    results ::= InterimPartialResult(
                        nextDependees,
                        continuationForShared(
                            defSiteObject, nextDependees.iterator.map(d ⇒ d → ((d, filter))).toMap
                        )
                    )
                }
                Results(results)
            case _ ⇒ throw new IllegalArgumentException(s"unexpected update: $eps")
        }
    }

    // todo name
    protected[this] def continuationForNewAllocationSitesAtArrayLoad(
        defSiteObject: DefinitionSite,
        arrayType:     ArrayType,
        filter:        ReferenceType ⇒ Boolean,
        dependees:     Map[SomeEPK, (SomeEOptionP, ReferenceType ⇒ Boolean)]
    )(eps: SomeEPS): ProperPropertyComputationResult = {
        eps match {
            case UBP(newDependeePointsTo: PointsToSet @unchecked) ⇒
                val newDependees = updatedDependees(eps, dependees)
                var nextDependees: List[SomeEPK] = Nil
                newDependeePointsTo.forNewestNElements(newDependeePointsTo.numElements - getNumElements(dependees(eps.toEPK)._1)) { as ⇒
                    val typeId = allocationSiteLongToTypeId(as.asInstanceOf[Long])
                    if (typeId < 0 && classHierarchy.isSubtypeOf(ArrayType.lookup(typeId), arrayType)) {
                        val epk = EPK(ArrayEntity(as), pointsToPropertyKey)
                        ps(epk)
                        nextDependees ::= epk
                    }
                }

                var results: List[ProperPropertyComputationResult] = Nil
                if (newDependees.nonEmpty) {
                    results ::= InterimPartialResult(
                        newDependees.values.map(_._1),
                        continuationForNewAllocationSitesAtArrayLoad(
                            defSiteObject, arrayType, filter, newDependees
                        )
                    )
                }
                if (nextDependees.nonEmpty) {
                    results ::= InterimPartialResult(
                        nextDependees,
                        continuationForShared(
                            defSiteObject, nextDependees.iterator.map(d ⇒ d → ((d, filter))).toMap
                        )
                    )
                }
                Results(results)
            case _ ⇒ throw new IllegalArgumentException(s"unexpected update: $eps")
        }
    }

    protected[this] def continuationForShared(
        e: Entity, dependees: Map[SomeEPK, (SomeEOptionP, ReferenceType ⇒ Boolean)]
    )(eps: SomeEPS): ProperPropertyComputationResult = {
        eps match {
            case UBP(newDependeePointsTo: PointsToSet @unchecked) ⇒
                val newDependees = updatedDependees(eps, dependees)

                updatedResults(e, newDependees, newDependeePointsTo, {
                    old ⇒
                        updatedPointsToSet(
                            old,
                            newDependeePointsTo,
                            eps,
                            dependees
                        )
                })

            case _ ⇒ throw new IllegalArgumentException(s"unexpected update: $eps")
        }
    }

    @inline protected[this] def updatedResults(
        e:                   Entity,
        newDependees:        Map[SomeEPK, (SomeEOptionP, ReferenceType ⇒ Boolean)],
        newDependeePointsTo: PointsToSet,
        updatePointsTo:      PointsToSet ⇒ PointsToSet
    ): ProperPropertyComputationResult = {
        if (newDependeePointsTo ne emptyPointsToSet) {
            val pr = PartialResult[Entity, PointsToSetLike[_, _, PointsToSet]](
                e,
                pointsToPropertyKey,
                {
                    case eps @ UBP(ub: PointsToSet @unchecked) ⇒
                        val newPointsToSet = updatePointsTo(ub)

                        if (newPointsToSet ne ub) {
                            Some(InterimEUBP(e, newPointsToSet))
                        } else {
                            None
                        }

                    case _: EPK[Entity, _] ⇒
                        val newPointsToSet = updatePointsTo(emptyPointsToSet)
                        if(newPointsToSet ne emptyPointsToSet)
                            Some(InterimEUBP(e, newPointsToSet))
                        else
                            None

                    case eOptP ⇒
                        throw new IllegalArgumentException(s"unexpected eOptP: $eOptP")
                }
            )

            if (newDependees.nonEmpty) {
                val ipr = InterimPartialResult(
                    newDependees.values.map(_._1), continuationForShared(e, newDependees)
                )
                Results(pr, ipr)
            } else {
                pr
            }
        } else if (newDependees.nonEmpty) {
            InterimPartialResult(
                newDependees.values.map(_._1),
                continuationForShared(e, newDependees)
            )
        } else {
            Results()
        }
    }
}

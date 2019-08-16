/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package pointsto

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

import org.opalj.log.LogContext
import org.opalj.log.OPALLogger
import org.opalj.log.Warn
import org.opalj.collection.immutable.IntTrieSet
import org.opalj.fpcf.Entity
import org.opalj.fpcf.EOptionP
import org.opalj.fpcf.PropertyStore
import org.opalj.fpcf.SomeEOptionP
import org.opalj.fpcf.SomeEPK
import org.opalj.br.DefinedMethod
import org.opalj.br.Method
import org.opalj.br.fpcf.properties.cg.Callees
import org.opalj.br.fpcf.properties.pointsto.PointsToSetLike
import org.opalj.br.DeclaredMethod
import org.opalj.br.fpcf.properties.cg.NoCallees
import org.opalj.br.ArrayType
import org.opalj.br.ReferenceType
import org.opalj.tac.common.DefinitionSite
import org.opalj.tac.fpcf.properties.TACAI

/**
 * Encapsulates the state of the analysis, analyzing a certain method using the
 * [[org.opalj.tac.fpcf.analyses.pointsto.TypeBasedPointsToAnalysis]].
 *
 * @author Florian Kuebler
 */
class PointsToAnalysisState[ElementType, PointsToSet <: PointsToSetLike[ElementType, _, PointsToSet]](
        override val method:                       DefinedMethod,
        override protected[this] var _tacDependee: EOptionP[Method, TACAI]
) extends TACAIBasedAnalysisState {

    private[this] val getFields: ArrayBuffer[(DefinitionSite, AField, ReferenceType ⇒ Boolean)] =
        ArrayBuffer.empty
    private[this] val putFields: ArrayBuffer[(IntTrieSet, AField)] = ArrayBuffer.empty

    def addGetFieldEntity(fakeEntity: (DefinitionSite, AField, ReferenceType ⇒ Boolean)): Unit = {
        getFields += fakeEntity
    }

    def getFieldsIterator: Iterator[(DefinitionSite, AField, ReferenceType ⇒ Boolean)] =
        getFields.iterator

    def addPutFieldEntity(fakeEntity: (IntTrieSet, AField)): Unit = {
        putFields += fakeEntity
    }

    def putFieldsIterator: Iterator[(IntTrieSet, AField)] = putFields.iterator

    private[this] val arrayLoads: ArrayBuffer[(DefinitionSite, ArrayType, ReferenceType ⇒ Boolean)] =
        ArrayBuffer.empty
    private[this] val arrayStores: ArrayBuffer[(IntTrieSet, ArrayType)] = ArrayBuffer.empty

    def addArrayLoadEntity(
        fakeEntity: (DefinitionSite, ArrayType, ReferenceType ⇒ Boolean)
    ): Unit = {
        arrayLoads += fakeEntity
    }

    def arrayLoadsIterator: Iterator[(DefinitionSite, ArrayType, ReferenceType ⇒ Boolean)] =
        arrayLoads.iterator

    def addArrayStoreEntity(fakeEntity: (IntTrieSet, ArrayType)): Unit = {
        arrayStores += fakeEntity
    }

    def arrayStoresIterator: Iterator[(IntTrieSet, ArrayType)] =
        arrayStores.iterator

    private[this] val _allocationSitePointsToSet: mutable.Map[DefinitionSite, PointsToSet] = {
        mutable.Map.empty
    }

    def hasAllocationSitePointsToSet(e: DefinitionSite): Boolean = _allocationSitePointsToSet.contains(e)

    def allocationSitePointsToSet(e: DefinitionSite): PointsToSet = _allocationSitePointsToSet(e)

    def allocationSitePointsToSetsIterator: Iterator[(DefinitionSite, PointsToSet)] = {
        _allocationSitePointsToSet.iterator
    }

    def setAllocationSitePointsToSet(ds: DefinitionSite, pointsToSet: PointsToSet): Unit = {
        assert(!_allocationSitePointsToSet.contains(ds))
        _allocationSitePointsToSet(ds) = pointsToSet
    }

    private[this] val _sharedPointsToSets: mutable.Map[Entity, PointsToSet] = {
        mutable.Map.empty
    }

    def includeSharedPointsToSet(
        e:           Entity,
        pointsToSet: PointsToSet,
        typeFilter:  ReferenceType ⇒ Boolean
    ): Unit = {
        if (_sharedPointsToSets.contains(e)) {
            val oldPointsToSet = _sharedPointsToSets(e)
            val newPointsToSet = oldPointsToSet.included(pointsToSet, typeFilter)
            _sharedPointsToSets(e) = newPointsToSet
        } else {
            _sharedPointsToSets(e) = pointsToSet.filter(typeFilter)
        }
    }

    def includeSharedPointsToSets(
        e: Entity, pointsToSets: Iterator[PointsToSet], typeFilter: ReferenceType ⇒ Boolean
    ): Unit = {
        pointsToSets.foreach(pointsToSet ⇒ includeSharedPointsToSet(e, pointsToSet, typeFilter))
    }

    def sharedPointsToSetsIterator: Iterator[(Entity, PointsToSet)] = {
        _sharedPointsToSets.iterator
    }

    // TODO: should include PointsTo and Callees dependencies
    private[this] val _dependerToDependees: mutable.Map[Entity, mutable.Set[(SomeEOptionP, ReferenceType ⇒ Boolean)]] = {
        mutable.Map.empty
    }

    final def hasDependees(depender: Entity): Boolean = {
        assert(!_dependerToDependees.contains(depender) || _dependerToDependees(depender).nonEmpty)
        _dependerToDependees.contains(depender)
    }

    final def addDependee(
        depender:   Entity,
        dependee:   SomeEOptionP,
        typeFilter: ReferenceType ⇒ Boolean
    ): Unit = {
        assert(
            !_dependerToDependees.contains(depender) ||
                !_dependerToDependees(depender).exists(other ⇒ other._1.e == dependee.e && other._1.pk.id == dependee.pk.id)
        )
        if (_dependerToDependees.contains(depender)) {
            _dependerToDependees(depender) += ((dependee, typeFilter))
        } else {
            _dependerToDependees += (depender → mutable.Set((dependee, typeFilter)))
        }
    }

    // IMPROVE: potentially inefficient exists check
    final def hasDependency(depender: Entity, dependee: SomeEPK): Boolean = {
        _dependerToDependees.contains(depender) &&
            _dependerToDependees(depender).exists(other ⇒ other._1.e == dependee.e && other._1.pk.id == dependee.pk.id)
    }

    // IMPROVE: make it efficient
    final def dependeesOf(depender: Entity): Map[SomeEPK, (SomeEOptionP, ReferenceType ⇒ Boolean)] = {
        assert(_dependerToDependees.contains(depender))
        _dependerToDependees(depender).iterator.map(dependee ⇒ (dependee._1.toEPK, dependee)).toMap
    }

    private[this] var _calleesDependee: Option[EOptionP[DeclaredMethod, Callees]] = None

    def callees(ps: PropertyStore): Callees = {
        val calleesProperty = if (_calleesDependee.isDefined) {

            _calleesDependee.get
        } else {
            val calleesProperty = ps(method, Callees.key)
            _calleesDependee = Some(calleesProperty)
            calleesProperty
        }

        if (calleesProperty.isEPK)
            NoCallees
        else calleesProperty.ub
    }

    def hasCalleesDepenedee: Boolean = {
        _calleesDependee.nonEmpty && _calleesDependee.get.isRefinable
    }

    def setCalleesDependee(calleeDependee: EOptionP[DeclaredMethod, Callees]): Unit = {
        _calleesDependee = Some(calleeDependee)
    }

    def calleesDependee: EOptionP[DeclaredMethod, Callees] = _calleesDependee.get

    def addIncompletePointsToInfo(pc: Int)(implicit logContext: LogContext): Unit = {
        OPALLogger.logOnce(Warn(
            "the points-to sets might be incomplete (e.g. due to reflection or incomplete project information)"
        ))
        // Todo: We need a mechanism to mark points-to sets as incomplete
    }

}

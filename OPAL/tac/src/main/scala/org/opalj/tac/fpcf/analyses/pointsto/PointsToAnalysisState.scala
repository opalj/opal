/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package pointsto

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

import org.opalj.br.ArrayType
import org.opalj.br.DeclaredField
import org.opalj.br.DeclaredMethod
import org.opalj.br.Method
import org.opalj.br.ReferenceType
import org.opalj.br.fpcf.properties.Context
import org.opalj.br.fpcf.properties.cg.Callees
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
import org.opalj.fpcf.Property
import org.opalj.fpcf.PropertyKey
import org.opalj.fpcf.PropertyStore
import org.opalj.fpcf.SomeEOptionP
import org.opalj.fpcf.SomeEPK
import org.opalj.log.LogContext
import org.opalj.log.OPALLogger
import org.opalj.log.Warn
import org.opalj.tac.fpcf.analyses.cg.BaseAnalysisState
import org.opalj.tac.fpcf.properties.TACAI

/**
 * Encapsulates the state of the analysis, analyzing a certain method using the
 * [[org.opalj.tac.fpcf.analyses.pointsto.TypeBasedPointsToAnalysis]].
 *
 * @author Florian Kuebler
 */
class PointsToAnalysisState[
    ElementType,
    PointsToSet <: PointsToSetLike[ElementType, _, PointsToSet],
    ContextType <: Context
](
    override val callContext:                  ContextType,
    override protected[this] var _tacDependee: EOptionP[Method, TACAI]
) extends BaseAnalysisState with TACAIBasedAnalysisState[ContextType] {

    private[this] val getFields: ArrayBuffer[(Entity, Option[DeclaredField], ReferenceType => Boolean)] =
        ArrayBuffer.empty
    private[this] val putFields: ArrayBuffer[(IntTrieSet, Option[DeclaredField])] = ArrayBuffer.empty

    def addGetFieldEntity(fakeEntity: (Entity, Option[DeclaredField], ReferenceType => Boolean)): Unit = {
        getFields += fakeEntity
    }

    def getFieldsIterator: Iterator[(Entity, Option[DeclaredField], ReferenceType => Boolean)] = getFields.iterator

    def addPutFieldEntity(fakeEntity: (IntTrieSet, Option[DeclaredField])): Unit = {
        putFields += fakeEntity
    }

    def putFieldsIterator: Iterator[(IntTrieSet, Option[DeclaredField])] = putFields.iterator

    private[this] val arrayLoads: ArrayBuffer[(Entity, ArrayType, ReferenceType => Boolean)] = ArrayBuffer.empty
    private[this] val arrayStores: ArrayBuffer[(IntTrieSet, ArrayType)] = ArrayBuffer.empty

    def addArrayLoadEntity(
        fakeEntity: (Entity, ArrayType, ReferenceType => Boolean)
    ): Unit = {
        arrayLoads += fakeEntity
    }

    def arrayLoadsIterator: Iterator[(Entity, ArrayType, ReferenceType => Boolean)] =
        arrayLoads.iterator

    def addArrayStoreEntity(fakeEntity: (IntTrieSet, ArrayType)): Unit = {
        arrayStores += fakeEntity
    }

    def arrayStoresIterator: Iterator[(IntTrieSet, ArrayType)] =
        arrayStores.iterator

    private[this] val _allocationSitePointsToSets: mutable.Map[Entity, PointsToSet] = {
        mutable.Map.empty
    }

    def hasAllocationSitePointsToSet(e: Entity): Boolean = _allocationSitePointsToSets.contains(e)

    def allocationSitePointsToSet(e: Entity): PointsToSet = _allocationSitePointsToSets(e)

    def allocationSitePointsToSetsIterator: Iterator[(Entity, PointsToSet)] = {
        _allocationSitePointsToSets.iterator
    }

    def setAllocationSitePointsToSet(ds: Entity, pointsToSet: PointsToSet): Unit = {
        assert(!_allocationSitePointsToSets.contains(ds))
        _allocationSitePointsToSets(ds) = pointsToSet
    }

    private[this] val _sharedPointsToSets: mutable.Map[Entity, PointsToSet] = {
        mutable.Map.empty
    }

    def includeSharedPointsToSet(
        e:           Entity,
        pointsToSet: PointsToSet,
        typeFilter:  ReferenceType => Boolean = PointsToSetLike.noFilter
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
        e:            Entity,
        pointsToSets: Iterator[PointsToSet],
        typeFilter:   ReferenceType => Boolean
    ): Unit = {
        pointsToSets.foreach(pointsToSet => includeSharedPointsToSet(e, pointsToSet, typeFilter))
    }

    def sharedPointsToSetsIterator: Iterator[(Entity, PointsToSet)] = {
        _sharedPointsToSets.iterator
    }

    private[this] val _dependees: mutable.Map[EPK[Entity, Property], EOptionP[Entity, Property]] = {
        mutable.Map.empty
    }

    // TODO: should include PointsTo and Callees dependencies
    private[this] val _dependerToDependees: mutable.Map[Entity, mutable.Set[(SomeEOptionP, ReferenceType => Boolean)]] = {
        mutable.Map.empty
    }

    final def hasDependees(depender: Entity): Boolean = {
        assert(!_dependerToDependees.contains(depender) || _dependerToDependees(depender).nonEmpty)
        _dependerToDependees.contains(depender)
    }

    final def addDependee(
        depender:   Entity,
        dependee:   SomeEOptionP,
        typeFilter: ReferenceType => Boolean
    ): Unit = {
        val dependeeEPK = dependee.toEPK

        assert(
            !_dependerToDependees.contains(depender) ||
            !_dependerToDependees(depender).exists(other =>
                other._1.e == dependee.e && other._1.pk.id == dependee.pk.id
            )
        )
        assert(!_dependees.contains(dependeeEPK) || _dependees(dependeeEPK) == dependee)
        if (_dependerToDependees.contains(depender)) {
            _dependerToDependees(depender) += ((dependee, typeFilter))
        } else {
            _dependerToDependees += (depender -> mutable.Set((dependee, typeFilter)))
        }

        if (!_dependees.contains(dependeeEPK))
            _dependees(dependeeEPK) = dependee
    }

    final def hasDependee(dependee: EPK[Entity, Property]): Boolean = {
        _dependees.contains(dependee)
    }

    final def getProperty[E <: Entity, P <: Property](dependee: EPK[E, P]): EOptionP[E, P] = {
        _dependees(dependee).asInstanceOf[EOptionP[E, P]]
    }

    // IMPROVE: potentially inefficient exists check
    final def hasDependency(depender: Entity, dependee: SomeEPK): Boolean = {
        _dependerToDependees.contains(depender) &&
        _dependerToDependees(depender).exists(other => other._1.e == dependee.e && other._1.pk.id == dependee.pk.id)
    }

    // IMPROVE: make it efficient
    final def dependeesOf(depender: Entity): Map[SomeEPK, (SomeEOptionP, ReferenceType => Boolean)] = {
        assert(_dependerToDependees.contains(depender))
        _dependerToDependees(depender).iterator.map(dependee => (dependee._1.toEPK, dependee)).toMap
    }

    private[this] var _calleesDependee: Option[EOptionP[DeclaredMethod, Callees]] = None

    def callees(ps: PropertyStore): Callees = {
        val calleesProperty = if (_calleesDependee.isDefined) {
            _calleesDependee.get
        } else {
            val calleesProperty = ps(callContext.method, Callees.key)
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

    private[this] def accesses[P <: MethodFieldAccessInformation[P]](
        ps:                 PropertyStore,
        dependee:           Option[EOptionP[Method, P]],
        setDependee:        EOptionP[Method, P] => Unit,
        key:                PropertyKey[P],
        noAccessesProperty: P
    ): P = {
        val accessesProperty = if (dependee.isDefined) {
            dependee.get
        } else {
            val writeAccessesProperty = ps(callContext.method.asDefinedMethod.definedMethod, key)
            setDependee(writeAccessesProperty)
            writeAccessesProperty
        }

        if (accessesProperty.isEPK) noAccessesProperty
        else accessesProperty.ub
    }

    private[this] var _readAccessesDependee: Option[EOptionP[Method, MethodFieldReadAccessInformation]] = None
    private[this] var _writeAccessesDependee: Option[EOptionP[Method, MethodFieldWriteAccessInformation]] = None

    def readAccesses(ps: PropertyStore): MethodFieldReadAccessInformation = accesses(
        ps,
        _readAccessesDependee,
        setReadAccessDependee,
        MethodFieldReadAccessInformation.key,
        NoMethodFieldReadAccessInformation
    )

    def hasReadAccessDependee: Boolean = {
        _readAccessesDependee.nonEmpty && _readAccessesDependee.get.isRefinable
    }

    def setReadAccessDependee(dependee: EOptionP[Method, MethodFieldReadAccessInformation]): Unit = {
        _readAccessesDependee = Some(dependee)
    }

    def readAccessDependee: EOptionP[Method, MethodFieldReadAccessInformation] = _readAccessesDependee.get

    def writeAccesses(ps: PropertyStore): MethodFieldWriteAccessInformation = accesses(
        ps,
        _writeAccessesDependee,
        setWriteAccessDependee,
        MethodFieldWriteAccessInformation.key,
        NoMethodFieldWriteAccessInformation
    )

    def hasWriteAccessDependee: Boolean = {
        _writeAccessesDependee.nonEmpty && _writeAccessesDependee.get.isRefinable
    }

    def setWriteAccessDependee(dependee: EOptionP[Method, MethodFieldWriteAccessInformation]): Unit = {
        _writeAccessesDependee = Some(dependee)
    }

    def writeAccessDependee: EOptionP[Method, MethodFieldWriteAccessInformation] = _writeAccessesDependee.get

    override def hasOpenDependencies: Boolean = {
        hasCalleesDepenedee || super.hasOpenDependencies
    }

    override def dependees: Set[SomeEOptionP] = {
        val otherDependees = super.dependees
        if (hasCalleesDepenedee)
            otherDependees + _calleesDependee.get
        else
            otherDependees
    }

    def addIncompletePointsToInfo(pc: Int)(implicit logContext: LogContext): Unit = {
        OPALLogger.logOnce(Warn(
            "the points-to sets might be incomplete (e.g. due to reflection or incomplete project information)"
        ))
        // Todo: We need a mechanism to mark points-to sets as incomplete
    }

}

/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package pointsto

import scala.collection.mutable

import org.opalj.log.LogContext
import org.opalj.log.OPALLogger
import org.opalj.log.Warn
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
import org.opalj.tac.common.DefinitionSite
import org.opalj.tac.fpcf.properties.TACAI

/**
 * Encapsulates the state of the analysis, analyzing a certain method using the
 * [[org.opalj.tac.fpcf.analyses.pointsto.TypeBasedPointsToAnalysis]].
 *
 * @author Florian Kuebler
 */
class PointsToAnalysisState[PointsToSet <: PointsToSetLike[_, _, PointsToSet]](
        override val method:                       DefinedMethod,
        override protected[this] var _tacDependee: EOptionP[Method, TACAI]
) extends TACAIBasedAnalysisState {

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

    private[this] val _localPointsToSets: mutable.Map[Entity, PointsToSet] = mutable.Map.empty

    def setLocalPointsToSet(e: Entity, pointsToSet: PointsToSet): Unit = {
        assert(!_localPointsToSets.contains(e))
        _localPointsToSets(e) = pointsToSet
    }

    def includeLocalPointsToSet(e: Entity, pointsToSet: PointsToSet): Unit = {
        if (_localPointsToSets.contains(e)) {
            val oldPointsToSet = _localPointsToSets(e)
            val newPointsToSet = oldPointsToSet.included(pointsToSet)
            _localPointsToSets(e) = newPointsToSet
        } else {
            _localPointsToSets(e) = pointsToSet
        }
    }

    def includeLocalPointsToSets(e: Entity, pointsToSets: Iterator[PointsToSet]): Unit = {
        pointsToSets.foreach(pointsToSet ⇒ includeLocalPointsToSet(e, pointsToSet))
    }

    def localPointsToSetsIterator: Iterator[(Entity, PointsToSet)] = {
        _localPointsToSets.iterator
    }

    def hasLocalPointsToSet(e: Entity): Boolean = _localPointsToSets.contains(e)

    def localPointsToSet(e: Entity): PointsToSet = _localPointsToSets(e)

    private[this] val _sharedPointsToSets: mutable.Map[Entity, PointsToSet] = mutable.Map.empty

    def includeSharedPointsToSet(e: Entity, pointsToSet: PointsToSet): Unit = {
        if (_sharedPointsToSets.contains(e)) {
            val oldPointsToSet = _sharedPointsToSets(e)
            val newPointsToSet = oldPointsToSet.included(pointsToSet)
            _sharedPointsToSets(e) = newPointsToSet
        } else {
            _sharedPointsToSets(e) = pointsToSet
        }
    }

    def includeSharedPointsToSets(e: Entity, pointsToSets: Iterator[PointsToSet]): Unit = {
        pointsToSets.foreach(pointsToSet ⇒ includeSharedPointsToSet(e, pointsToSet))
    }

    def sharedPointsToSetsIterator: Iterator[(Entity, PointsToSet)] = {
        _sharedPointsToSets.iterator
    }

    // TODO: should include PointsTo and Callees dependencies
    private[this] val _dependerToDependees: mutable.Map[Entity, mutable.Set[SomeEOptionP]] = {
        mutable.Map.empty
    }

    final def hasDependees(depender: Entity): Boolean = {
        assert(!_dependerToDependees.contains(depender) || _dependerToDependees(depender).nonEmpty)
        _dependerToDependees.contains(depender)
    }

    final def addDependee(
        depender: Entity,
        dependee: SomeEOptionP
    ): Unit = {
        assert(
            !_dependerToDependees.contains(depender) ||
                !_dependerToDependees(depender).exists(other ⇒ other.e == dependee.e && other.pk == dependee.pk)
        )
        if (_dependerToDependees.contains(depender)) {
            _dependerToDependees(depender) += dependee
        } else {
            _dependerToDependees += (depender → mutable.Set(dependee))
        }
    }

    // IMPROVE: potentially inefficient exists check
    final def hasDependency(depender: Entity, dependee: SomeEPK): Boolean = {
        _dependerToDependees.contains(depender) &&
            _dependerToDependees(depender).exists(other ⇒ other.e == dependee.e && other.pk == dependee.pk)
    }

    // IMPROVE: make it efficient
    final def dependeesOf(depender: Entity): Map[SomeEPK, SomeEOptionP] = {
        _dependerToDependees(depender).iterator.map(dependee ⇒ (dependee.toEPK, dependee)).toMap
    }

    final def plainDependeesOf(depender: Entity): mutable.Set[SomeEOptionP] = {
        _dependerToDependees(depender)
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

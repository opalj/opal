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
import org.opalj.fpcf.EPS
import org.opalj.fpcf.Property
import org.opalj.fpcf.PropertyStore
import org.opalj.br.DeclaredMethod
import org.opalj.br.DefinedMethod
import org.opalj.br.Method
import org.opalj.br.fpcf.properties.cg.Callees
import org.opalj.br.fpcf.properties.cg.NoCallees
import org.opalj.br.fpcf.properties.pointsto.PointsToSetLike
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
) extends AbstractPointsToState[Entity, PointsToSet] {

    private[this] val _pointsToSets: mutable.Map[Entity, PointsToSet] = mutable.Map.empty

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

    def updateCalleesDependee(newCallees: EPS[DeclaredMethod, Callees]): Unit = {
        _calleesDependee = Some(newCallees)
    }

    override def dependees: List[EOptionP[Entity, Property]] = {
        // IMPROVE: make it more efficient (maybe use immutable map and join traversables)
        var allDependees = super.dependees

        if (_calleesDependee.isDefined && _calleesDependee.get.isRefinable)
            allDependees ::= _calleesDependee.get

        allDependees
    }

    override def hasOpenDependencies: Boolean = {
        (_calleesDependee.isDefined && _calleesDependee.get.isRefinable) ||
            super.hasOpenDependencies
    }

    def setOrUpdatePointsToSet(e: Entity, pointsToSet: PointsToSet): Unit = {
        if (_pointsToSets.contains(e)) {
            val oldPointsToSet = _pointsToSets(e)
            val newPointsToSet = oldPointsToSet.included(pointsToSet)
            _pointsToSets(e) = newPointsToSet
        } else {
            _pointsToSets(e) = pointsToSet
        }
    }

    def setPointsToSet(e: Entity, pointsToSet: PointsToSet): Unit = {
        assert(!_pointsToSets.contains(e))
        _pointsToSets(e) = pointsToSet
    }

    def setOrUpdatePointsToSet(e: Entity, pointsToSets: Iterator[PointsToSet]): Unit = {
        pointsToSets.foreach { pointsToSet ⇒
            setOrUpdatePointsToSet(e, pointsToSet)
        }
    }

    def clearPointsToSet(): Unit = {
        _pointsToSets.clear()
    }

    def pointsToSetsIterator: Iterator[(Entity, PointsToSet)] = {
        _pointsToSets.iterator
    }

    def addIncompletePointsToInfo(pc: Int)(implicit logContext: LogContext): Unit = {
        OPALLogger.logOnce(Warn(
            "the points-to sets might be incomplete (e.g. due to reflection or incomplete project information)"
        ))
        // Todo: We need a mechanism to mark points-to sets as incomplete
    }
}

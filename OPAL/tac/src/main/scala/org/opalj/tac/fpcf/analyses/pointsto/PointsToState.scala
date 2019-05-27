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
import org.opalj.collection.immutable.UIDSet
import org.opalj.fpcf.Entity
import org.opalj.fpcf.EOptionP
import org.opalj.fpcf.EPS
import org.opalj.fpcf.Property
import org.opalj.fpcf.PropertyStore
import org.opalj.br.fpcf.cg.properties.Callees
import org.opalj.br.fpcf.pointsto.properties.PointsTo
import org.opalj.br.DeclaredMethod
import org.opalj.br.DefinedMethod
import org.opalj.br.Method
import org.opalj.br.ObjectType
import org.opalj.br.fpcf.cg.properties.NoCallees
import org.opalj.tac.fpcf.properties.TACAI

/**
 * Interface for state classes of points-to based analyses that declares functionality to handle
 * dependencies of [[org.opalj.br.fpcf.pointsto.properties.PointsTo]] objects.
 *
 * @author Florian Kuebler
 */
// TODO: already implement these methods here.
trait AbstractPointsToState extends TACAIBasedAnalysisState {
    def addPointsToDependency(
        depender: Entity,
        dependee: EOptionP[Entity, PointsTo]
    ): Unit

    def getOrRetrievePointsToEPS(
        dependee: Entity, ps: PropertyStore
    ): EOptionP[Entity, PointsTo]
}

/**
 * Encapsulates the state of the analysis, analyzing a certain method using the
 * [[org.opalj.tac.fpcf.analyses.pointsto.AndersenStylePointsToAnalysis]].
 *
 * @author Florian Kuebler
 */
class PointsToState private (
        override val method:                       DefinedMethod,
        override protected[this] var _tacDependee: EOptionP[Method, TACAI]
) extends AbstractPointsToState {

    private[this] val _pointsToSets: mutable.Map[Entity, UIDSet[ObjectType]] = mutable.Map.empty

    private[this] var _calleesDependee: Option[EOptionP[DeclaredMethod, Callees]] = None

    // We organize the dependencies to points-to states within a bijective mapping of
    // dependers (the use sites) and their dependees (the respective definition sites).
    // For each dependee (def-site) we store the corresponding EOptionP, such that we can
    // efficiently perform updates here.
    // If we get an update for a dependee, we have to update all points-to sets for the
    // its dependers (_dependeeToDependers(dependee).
    private[this] val _dependeeToDependers: mutable.Map[Entity, mutable.Set[Entity]] = mutable.Map.empty

    private[this] val _dependerToDependees: mutable.Map[Entity, mutable.Set[Entity]] = mutable.Map.empty

    private[this] val _dependees: mutable.Map[Entity, EOptionP[Entity, PointsTo]] = mutable.Map.empty

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

        _dependees.valuesIterator.foreach(d ⇒ allDependees ::= d)

        if (_calleesDependee.isDefined && _calleesDependee.get.isRefinable)
            allDependees ::= _calleesDependee.get

        allDependees
    }

    override def hasOpenDependencies: Boolean = {
        _dependees.nonEmpty ||
            (_calleesDependee.isDefined && _calleesDependee.get.isRefinable) ||
            super.hasOpenDependencies

    }

    override def addPointsToDependency(
        depender: Entity,
        dependee: EOptionP[Entity, PointsTo]
    ): Unit = {
        _dependeeToDependers.getOrElseUpdate(dependee.e, mutable.Set.empty).add(depender)
        _dependerToDependees.getOrElseUpdate(depender, mutable.Set.empty).add(dependee.e)
        _dependees(dependee.e) = dependee
    }

    def removePointsToDependee(eps: EPS[Entity, PointsTo]): Unit = {
        val dependee = eps.e
        assert(_dependees.contains(dependee))
        //
        if (_dependees.remove(dependee).isEmpty)
            throw new RuntimeException(s"failed to remove dependee: $dependee")

        val dependers = _dependeeToDependers(dependee)

        if (_dependeeToDependers.remove(dependee).isEmpty)
            throw new RuntimeException(s"failed to remove dependee: $dependee")

        for (depender ← dependers) {
            val dependees = _dependerToDependees(depender)
            if (dependees.remove(dependee) && dependees.isEmpty)
                if (_dependerToDependees.remove(depender).isEmpty) {
                    throw new RuntimeException(s"failed to remove depender: $depender")
                }
        }
    }

    def updatePointsToDependee(eps: EOptionP[Entity, PointsTo]): Unit = {
        _dependees(eps.e) = eps
    }

    override def getOrRetrievePointsToEPS(
        dependee: Entity, ps: PropertyStore
    ): EOptionP[Entity, PointsTo] = {
        _dependees.getOrElse(dependee, ps(dependee, PointsTo.key))
    }

    def setOrUpdatePointsToSet(e: Entity, pointsToSet: UIDSet[ObjectType]): Unit = {
        if (_pointsToSets.contains(e)) {
            val newPointsToSet = _pointsToSets(e) ++ pointsToSet
            _pointsToSets(e) = newPointsToSet
        } else {
            _pointsToSets(e) = pointsToSet
        }
    }

    def clearPointsToSet(): Unit = {
        _pointsToSets.clear()
    }

    def pointsToSetsIterator: Iterator[(Entity, UIDSet[ObjectType])] = {
        _pointsToSets.iterator
    }

    def dependersOf(dependee: Entity): Traversable[Entity] = _dependeeToDependers(dependee)

    def hasDependees(potentialDepender: Entity): Boolean =
        _dependerToDependees.contains(potentialDepender)

    def addIncompletePointsToInfo(pc: Int)(implicit logContext: LogContext): Unit = {
        OPALLogger.logOnce(Warn(
            "the points-to sets might be incomplete (e.g. due to reflection or incomplete project information)"
        ))
        // Todo: We need a mechanism to mark points-to sets as incomplete
    }
}

object PointsToState {
    def apply(
        method:      DefinedMethod,
        tacDependee: EOptionP[Method, TACAI]
    ): PointsToState = {
        new PointsToState(method, tacDependee)
    }
}

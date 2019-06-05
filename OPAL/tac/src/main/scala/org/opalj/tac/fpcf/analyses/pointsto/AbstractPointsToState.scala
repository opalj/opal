/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package pointsto

import scala.collection.mutable

import org.opalj.fpcf.Entity
import org.opalj.fpcf.EOptionP
import org.opalj.fpcf.EPS
import org.opalj.fpcf.SomeEOptionP
import org.opalj.br.fpcf.properties.pointsto.PointsToSetLike

/**
 * Interface for state classes of points-to based analyses that declares functionality to handle
 * dependencies of [[PointsToSetLike]] objects.
 *
 * @author Florian Kuebler
 */
trait AbstractPointsToState[Depender, PointsToSet <: PointsToSetLike[_]]
    extends TACAIBasedAnalysisState {

    // maps a defsite to its result in the property store for the points-to set
    private[this] val _pointsToDependees: mutable.Map[Entity, EOptionP[Entity, PointsToSet]] = {
        mutable.Map.empty
    }

    // We organize the dependencies to points-to states within a bijective mapping of
    // dependers (the use sites) and their dependees (the respective definition sites).
    // For each dependee (def-site) we store the corresponding EOptionP, such that we can
    // efficiently perform updates here.
    // If we get an update for a dependee, we have to update all points-to sets for the
    // its dependers (_dependeeToDependers(dependee).
    private[this] val _dependeeToDependers: mutable.Map[Entity, mutable.Set[Depender]] = {
        mutable.Map.empty
    }

    private[this] val _dependerToDependees: mutable.Map[Depender, mutable.Set[Entity]] = {
        mutable.Map.empty
    }

    final def addPointsToDependency(
        depender: Depender,
        dependee: EOptionP[Entity, PointsToSet]
    ): Unit = {
        assert(
            !_dependeeToDependers.contains(dependee.e) ||
                !_dependeeToDependers(dependee.e).contains(depender)
        )
        _dependeeToDependers.getOrElseUpdate(dependee.e, mutable.Set.empty).add(depender)

        assert(
            !_dependerToDependees.contains(depender) ||
                !_dependerToDependees(depender).contains(dependee.e)
        )
        _dependerToDependees.getOrElseUpdate(depender, mutable.Set.empty).add(dependee.e)

        if (!_pointsToDependees.contains(dependee.e))
            _pointsToDependees(dependee.e) = dependee
    }

    final def removePointsToDependee(dependee: Entity): Unit = {
        // delete the points-to set of the dependee
        if (_pointsToDependees.remove(dependee).isEmpty)
            throw new RuntimeException(s"failed to remove dependee: $dependee")

        // for every depender we have to remove the dependee of the set of dependees
        val dependers = _dependeeToDependers(dependee)
        for (depender ← dependers) {
            val dependees = _dependerToDependees(depender)
            if (!dependees.remove(dependee))
                throw new RuntimeException(s"failed to remove dependee: $dependee")

            // if there is no dependee left, we remove the entry from the map
            if (dependees.isEmpty && _dependerToDependees.remove(depender).isEmpty) {
                throw new RuntimeException(s"failed to remove depender: $depender")
            }
        }

        // finally, we also remove the dependee from the map
        if (_dependeeToDependers.remove(dependee).isEmpty)
            throw new RuntimeException(s"failed to remove dependee: $dependee")
    }

    final def removePointsToDepender(depender: Depender): Unit = {
        // for every dependee of the given depender:
        // we have to remove the depender from the set of their dependers
        for (dependee ← _dependerToDependees(depender)) {
            if (!_dependeeToDependers(dependee).remove(depender)) {
                throw new RuntimeException(s"failed to remove depender: $depender")
            }
            // if there is no depender left for this dependee, we remove the entry from the map
            if (_dependeeToDependers(dependee).isEmpty) {
                removePointsToDependee(dependee)
            }
        }

        // now we can delete the depender
        if (_dependerToDependees.remove(depender).isEmpty) {
            throw new RuntimeException(s"failed to remove depender: $depender")
        }
    }

    final def hasPointsToDependee(dependee: Entity): Boolean = {
        _pointsToDependees.contains(dependee)
    }

    final def hasPointsToDependency(depender: Depender, dependee: Entity): Boolean = {
        _dependerToDependees.contains(depender) && _dependerToDependees(depender).contains(dependee)
    }

    final def getPointsToProperty(dependee: Entity): EOptionP[Entity, PointsToSet] = {
        _pointsToDependees(dependee)
    }

    final def updatePointsToDependency(eps: EPS[Entity, PointsToSet]): Unit = {
        assert(_pointsToDependees.contains(eps.e))
        _pointsToDependees(eps.e) = eps
    }

    // IMPROVE: In order to be thread-safe, we return an immutable copy of the set.
    //  However, this is very inefficient!
    final def dependersOf(dependee: Entity): Set[Depender] = {
        _dependeeToDependers(dependee).toSet
    }

    final def hasPointsToDependees: Boolean = {
        assert(
            (_pointsToDependees.isEmpty == _dependeeToDependers.isEmpty) &&
                (_dependeeToDependers.isEmpty == _dependerToDependees.isEmpty)
        )
        _pointsToDependees.nonEmpty
    }

    override def dependees: List[SomeEOptionP] = {
        // IMPROVE: make it more efficient (maybe use immutable map and join traversables)
        var allDependees = super.dependees

        _pointsToDependees.valuesIterator.foreach(d ⇒ allDependees ::= d)

        allDependees
    }

    override def hasOpenDependencies: Boolean = {
        hasPointsToDependees || super.hasOpenDependencies
    }
}

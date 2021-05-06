/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package cg
package pointsto

import scala.collection.mutable

import org.opalj.collection.immutable.IntTrieSet
import org.opalj.fpcf.Entity
import org.opalj.fpcf.EOptionP
import org.opalj.fpcf.EPS
import org.opalj.fpcf.SomeEOptionP
import org.opalj.br.DefinedMethod
import org.opalj.br.Method
import org.opalj.br.ObjectType
import org.opalj.br.fpcf.properties.pointsto.PointsToSetLike
import org.opalj.tac.fpcf.properties.TACAI

/**
 * TODO: That state object is currently copy&paste and should be refactored.
 *
 * @author Florian Kuebler
 */
class PointsToBasedCGState[PointsToSet <: PointsToSetLike[_, _, PointsToSet]](
        override val method:                       DefinedMethod,
        override protected[this] var _tacDependee: EOptionP[Method, TACAI]
) extends CGState with TACAIBasedAnalysisState {

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
    private[this] val _dependeeToDependers: mutable.Map[Entity, mutable.Set[CallSite]] = {
        mutable.Map.empty
    }
    private[this] val _dependerToDependees: mutable.Map[CallSite, mutable.Set[Entity]] = {
        mutable.Map.empty
    }

    final def addPointsToDependency(
        depender: CallSite,
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

    final def removePointsToDepender(depender: CallSite): Unit = {
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
            assert((!_dependeeToDependers.contains(dependee) && !_pointsToDependees.contains(dependee)) ||
                (_dependeeToDependers(dependee).nonEmpty && _pointsToDependees.contains(dependee)))
        }

        // now we can delete the depender
        if (_dependerToDependees.remove(depender).isEmpty) {
            throw new RuntimeException(s"failed to remove depender: $depender")
        }
    }

    final def removeTypeForCallSite(callSite: CallSite, instantiatedType: ObjectType): Unit = {
        if (!_virtualCallSites.contains(callSite))
            assert(_virtualCallSites(callSite).contains(instantiatedType.id))
        val typesLeft = _virtualCallSites(callSite) - instantiatedType.id
        if (typesLeft.isEmpty) {
            _virtualCallSites -= callSite
            removePointsToDepender(callSite)
        } else {
            _virtualCallSites(callSite) = typesLeft
        }
    }

    final def hasPointsToDependee(dependee: Entity): Boolean = {
        assert(!_dependeeToDependers.contains(dependee) || _dependeeToDependers(dependee).nonEmpty)
        _pointsToDependees.contains(dependee)
    }

    final def hasPointsToDependency(depender: CallSite, dependee: Entity): Boolean = {
        _dependerToDependees.contains(depender) && _dependerToDependees(depender).contains(dependee)
    }

    final def hasPointsToDependees: Boolean = {
        assert(
            (_pointsToDependees.isEmpty == _dependeeToDependers.isEmpty) &&
                (_dependeeToDependers.isEmpty == _dependerToDependees.isEmpty)
        )
        _pointsToDependees.nonEmpty
    }

    override def hasOpenDependencies: Boolean = {
        hasPointsToDependees || super.hasOpenDependencies
    }

    override def dependees: Set[SomeEOptionP] = {
        // IMPROVE: make it more efficient (maybe use immutable map and join traversables)
        var allDependees = super.dependees

        _pointsToDependees.valuesIterator.foreach { d ⇒
            assert(_dependeeToDependers.contains(d.e))
            allDependees += d
        }

        allDependees
    }

    final def getPointsToProperty(dependee: Entity): EOptionP[Entity, PointsToSet] = {
        _pointsToDependees(dependee)
    }

    final def updatePointsToDependency(eps: EPS[Entity, PointsToSet]): Unit = {
        assert(_pointsToDependees.contains(eps.e))
        _pointsToDependees(eps.e) = eps
    }

    // IMPROVE: In order to be thread-safe, we return an immutable copy of the set.
    // However, this is very inefficient!
    // The size of the sets is typically 1 or 2, but there are outliers with up to 100 elements.
    final def dependersOf(dependee: Entity): Set[CallSite] = {
        _dependeeToDependers(dependee).toSet
    }

    // maps a definition site to the ids of the potential (not yet resolved) objecttypes
    private[this] val _virtualCallSites: mutable.Map[CallSite, IntTrieSet] = mutable.Map.empty

    def typesForCallSite(callSite: CallSite): IntTrieSet = {
        _virtualCallSites(callSite)
    }

    def addPotentialTypesOfCallSite(
        callSite: CallSite, potentialTypes: IntTrieSet
    ): Unit = {
        _virtualCallSites(callSite) =
            _virtualCallSites.getOrElse(callSite, IntTrieSet.empty) ++ potentialTypes
    }

    override def hasNonFinalCallSite: Boolean = _virtualCallSites.nonEmpty
}
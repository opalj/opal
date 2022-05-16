/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package cg

import scala.collection.mutable

import org.opalj.fpcf.Entity
import org.opalj.fpcf.EOptionP
import org.opalj.fpcf.EPK
import org.opalj.fpcf.Property
import org.opalj.fpcf.SomeEOptionP
import org.opalj.fpcf.SomeEPS

/**
 * A trait to implement state classes that have to manage the state of a [[TypeProvider]], i.e.,
 * the dependencies introduced by querying type information.
 *
 * @author Dominik Helm
 */
trait TypeProviderState extends AnalysisState {
    private[this] val _dependees: mutable.Map[EPK[Entity, Property], EOptionP[Entity, Property]] = {
        mutable.Map.empty
    }

    // We organize the dependencies to type states within a bijective mapping of
    // dependers (the use sites) and their dependees (the respective definition sites).
    // For each dependee (def-site) we store the corresponding EOptionP, such that we can
    // efficiently perform updates here.
    // If we get an update for a dependee, we have to update all points-to sets for the
    // its dependers (_dependeeToDependers(dependee).
    private[this] val _dependeeToDependers: mutable.Map[EPK[Entity, Property], mutable.Set[Entity]] = {
        mutable.Map.empty
    }
    private[this] val _dependerToDependees: mutable.Map[Entity, mutable.Set[EPK[Entity, Property]]] = {
        mutable.Map.empty
    }

    final def addDependency(
        depender: Entity,
        dependee: EOptionP[Entity, Property]
    ): Unit = {
        val dependeeEPK = dependee.toEPK

        _dependeeToDependers.getOrElseUpdate(dependeeEPK, mutable.Set.empty).add(depender)
        _dependerToDependees.getOrElseUpdate(depender, mutable.Set.empty).add(dependeeEPK)

        if (!_dependees.contains(dependeeEPK))
            _dependees(dependeeEPK) = dependee
    }

    final def removeDependee(dependee: EPK[Entity, Property]): Unit = {
        // delete the type property of the dependee
        if (_dependees.remove(dependee).isEmpty)
            throw new RuntimeException(s"failed to remove dependee: $dependee")

        // for every depender we have to remove the dependee of the set of dependees
        val dependers = _dependeeToDependers(dependee)
        for (depender <- dependers) {
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

    final def removeDepender(depender: Entity): Unit = {
        // for every dependee of the given depender:
        // we have to remove the depender from the set of their dependers
        for (dependee <- _dependerToDependees(depender)) {
            if (!_dependeeToDependers(dependee).remove(depender)) {
                throw new RuntimeException(s"failed to remove depender: $depender")
            }
            // if there is no depender left for this dependee, we remove the entry from the map
            if (_dependeeToDependers(dependee).isEmpty) {
                removeDependee(dependee)
            }
            assert((!_dependeeToDependers.contains(dependee) && !_dependees.contains(dependee)) ||
                (_dependeeToDependers(dependee).nonEmpty && _dependees.contains(dependee)))
        }

        // now we can delete the depender
        if (_dependerToDependees.remove(depender).isEmpty) {
            throw new RuntimeException(s"failed to remove depender: $depender")
        }
    }

    final def hasDependee(dependee: EPK[Entity, Property]): Boolean = {
        assert(!_dependeeToDependers.contains(dependee) || _dependeeToDependers(dependee).nonEmpty)
        _dependees.contains(dependee)
    }

    final def hasDependency(depender: Entity, dependee: EPK[Entity, Property]): Boolean = {
        _dependerToDependees.contains(depender) && _dependerToDependees(depender).contains(dependee)
    }

    private final def hasDependees: Boolean = {
        assert(
            (_dependees.isEmpty == _dependeeToDependers.isEmpty) &&
                (_dependeeToDependers.isEmpty == _dependerToDependees.isEmpty)
        )
        _dependees.nonEmpty
    }

    abstract override def hasOpenDependencies: Boolean = {
        hasDependees || super.hasOpenDependencies
    }

    abstract override def dependees: Set[SomeEOptionP] = {
        // IMPROVE: make it more efficient (maybe use immutable map and join traversables)
        var allDependees = super.dependees

        _dependees.valuesIterator.foreach { d =>
            assert(_dependeeToDependers.contains(d.toEPK))
            allDependees += d
        }

        allDependees
    }

    final def getProperty[E <: Entity, P <: Property](dependee: EPK[E, P]): EOptionP[E, P] = {
        _dependees(dependee).asInstanceOf[EOptionP[E, P]]
    }

    final def updateDependency(eps: SomeEPS): Unit = {
        val dependeeEPK = eps.toEPK

        assert(_dependees.contains(dependeeEPK))
        _dependees(dependeeEPK) = eps
    }

    // IMPROVE: In order to be thread-safe, we return an immutable copy of the set.
    // However, this is very inefficient!
    // The size of the sets is typically 1 or 2, but there are outliers with up to 100 elements.
    final def dependersOf(dependee: EPK[Entity, Property]): Set[Entity] = {
        _dependeeToDependers(dependee).toSet
    }
}

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
import org.opalj.fpcf.PropertyStore
import org.opalj.fpcf.SomeEOptionP
import org.opalj.value.ValueInformation
import org.opalj.br.DefinedMethod
import org.opalj.br.Method
import org.opalj.br.fpcf.pointsto.properties.PointsTo
import org.opalj.br.ObjectType
import org.opalj.tac.fpcf.properties.TACAI

/**
 * Represents the state of a points-to based call graph analysis, while analyzing a certain method.
 *
 * @author Florian Kuebler
 */
class PointsToBasedCGState(
        override val method:            DefinedMethod,
        private[this] var _tacDependee: EOptionP[Method, TACAI]
) extends CGState {
    // maps a definition site to the ids of the potential (not yet resolved) objecttypes
    private[this] val _virtualCallSites: mutable.Map[CallSiteT, IntTrieSet] = mutable.Map.empty

    // maps a defsite to the callsites for which it is being used
    // this is only done if the defsite is used within a call and its points-to set is not final
    private[this] val _defSitesToCallSites: mutable.Map[Entity, Set[CallSiteT]] = mutable.Map.empty
    private[this] val _callSiteToDefSites: mutable.Map[CallSiteT, Set[Entity]] = mutable.Map.empty

    // maps a defsite to its result in the property store for the points-to set
    private[this] val _pointsToDependees: mutable.Map[Entity, EOptionP[Entity, PointsTo]] = mutable.Map.empty

    override def tac: TACode[TACMethodParameter, DUVar[ValueInformation]] = {
        assert(_tacDependee.ub.tac.isDefined)
        _tacDependee.ub.tac.get
    }

    def updateTACDependee(tacDependee: EOptionP[Method, TACAI]): Unit = {
        _tacDependee = tacDependee
    }

    def virtualCallSites: mutable.Map[CallSiteT, IntTrieSet] = {
        _virtualCallSites
    }

    def typesForCallSite(callSite: CallSiteT): IntTrieSet = {
        _virtualCallSites(callSite)
    }

    def initialPotentialTypesOfCallSite(
        callSite: CallSiteT, potentialTypes: IntTrieSet
    ): Unit = {
        assert(!_virtualCallSites.contains(callSite))
        _virtualCallSites(callSite) = potentialTypes
    }

    def removeTypeForCallSite(callSite: CallSiteT, instantiatedType: ObjectType): Unit = {
        val typesLeft = _virtualCallSites(callSite) - instantiatedType.id
        if (typesLeft.isEmpty) {
            _virtualCallSites -= callSite
            for (defSite ← _callSiteToDefSites(callSite)) {
                val newCallSites = _defSitesToCallSites(defSite) - callSite
                if (newCallSites.isEmpty)
                    removePointsToDependency(defSite)
                else
                    _defSitesToCallSites(defSite) = newCallSites
            }
            _callSiteToDefSites.remove(callSite)
            // todo here we should also remove all dependencies for this call-site
        } else {
            _virtualCallSites(callSite) = typesLeft
        }
    }

    def getOrRetrievePointsToEPS(
        dependee: Entity, ps: PropertyStore
    ): EOptionP[Entity, PointsTo] = {
        _pointsToDependees.getOrElse(dependee, ps(dependee, PointsTo.key))
    }

    def getPointsToEPS(dependee: Entity): EOptionP[Entity, PointsTo] = {
        _pointsToDependees(dependee)
    }

    def updatePointsToDependency(eps: EPS[Entity, PointsTo]): Unit = {
        assert(_pointsToDependees.contains(eps.e))
        _pointsToDependees(eps.e) = eps
    }

    def addPointsToDependency(
        callSite: CallSiteT, pointsToSetEOptP: EOptionP[Entity, PointsTo]
    ): Unit = {
        val defSite = pointsToSetEOptP.e
        assert((!_defSitesToCallSites.contains(defSite) && !_callSiteToDefSites.contains(callSite) && !_pointsToDependees.contains(defSite)) ||
            (!_defSitesToCallSites(defSite).contains(callSite) && !_callSiteToDefSites(callSite).contains(defSite)))
        _pointsToDependees(defSite) = pointsToSetEOptP
        val oldCallSites = _defSitesToCallSites.getOrElse(defSite, Set.empty)
        _defSitesToCallSites(defSite) = oldCallSites + callSite

        val oldDefSites = _callSiteToDefSites.getOrElse(callSite, Set.empty)
        _callSiteToDefSites(callSite) = oldDefSites + defSite
    }

    def removePointsToDependency(defSite: Entity): Unit = {
        assert(_pointsToDependees.contains(defSite))
        assert(_defSitesToCallSites.contains(defSite))
        _pointsToDependees.remove(defSite)
        for (callSite ← _defSitesToCallSites(defSite)) {
            val newDefSites = _callSiteToDefSites(callSite) - defSite
            if (newDefSites.isEmpty) {
                _callSiteToDefSites.remove(callSite)
            } else {
                _callSiteToDefSites(callSite) = newDefSites
            }
        }
        _defSitesToCallSites.remove(defSite)
    }

    def hasOpenDependencies: Boolean = {
        _pointsToDependees.nonEmpty || _tacDependee.isRefinable
    }

    def dependees: Traversable[SomeEOptionP] = {
        if (_tacDependee.isFinal)
            _pointsToDependees.values
        else
            Some(_tacDependee) ++ _pointsToDependees.values
    }

    def callSitesForDefSite(defSite: Entity): Traversable[CallSiteT] = {
        _defSitesToCallSites.getOrElse(defSite, Traversable.empty) // todo: ensure this is required
    }

    override def hasNonFinalCallSite: Boolean = _virtualCallSites.isEmpty
}
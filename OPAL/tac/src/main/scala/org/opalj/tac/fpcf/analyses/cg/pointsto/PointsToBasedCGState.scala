/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package cg
package pointsto

import scala.collection.mutable

import org.opalj.collection.immutable.IntTrieSet
import org.opalj.fpcf.EOptionP
import org.opalj.br.DefinedMethod
import org.opalj.br.Method
import org.opalj.br.ObjectType
import org.opalj.br.fpcf.properties.pointsto.PointsToSetLike
import org.opalj.tac.fpcf.analyses.pointsto.AbstractPointsToState
import org.opalj.tac.fpcf.properties.TACAI

/**
 * Represents the state of a points-to based call graph analysis, while analyzing a certain method.
 *
 * @author Florian Kuebler
 */
class PointsToBasedCGState(
        override val method:                       DefinedMethod,
        override protected[this] var _tacDependee: EOptionP[Method, TACAI]
) extends CGState with AbstractPointsToState[CallSiteT, PointsToSetLike[_, _, _]] {

    // maps a definition site to the ids of the potential (not yet resolved) objecttypes
    private[this] val _virtualCallSites: mutable.Map[CallSiteT, IntTrieSet] = mutable.Map.empty

    def typesForCallSite(callSite: CallSiteT): IntTrieSet = {
        _virtualCallSites(callSite)
    }

    def setPotentialTypesOfCallSite(
        callSite: CallSiteT, potentialTypes: IntTrieSet
    ): Unit = {
        assert(!_virtualCallSites.contains(callSite))
        _virtualCallSites(callSite) = potentialTypes
    }

    def removeTypeForCallSite(callSite: CallSiteT, instantiatedType: ObjectType): Unit = {
        assert(_virtualCallSites(callSite).contains(instantiatedType.id))
        val typesLeft = _virtualCallSites(callSite) - instantiatedType.id
        if (typesLeft.isEmpty) {
            _virtualCallSites -= callSite
            removePointsToDepender(callSite)
        } else {
            _virtualCallSites(callSite) = typesLeft
        }
    }

    override def hasNonFinalCallSite: Boolean = _virtualCallSites.nonEmpty
}
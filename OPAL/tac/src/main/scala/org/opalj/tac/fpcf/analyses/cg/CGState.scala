/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package cg

import scala.collection.mutable

import org.opalj.br.Method
import org.opalj.br.ReferenceType
import org.opalj.br.fpcf.properties.Context
import org.opalj.fpcf.EOptionP
import org.opalj.tac.fpcf.properties.TACAI

/**
 * @author Florian Kuebler
 */
class CGState[TheContextType <: Context](
    val callContext: TheContextType
) extends BaseAnalysisState with TypeIteratorState with ContextualAnalysis {

    override type ContextType = TheContextType

    // maps a definition site to the receiver var
    private[this] val _virtualCallSites: mutable.Map[CallSite, (V, Set[ReferenceType])] =
        mutable.Map.empty

    def callSiteData(callSite: CallSite): (V, Set[ReferenceType]) = {
        _virtualCallSites(callSite)
    }

    def addCallSite(callSite: CallSite, receiver: V, cbsTargets: Set[ReferenceType]): Unit = {
        if (_virtualCallSites.contains(callSite))
            _virtualCallSites.put(callSite, (receiver, _virtualCallSites(callSite)._2 ++ cbsTargets))
        else
            _virtualCallSites.put(callSite, (receiver, cbsTargets))
    }

    def hasNonFinalCallSite: Boolean = _virtualCallSites.nonEmpty
}

/**
 * @author Maximilian Rüsch
 */
class TACAIBasedCGState[ContextType <: Context](
    callContext:                               ContextType,
    override protected[this] var _tacDependee: EOptionP[Method, TACAI]
) extends CGState[ContextType](callContext) with TACAIBasedAnalysisState[ContextType]

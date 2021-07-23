/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package cg

import scala.collection.mutable

import org.opalj.fpcf.EOptionP
import org.opalj.br.Method
import org.opalj.tac.fpcf.properties.TACAI

/**
 * TODO: That state object is currently copy&paste and should be refactored.
 *
 * @author Florian Kuebler
 */
class CGState[ContextType <: Context](
        override val callContext:                  ContextType,
        override protected[this] var _tacDependee: EOptionP[Method, TACAI]
) extends BaseAnalysisState with TypeProviderState with TACAIBasedAnalysisState[ContextType] {

    // maps a definition site to the receiver var
    private[this] val _virtualCallSites: mutable.Map[CallSite, V] = mutable.Map.empty

    def receiverForCallSite(callSite: CallSite): V = {
        _virtualCallSites(callSite)
    }

    def addCallSite(callSite: CallSite, receiver: V): Unit = {
        _virtualCallSites.put(callSite, receiver)
    }

    def hasNonFinalCallSite: Boolean = _virtualCallSites.nonEmpty
}
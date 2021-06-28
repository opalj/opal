/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package cg

import org.opalj.collection.immutable.IntTrieSet
import org.opalj.fpcf.EOptionP
import org.opalj.fpcf.EPS
import org.opalj.br.Method
import org.opalj.br.analyses.SomeProject
import org.opalj.br.DefinedMethod
import org.opalj.br.ObjectType
import org.opalj.tac.fpcf.properties.TACAI

class CHAState(
        val method:                                DefinedMethod,
        override protected[this] var _tacDependee: EOptionP[Method, TACAI]
) extends CGState {

    override def hasNonFinalCallSite: Boolean = false
}

/**
 * A call graph based on Class Hierarchy Analysis (CHA).
 * Virtual calls are resolved to all methods matching the declared type, name and descriptor of the
 * call. Type information from abstract interpretation is taken into account, however.
 *
 * @author Florian Kuebler
 */
class CHACallGraphAnalysis private[analyses] (
        final val project: SomeProject
) extends AbstractCallGraphAnalysis {
    override type State = CHAState
    override type LocalTypeInformation = Null

    override def createInitialState(
        definedMethod: DefinedMethod, tacEP: EPS[Method, TACAI]
    ): CHAState = {
        new CHAState(definedMethod, tacEP)
    }

    @inline override protected[this] def canResolveCall(
        localTypeInformation: LocalTypeInformation,
        state:                CHAState
    ): ObjectType ⇒ Boolean = {
        _ ⇒ true
    }

    @inline protected[this] def handleUnresolvedCall(
        unresolvedTypes: IntTrieSet,
        callSite:        CallSite
    )(implicit state: CHAState): Unit = {
        throw new UnsupportedOperationException()
    }

    @inline protected[this] def getLocalTypeInformation(
        callSite: CallSite, call: Call[V] with VirtualCall[V]
    )(implicit state: CHAState): LocalTypeInformation = null
}

object CHACallGraphAnalysisScheduler extends CallGraphAnalysisScheduler {
    override def initializeAnalysis(p: SomeProject): AbstractCallGraphAnalysis = new CHACallGraphAnalysis(p)
}

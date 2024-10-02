/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package fpcf
package analyses

import org.opalj.br.analyses.DeclaredMethods
import org.opalj.br.analyses.DeclaredMethodsKey
import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.properties.CallStringContext
import org.opalj.br.fpcf.properties.CallStringContexts
import org.opalj.br.fpcf.properties.CallStringContextsKey
import org.opalj.br.fpcf.properties.Context
import org.opalj.br.fpcf.properties.NoContext
import org.opalj.br.fpcf.properties.SimpleContext
import org.opalj.br.fpcf.properties.SimpleContexts
import org.opalj.br.fpcf.properties.SimpleContextsKey

/**
 * Provides analyses with [[Context]]s for method executions.
 */
trait ContextProvider {

    type ContextType <: Context

    val project: SomeProject

    def newContext(method: DeclaredMethod): ContextType

    def expandContext(oldContext: Context, method: DeclaredMethod, pc: Int): ContextType

    def contextFromId(contextId: Int): Context
}

trait SimpleContextProvider extends ContextProvider {

    override type ContextType = SimpleContext

    protected[this] implicit lazy val declaredMethods: DeclaredMethods = project.get(DeclaredMethodsKey)
    private[this] lazy val simpleContexts: SimpleContexts = project.get(SimpleContextsKey)

    @inline def newContext(method: DeclaredMethod): SimpleContext = simpleContexts(method)

    @inline def expandContext(
        oldContext: Context,
        method:     DeclaredMethod,
        pc:         Int
    ): SimpleContext =
        simpleContexts(method)

    @inline def contextFromId(contextId: Int): Context = {
        if (contextId == -1) NoContext
        else simpleContexts(declaredMethods(contextId))
    }
}

trait CallStringContextProvider extends ContextProvider {

    override type ContextType = CallStringContext

    val k: Int

    private[this] lazy val callStringContexts: CallStringContexts = project.get(CallStringContextsKey)

    @inline def newContext(method: DeclaredMethod): CallStringContext =
        callStringContexts(method, Nil)

    @inline override def expandContext(
        oldContext: Context,
        method:     DeclaredMethod,
        pc:         Int
    ): CallStringContext = {
        oldContext match {
            case csc: CallStringContext =>
                callStringContexts(method, (oldContext.method, pc) :: csc.callString.take(k - 1))
            case _ if oldContext.hasContext =>
                callStringContexts(method, List((oldContext.method, pc)))
            case _ =>
                callStringContexts(method, Nil)
        }
    }

    @inline override def contextFromId(contextId: Int): Context = {
        if (contextId == -1) NoContext
        else callStringContexts(contextId)
    }
}

/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package cg

import org.opalj.br.DeclaredMethod

/**
 * Provides the context in which a method was invoked or an object was allocated.
 *
 * @author Dominik Helm
 */
trait Context {
    /** The method itself */
    def method: DeclaredMethod
}

/**
 * A simple context that provides the bare minumum for context-insensitive analyses.
 */
class SimpleContext(val method: DeclaredMethod) extends Context

object NoContext extends Context {
    override def method: DeclaredMethod = throw new UnsupportedOperationException()
}
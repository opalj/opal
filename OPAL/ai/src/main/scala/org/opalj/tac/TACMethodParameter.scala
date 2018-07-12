/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac

import org.opalj.ai.ValueOrigin
import org.opalj.collection.immutable.IntTrieSet

/**
 * Captures essential information about a method's parameter.
 *
 * @author Michael Eichberg
 */
case class TACMethodParameter(origin: ValueOrigin, useSites: IntTrieSet) {

    override def toString: String = useSites.mkString("useSites={", ",", s"} (origin=$origin)")

}


/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package common

import org.opalj.collection.immutable.IntTrieSet
import org.opalj.value.ValueInformation
import org.opalj.br.Method

/**
 * Identifies a definition site object in a method in the bytecode using its program counter.
 * It corresponds to the [[org.opalj.br.fpcf.properties.EscapeProperty]] and the computing
 * analyses.
 * Concrete definition sites are usually associated with [[DefinitionSite]].
 * However, to compute the [[org.opalj.br.fpcf.properties.FieldLocality]] it is necessary to
 * represent definition site objects that have a reduced usedBy set (PutField's are removed here).
 * Therefore, this trait acts as a common interface for such objects.
 *
 * @author Dominik Helm
 * @author Florian Kuebler
 */
trait DefinitionSiteLike {

    val pc: Int

    val method: Method

    def usedBy[V <: ValueInformation](tacode: TACode[TACMethodParameter, DUVar[V]]): IntTrieSet
}

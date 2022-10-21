/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.js

import org.opalj.tac.fpcf.properties.TaintFact

/* Common trait for facts for ScriptEngines. */
trait JSFact extends TaintFact

/**
 * A tainted value inside a Key-Value-Map.
 *
 * @param index variable of a map type
 * @param keyName name of the key. Empty string if unknown.
 */
case class BindingFact(index: Int, keyName: String) extends JSFact with TaintFact

/**
 * A tainted value inside a Key-Value-Map where the value is not statically known.
 *
 * @param index variable of a map type
 */
case class WildcardBindingFact(index: Int) extends JSFact with TaintFact

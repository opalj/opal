/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.python

import org.opalj.tac.fpcf.analyses.ifds.taint.TaintFact

/* Common trait for facts for ScriptEngines. */
trait PythonFact extends TaintFact

/**
 * A tainted value inside a Key-Value-Map.
 *
 * @param index variable of a map type
 * @param keyName name of the key. Empty string if unknown.
 */
case class BindingFact(index: Int, keyName: String) extends PythonFact with TaintFact

/**
 * A tainted value inside a Key-Value-Map where the value is not statically known.
 *
 * @param index variable of a map type
 */
case class WildcardBindingFact(index: Int) extends PythonFact with TaintFact

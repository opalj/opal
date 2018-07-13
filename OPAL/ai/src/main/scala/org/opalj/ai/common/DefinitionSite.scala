/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai
package common

import org.opalj.br.Method
import org.opalj.collection.immutable.IntTrieSet

/**
 * Identifies a definition site object in a method in the bytecode using its program counter and
 * the corresponding use-sites.
 * It acts as entity for the [[org.opalj.fpcf.properties.EscapeProperty]] and the computing
 * analyses.
 * A definition-site can be for example an allocation, the result of a function call, or the result
 * of a field-retrieval.
 *
 * @author Dominik Helm
 * @author Florian Kuebler
 */
case class DefinitionSite(method: Method, pc: Int, usedBy: IntTrieSet) extends DefinitionSiteLike

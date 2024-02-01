/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package collection

/**
 * Describes the relation between two sets.
 *
 * @author Michael Eichberg
 */
sealed abstract class SetRelation

object StrictSubset extends SetRelation
object EqualSets extends SetRelation
object StrictSuperset extends SetRelation
object UncomparableSets extends SetRelation

/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package collection

/**
 * Describes the relation between two sets.
 *
 * @author Michael Eichberg
 */
sealed abstract class SetRelation

final object StrictSubset extends SetRelation
final object EqualSets extends SetRelation
final object StrictSuperset extends SetRelation
final object UncomparableSets extends SetRelation

/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf

/**
 * Hints about the nature of the property computations, which can/are used by the property store
 * to implement different scheduling schemes.
 */
sealed trait PropertyComputationHint

object PropertyComputationHint {

    final val Default = DefaultPropertyComputation
}

/**
 * A standard property computation.
 */
case object DefaultPropertyComputation extends PropertyComputationHint

/**
 * The property computation is extremely cheap (at most a few dozen bytecode operations).
 * Therefore, the property store may process the computation right away, because it is
 * extremely unlikely that we will gain anything from parallelization or postponing
 * the computation.
 *
 * Please note, that even if a computation is very cheap dependees may not be triggered
 * immediately by the property store.
 */
case object CheapPropertyComputation extends PropertyComputationHint

/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf

/**
 * Hints about the nature of the property computations, which can/are used by the property store
 * to implement different scheduling schemes.
 */
sealed trait PropertyComputationHint

/**
 * A standard property computation.
 */
case object DefaultPropertyComputation extends PropertyComputationHint

/**
 * The property computation is extremely cheap. Therefore, the computation can/should be
 * processed right away, because it is extremely unlikely that we will gain anything
 * from parallelization or postponing the computation.
 * Here, `cheap` means that the computation requires ''at most a few hundred bytecode instructions''
 * overall and that the computation in particular '''will not query further properties'''.
 */
case object CheapPropertyComputation extends PropertyComputationHint

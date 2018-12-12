/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf

/**
 * Determines how and when an analysis is executed.
 */
sealed trait ComputationType
case object LazyComputation extends ComputationType
case object EagerComputation extends ComputationType
case object TriggeredComputation extends ComputationType
case object Transformer extends ComputationType

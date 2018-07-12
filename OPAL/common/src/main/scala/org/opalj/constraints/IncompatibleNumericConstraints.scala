/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package constraints

/**
 * Raised if two constraints should be combined that are incompatible. E.g.,
 * `a > b` and `a < b` are incompatible.
 *
 * @author Michael Eichberg
 */
case class IncompatibleNumericConstraints(
        message:            String,
        constraint1:        NumericConstraint,
        constraint2:        NumericConstraint,
        enableSuppression:  Boolean           = false,
        writableStackTrace: Boolean           = true
) extends RuntimeException(
    s"$message (incompatible: $constraint1 and $constraint2)",
    /*cause = */ null, enableSuppression, writableStackTrace
)

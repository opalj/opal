/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai
package domain

/**
 * Records the results of the evaluation of the `current` method such that the results
 * can directly be adapted to the calling context and can be used by the caller to continue
 * the abstract interpretation of the calling method.
 *
 * @author Michael Eichberg
 */
trait DefaultRecordMethodCallResults
    extends RecordMethodCallResults
    with RecordLastReturnedValues
    with RecordAllThrownExceptions {
    this: Domain =>

}

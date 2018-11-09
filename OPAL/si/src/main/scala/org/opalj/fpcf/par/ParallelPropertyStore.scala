/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package par

/**
 * A parallel implementation of the property store.
 *
 * @author Michael Eichberg
 */
abstract class ParallelPropertyStore extends PropertyStore { store ⇒

    // --------------------------------------------------------------------------------------------
    //
    // CONFIGURATION OPTIONS
    //
    // --------------------------------------------------------------------------------------------

    def NumberOfThreadsForProcessingPropertyComputations: Int

}

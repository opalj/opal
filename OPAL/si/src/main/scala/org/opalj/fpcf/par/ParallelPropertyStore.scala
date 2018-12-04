/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package par

import org.opalj.concurrent.NumberOfThreadsForCPUBoundTasks

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

trait ParallelPropertyStoreFactory extends PropertyStoreFactory {

    @volatile var NumberOfThreadsForProcessingPropertyComputations: Int = {
        // We need at least one thread for processing property computations.
        Math.max(NumberOfThreadsForCPUBoundTasks, 1)
    }

}

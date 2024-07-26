/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.ide.integration

import org.opalj.fpcf.PropertyMetaInformation
import org.opalj.ide.problem.IDEFact
import org.opalj.ide.problem.IDEValue

/**
 * Base interface of property meta information of IDE analyses. Creates [[BasicIDEProperty]] by default.
 */
trait IDEPropertyMetaInformation[Fact <: IDEFact, Value <: IDEValue] extends PropertyMetaInformation {
    override type Self = BasicIDEProperty[Fact, Value]

    def createProperty(
        results: collection.Set[(Fact, Value)]
    ): IDEProperty[Fact, Value] = {
        new BasicIDEProperty(results, this)
    }
}

/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.ide.integration

import org.opalj.fpcf.PropertyMetaInformation
import org.opalj.ide.problem.IDEFact
import org.opalj.ide.problem.IDEValue

/**
 * Base interface of property meta information of IDE analyses. Creates [[BasicIDEProperty]] by default.
 */
trait IDEPropertyMetaInformation[Statement, Fact <: IDEFact, Value <: IDEValue] extends PropertyMetaInformation {
    override type Self = BasicIDEProperty[Statement, Fact, Value]

    def createProperty(
        results: collection.Map[Statement, collection.Set[(Fact, Value)]]
    ): IDEProperty[Statement, Fact, Value] = {
        new BasicIDEProperty(results, this)
    }
}

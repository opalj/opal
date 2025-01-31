/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.ide.integration

import org.opalj.fpcf.Entity
import org.opalj.fpcf.PropertyMetaInformation
import org.opalj.ide.problem.IDEFact
import org.opalj.ide.problem.IDEValue

/**
 * Base interface of property meta information of IDE analyses. Creates [[BasicIDEProperty]] by default.
 */
trait IDEPropertyMetaInformation[Fact <: IDEFact, Value <: IDEValue, Statement, Callable <: Entity]
    extends PropertyMetaInformation {
    override type Self = BasicIDEProperty[Fact, Value]

    /**
     * A property meta information corresponding to this one but used for the actual/underlying IDE analysis
     */
    private[ide] val backingPropertyMetaInformation: IDERawPropertyMetaInformation[Fact, Value, Statement] =
        new IDERawPropertyMetaInformation[Fact, Value, Statement](this)

    /**
     * Create a property
     * @param results the results the property should represent
     */
    def createProperty(
        results: collection.Set[(Fact, Value)]
    ): IDEProperty[Fact, Value] = {
        new BasicIDEProperty(key, results)
    }
}

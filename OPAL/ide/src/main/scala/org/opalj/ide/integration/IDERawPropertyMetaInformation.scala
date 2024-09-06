/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.ide.integration

import org.opalj.fpcf.PropertyKey
import org.opalj.fpcf.PropertyMetaInformation
import org.opalj.ide.problem.IDEFact
import org.opalj.ide.problem.IDEValue

/**
 * Class for property meta information for properties that are created by IDE analyses directly (also called 'raw').
 * The property type is fixed to [[IDERawProperty]].
 * @param propertyMetaInformation the property meta information this object should be backing
 */
final class IDERawPropertyMetaInformation[Fact <: IDEFact, Value <: IDEValue](
    propertyMetaInformation: IDEPropertyMetaInformation[Fact, Value]
) extends PropertyMetaInformation {
    override type Self = IDERawProperty[Fact, Value]

    /**
     * The used property key, based on [[propertyMetaInformation]]
     */
    private lazy val backingPropertyKey: PropertyKey[IDERawProperty[Fact, Value]] = {
        PropertyKey.create(s"${PropertyKey.name(propertyMetaInformation.key)}_Raw")
    }

    override def key: PropertyKey[IDERawProperty[Fact, Value]] = backingPropertyKey
}

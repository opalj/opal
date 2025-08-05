/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ide
package integration

import org.opalj.fpcf.PropertyKey
import org.opalj.fpcf.PropertyMetaInformation
import org.opalj.ide.problem.IDEFact
import org.opalj.ide.problem.IDEValue

/**
 * Class for property meta information for properties that are created by IDE analyses directly (also called 'raw').
 * The property type is fixed to [[IDERawProperty]].
 *
 * @param propertyMetaInformation the property meta information this object should be backing
 *
 * @author Robin Körkemeier
 */
final class IDERawPropertyMetaInformation[Fact <: IDEFact, Value <: IDEValue, Statement](
    propertyMetaInformation: IDEPropertyMetaInformation[Fact, Value, Statement, ?]
) extends PropertyMetaInformation {
    override type Self = IDERawProperty[Fact, Value, Statement]

    /**
     * The used property key, based on [[propertyMetaInformation]]
     */
    private lazy val propertyKey: PropertyKey[IDERawProperty[Fact, Value, Statement]] = {
        PropertyKey.create(s"${PropertyKey.name(propertyMetaInformation.key)}_Raw")
    }

    override def key: PropertyKey[IDERawProperty[Fact, Value, Statement]] = propertyKey
}

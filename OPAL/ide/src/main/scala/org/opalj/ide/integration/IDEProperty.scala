/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.ide.integration

import org.opalj.fpcf.Property
import org.opalj.fpcf.PropertyKey
import org.opalj.ide.problem.IDEFact
import org.opalj.ide.problem.IDEValue

/**
 * Base class of properties that are produced by an IDE analysis
 */
abstract class IDEProperty[Fact <: IDEFact, Value <: IDEValue] extends Property
    with IDEPropertyMetaInformation[Fact, Value]

/**
 * Basic implementation of [[IDEProperty]] that simply wraps the fact-value results of an IDE analysis
 * @param results the results produced by the analysis
 * @param propertyMetaInformation corresponding to the produced property
 */
class BasicIDEProperty[Fact <: IDEFact, Value <: IDEValue](
    val results:                 collection.Set[(Fact, Value)],
    val propertyMetaInformation: IDEPropertyMetaInformation[Fact, Value]
) extends IDEProperty[Fact, Value] {

    override type Self = propertyMetaInformation.Self

    override def key: PropertyKey[Self] = propertyMetaInformation.key

    override def toString: String = {
        s"${PropertyKey.name(propertyMetaInformation.key)}:\n${
                results.map { case (fact, value) => s"\t($fact,$value)" }.mkString("\n")
            }"
    }

    override def equals(obj: Any): Boolean = {
        obj match {
            case basicIDEProperty: BasicIDEProperty[?, ?] =>
                results == basicIDEProperty.results && propertyMetaInformation == basicIDEProperty.propertyMetaInformation
            case _ => false
        }
    }

    override def hashCode(): Int = {
        results.hashCode() * 31 + propertyMetaInformation.hashCode()
    }
}

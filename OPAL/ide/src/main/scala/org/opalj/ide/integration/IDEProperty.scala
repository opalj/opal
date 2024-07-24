/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.ide.integration

import org.opalj.fpcf.Property
import org.opalj.fpcf.PropertyKey
import org.opalj.ide.problem.IDEFact
import org.opalj.ide.problem.IDEValue

/**
 * Base class of properties that are produced by an IDE analysis
 */
abstract class IDEProperty[Statement, Fact <: IDEFact, Value <: IDEValue] extends Property
    with IDEPropertyMetaInformation[Statement, Fact, Value]

/**
 * Basic implementation of [[IDEProperty]] that simply wraps the fact-value results of an IDE analysis
 * @param results the results produced by the analysis
 * @param propertyMetaInformation corresponding to the produced property
 */
class BasicIDEProperty[Statement, Fact <: IDEFact, Value <: IDEValue](
        val results:                 collection.Map[Statement, collection.Set[(Fact, Value)]],
        val propertyMetaInformation: IDEPropertyMetaInformation[Statement, Fact, Value]
) extends IDEProperty[Statement, Fact, Value] {

    override type Self = propertyMetaInformation.Self

    override def key: PropertyKey[Self] = propertyMetaInformation.key

    override def toString: String = {
        s"${PropertyKey.name(propertyMetaInformation.key)}:\n${results.map { case (stmt, result) =>
                s"\t$stmt:\n\t\t${result.map { case (fact, value) => s"($fact,$value)" }.mkString("\n\t\t")}"
            }.mkString("\n")}"
    }

    override def equals(obj: Any): Boolean = {
        obj match {
            case basicIDEProperty: BasicIDEProperty[?, ?, ?] =>
                results == basicIDEProperty.results && propertyMetaInformation == basicIDEProperty.propertyMetaInformation
            case _ => false
        }
    }

    override def hashCode(): Int = {
        results.hashCode() * 31 + propertyMetaInformation.hashCode()
    }
}

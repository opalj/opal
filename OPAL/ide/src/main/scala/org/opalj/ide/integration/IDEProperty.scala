/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ide
package integration

import scala.collection

import org.opalj.fpcf.Property
import org.opalj.fpcf.PropertyKey
import org.opalj.ide.problem.IDEFact
import org.opalj.ide.problem.IDEValue

/**
 * Base interface of properties that are produced by an IDE analysis
 */
trait IDEProperty[Fact <: IDEFact, Value <: IDEValue] extends Property

/**
 * Basic implementation of [[IDEProperty]] that simply wraps the fact-value results of an IDE analysis
 * @param key the property key
 * @param results the results produced by the analysis
 */
class BasicIDEProperty[Fact <: IDEFact, Value <: IDEValue](
    val key:     PropertyKey[BasicIDEProperty[Fact, Value]],
    val results: collection.Set[(Fact, Value)]
) extends IDEProperty[Fact, Value] {
    override type Self = BasicIDEProperty[Fact, Value]

    override def toString: String = {
        s"BasicIDEProperty(${PropertyKey.name(key)}, {\n${
                results.map { case (fact, value) => s"\t($fact,$value)" }.toList.sorted.mkString("\n")
            }\n})"
    }

    override def equals(other: Any): Boolean = {
        other match {
            case basicIDEProperty: BasicIDEProperty[?, ?] =>
                key == basicIDEProperty.key && results == basicIDEProperty.results
            case _ => false
        }
    }

    override def hashCode(): Int = {
        key.hashCode() * 31 + results.hashCode()
    }
}

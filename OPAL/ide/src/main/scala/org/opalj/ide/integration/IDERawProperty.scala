/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.ide.integration

import org.opalj.fpcf.Property
import org.opalj.fpcf.PropertyKey
import org.opalj.ide.problem.IDEFact
import org.opalj.ide.problem.IDEValue

/**
 * Class representing a property that is directly created by an IDE analysis.
 * @param key the property key (very likely taken from an [[IDERawPropertyMetaInformation]] instance)
 * @param results the raw results produced by the analysis
 */
class IDERawProperty[Fact <: IDEFact, Value <: IDEValue](
    val key:     PropertyKey[IDERawProperty[Fact, Value]],
    val results: collection.Set[(Fact, Value)]
) extends Property {
    override type Self = IDERawProperty[Fact, Value]

    override def toString: String = {
        s"IDERawProperty(${PropertyKey.name(key)}, {\n${
                results.map { case (fact, value) => s"\t($fact,$value)" }.mkString("\n")
            }\n})"
    }

    override def equals(other: Any): Boolean = {
        other match {
            case ideRawProperty: IDERawProperty[?, ?] =>
                key == ideRawProperty.key && results == ideRawProperty.results
            case _ => false
        }
    }

    override def hashCode(): Int = {
        key.hashCode() * 31 + results.hashCode()
    }
}

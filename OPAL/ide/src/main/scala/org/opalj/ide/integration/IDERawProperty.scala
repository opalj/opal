/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.ide.integration

import org.opalj.fpcf.Property
import org.opalj.fpcf.PropertyKey
import org.opalj.ide.problem.IDEFact
import org.opalj.ide.problem.IDEValue

/**
 * Class representing a property that is directly created by an IDE analysis.
 * @param key the property key (very likely taken from an [[IDERawPropertyMetaInformation]] instance)
 * @param stmtResults the raw statement results produced by the analysis
 * @param callableResults the raw callable results produced by the analysis
 */
class IDERawProperty[Fact <: IDEFact, Value <: IDEValue, Statement](
    val key:             PropertyKey[IDERawProperty[Fact, Value, Statement]],
    val stmtResults:     collection.Map[Statement, collection.Set[(Fact, Value)]],
    val callableResults: collection.Set[(Fact, Value)]
) extends Property {
    override type Self = IDERawProperty[Fact, Value, Statement]

    override def toString: String = {
        s"IDERawProperty(${PropertyKey.name(key)}, {\n${
                stmtResults.map { case (stmt, results) =>
                    s"\t$stmt\n${
                            results.map { case (fact, value) => s"\t\t($fact,$value)" }.mkString("\n")
                        }"
                }.mkString("\n")
            }\n}, {\n${
                callableResults.map { case (fact, value) => s"\t($fact,$value)" }.mkString("\n")
            }\n})"
    }

    override def equals(other: Any): Boolean = {
        other match {
            case ideRawProperty: IDERawProperty[?, ?, ?] =>
                key == ideRawProperty.key && stmtResults == ideRawProperty.stmtResults &&
                    callableResults == ideRawProperty.callableResults
            case _ => false
        }
    }

    override def hashCode(): Int = {
        (key.hashCode() * 31 + stmtResults.hashCode()) * 31 + callableResults.hashCode()
    }
}

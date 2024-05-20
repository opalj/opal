/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package properties
package string

import org.opalj.br.fpcf.properties.string.StringTreeDynamicString
import org.opalj.br.fpcf.properties.string.StringTreeNode
import org.opalj.br.fpcf.properties.string.StringTreeOr

/**
 * @author Maximilian Rüsch
 */
case class StringTreeEnvironment(private val map: Map[PV, StringTreeNode]) {

    def apply(v: PV): StringTreeNode = map(v)

    def update(v: PV, value: StringTreeNode): StringTreeEnvironment = StringTreeEnvironment(map.updated(v, value))

    def updateAll(value: StringTreeNode): StringTreeEnvironment = StringTreeEnvironment(map.map { kv => (kv._1, value) })

    def join(other: StringTreeEnvironment): StringTreeEnvironment = {
        val result = (map.keySet ++ other.map.keys) map { pv =>
            (map.get(pv), other.map.get(pv)) match {
                case (Some(value1), Some(value2)) => pv -> StringTreeOr.fromNodes(value1, value2)
                case (Some(value1), None)         => pv -> value1
                case (None, Some(value2))         => pv -> value2
                case (None, None)                 => throw new IllegalStateException("Found a key in a map that is not in the map!")
            }
        }

        StringTreeEnvironment(result.toMap)
    }
}

object StringTreeEnvironment {
    def lb: StringTreeEnvironment =
        StringTreeEnvironment(Map.empty[PV, StringTreeNode].withDefaultValue(StringTreeDynamicString))
}

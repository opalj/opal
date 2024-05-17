/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package properties
package string

import org.opalj.br.fpcf.properties.string.StringTreeDynamicString
import org.opalj.br.fpcf.properties.string.StringTreeNode

/**
 * @author Maximilian Rüsch
 */
case class StringTreeEnvironment(private val map: Map[PV, StringTreeNode]) {

    def apply(v: PV): StringTreeNode = map(v)

    def update(v: PV, value: StringTreeNode): StringTreeEnvironment = StringTreeEnvironment(map.updated(v, value))

    def updateAll(value: StringTreeNode): StringTreeEnvironment = StringTreeEnvironment(map.map { kv => (kv._1, value) })

    def join(other: StringTreeEnvironment): StringTreeEnvironment = {
        this // TODO implement join here
    }
}

object StringTreeEnvironment {
    def lb: StringTreeEnvironment =
        StringTreeEnvironment(Map.empty[PV, StringTreeNode].withDefaultValue(StringTreeDynamicString))
}

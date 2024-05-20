/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package properties
package string

import org.opalj.br.fpcf.properties.string.StringTreeNode
import org.opalj.br.fpcf.properties.string.StringTreeOr

/**
 * @author Maximilian Rüsch
 */
case class StringTreeEnvironment(private val map: Map[PDUWeb, StringTreeNode]) {

    private def getWebFor(pc: Int, pv: PV): PDUWeb = map.keys.find(_.containsVarAt(pc, pv)).getOrElse(PDUWeb(pc, pv))

    def apply(pc: Int, pv: PV): StringTreeNode = map(getWebFor(pc, pv))

    def apply(web: PDUWeb): StringTreeNode = map(web)

    def update(web: PDUWeb, value: StringTreeNode): StringTreeEnvironment =
        StringTreeEnvironment(map.updated(web, value))

    def update(pc: Int, pv: PV, value: StringTreeNode): StringTreeEnvironment =
        StringTreeEnvironment(map.updated(getWebFor(pc, pv), value))

    def updateAll(value: StringTreeNode): StringTreeEnvironment = StringTreeEnvironment(map.map { kv => (kv._1, value) })

    def join(other: StringTreeEnvironment): StringTreeEnvironment = {
        val result = (map.keySet ++ other.map.keys) map { web =>
            (map.get(web), other.map.get(web)) match {
                case (Some(value1), Some(value2)) => web -> StringTreeOr.fromNodes(value1, value2)
                case (Some(value1), None)         => web -> value1
                case (None, Some(value2))         => web -> value2
                case (None, None)                 => throw new IllegalStateException("Found a key in a map that is not in the map!")
            }
        }

        StringTreeEnvironment(result.toMap)
    }
}

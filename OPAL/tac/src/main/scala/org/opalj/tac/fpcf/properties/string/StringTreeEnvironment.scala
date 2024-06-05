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

    private def getWebsFor(pc: Int, pv: PV): Seq[PDUWeb] = map.keys.toList
        .filter(_.containsVarAt(pc, pv))
        .sortBy(_.defPCs.toList.min)

    private def getWebsFor(web: PDUWeb): Seq[PDUWeb] = map.keys.toList
        .filter(_.identifiesSameVarAs(web))
        .sortBy(_.defPCs.toList.min)

    private def getWebFor(pc: Int, pv: PV): PDUWeb = {
        val webs = getWebsFor(pc, pv)
        webs.size match {
            case 0 => PDUWeb(pc, pv)
            case 1 => webs.head
            case _ => throw new IllegalStateException("Found more than one matching web when only one should be given!")
        }
    }

    private def getWebFor(web: PDUWeb): PDUWeb = {
        val webs = getWebsFor(web)
        webs.size match {
            case 0 => web
            case 1 => webs.head
            case _ => throw new IllegalStateException("Found more than one matching web when only one should be given!")
        }
    }

    def mergeAllMatching(pc: Int, pv: PV): StringTreeNode = StringTreeOr.fromNodes(getWebsFor(pc, pv).map(map(_)): _*)

    def apply(pc: Int, pv: PV): StringTreeNode = map(getWebFor(pc, pv))

    def apply(web: PDUWeb): StringTreeNode = map(getWebFor(web))

    def update(web: PDUWeb, value: StringTreeNode): StringTreeEnvironment =
        StringTreeEnvironment(map.updated(getWebFor(web), value))

    def update(pc: Int, pv: PV, value: StringTreeNode): StringTreeEnvironment =
        StringTreeEnvironment(map.updated(getWebFor(pc, pv), value))

    def updateAll(value: StringTreeNode): StringTreeEnvironment = StringTreeEnvironment(map.map { kv => (kv._1, value) })

    def join(other: StringTreeEnvironment): StringTreeEnvironment = {
        val (smallMap, bigMap) = if (map.size < other.map.size) (map, other.map) else (other.map, map)

        StringTreeEnvironment {
            bigMap.toList.concat(smallMap.toList).groupBy(_._1).map {
                case (k, vs) if vs.take(2).size == 1 => (k, vs.head._2)
                case (k, vs)                         => (k, StringTreeOr.fromNodes(vs.map(_._2): _*))
            }
        }
    }

    def joinMany(others: Iterable[StringTreeEnvironment]): StringTreeEnvironment = {
        if (others.isEmpty) {
            this
        } else {
            StringTreeEnvironment {
                map.toList.concat(others.flatMap(_.map.toList)).groupBy(_._1).map {
                    case (k, vs) if vs.take(2).size == 1 => (k, vs.head._2)
                    case (k, vs)                         => (k, StringTreeOr.fromNodes(vs.map(_._2): _*))
                }
            }
        }
    }
}

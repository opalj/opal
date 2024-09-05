/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package properties
package string

import org.opalj.br.fpcf.properties.string.SetBasedStringTreeOr
import org.opalj.br.fpcf.properties.string.StringTreeNode
import org.opalj.br.fpcf.properties.string.StringTreeOr

/**
 * @author Maximilian Rüsch
 */
case class StringTreeEnvironment private (
    private val map:      Map[PDUWeb, StringTreeNode],
    private val pcToWebs: Map[Int, Seq[PDUWeb]]
) {

    private def getWebsFor(pc: Int, pv: PV): Seq[PDUWeb] = {
        val webs = pv match {
            case _: PDVar[_] => pcToWebs(pc)
            case _: PUVar[_] => pv.defPCs.map(pcToWebs).toSeq.flatten
        }

        webs.sortBy(_.defPCs.toList.toSet.min)
    }

    private def getWebsFor(web: PDUWeb): Seq[PDUWeb] = map.keys
        .filter(_.identifiesSameVarAs(web))
        .toSeq
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
        recreate(map.updated(getWebFor(web), value))

    def update(pc: Int, pv: PV, value: StringTreeNode): StringTreeEnvironment =
        recreate(map.updated(getWebFor(pc, pv), value))

    def updateAll(value: StringTreeNode): StringTreeEnvironment = recreate(map.transform((_, _) => value))

    def join(other: StringTreeEnvironment): StringTreeEnvironment = {
        recreate(map.transform { (web, tree) => SetBasedStringTreeOr.createWithSimplify(Set(tree, other.map(web))) })
    }

    def recreate(newMap: Map[PDUWeb, StringTreeNode]): StringTreeEnvironment = StringTreeEnvironment(newMap, pcToWebs)
}

object StringTreeEnvironment {

    def apply(map: Map[PDUWeb, StringTreeNode]): StringTreeEnvironment = {
        val pcToWebs =
            map.keys.toSeq.flatMap(web => web.defPCs.map((_, web))).groupMap(_._1)(_._2)
                .withDefaultValue(Seq.empty)
        StringTreeEnvironment(map, pcToWebs)
    }

    def joinMany(envs: Iterable[StringTreeEnvironment]): StringTreeEnvironment = {
        envs.size match {
            case 0 => throw new IllegalArgumentException("Cannot join zero environments!")
            case 1 => envs.head
            case _ =>
                val head = envs.head
                // This only works as long as environment maps are not sparse
                head.recreate(head.map.transform { (web, _) =>
                    SetBasedStringTreeOr.createWithSimplify(envs.map(_.map(web)).toSet)
                })
        }
    }
}

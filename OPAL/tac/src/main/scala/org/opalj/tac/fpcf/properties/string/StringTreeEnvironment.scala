/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package properties
package string

import org.opalj.br.PDVar
import org.opalj.br.PUVar
import org.opalj.br.fpcf.properties.string.SetBasedStringTreeOr
import org.opalj.br.fpcf.properties.string.StringTreeNode

/**
 * A mapping from [[PDUWeb]] to [[StringTreeNode]], used to identify the state of string variables during a given fixed
 * point of the [[org.opalj.tac.fpcf.analyses.string.flowanalysis.DataFlowAnalysis]].
 *
 * @author Maximilian Rüsch
 */
case class StringTreeEnvironment private (
    private val map:      Map[PDUWeb, StringTreeNode],
    private val pcToWebs: Map[Int, Set[PDUWeb]]
) {

    private def getWebsFor(pc: Int, pv: PV): Set[PDUWeb] = {
        pv match {
            case _: PDVar[_] => pcToWebs(pc)
            case _: PUVar[_] => pv.defPCs.map(pcToWebs).flatten
        }
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

    def mergeAllMatching(pc: Int, pv: PV): StringTreeNode =
        SetBasedStringTreeOr.createWithSimplify(getWebsFor(pc, pv).map(map(_)))

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
            map.keySet.flatMap(web => web.defPCs.map((_, web))).groupMap(_._1)(_._2)
                .withDefaultValue(Set.empty)
        StringTreeEnvironment(map, pcToWebs)
    }

    def joinMany(envs: Iterable[StringTreeEnvironment]): StringTreeEnvironment = {
        // IMPROVE as environments get larger (with growing variable count in a method) joining them impacts performance
        // especially when analysing flow through proper regions. This could be improved by detecting and joining only
        // changes between multiple environments, e.g. with linked lists.
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

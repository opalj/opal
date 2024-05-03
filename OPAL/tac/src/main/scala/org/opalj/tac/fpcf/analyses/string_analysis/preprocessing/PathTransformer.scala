/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package string_analysis
package preprocessing

import org.opalj.br.fpcf.properties.StringConstancyProperty
import org.opalj.br.fpcf.properties.string_definition.StringConstancyInformation
import org.opalj.br.fpcf.properties.string_definition.StringTreeConcat
import org.opalj.br.fpcf.properties.string_definition.StringTreeDynamicString
import org.opalj.br.fpcf.properties.string_definition.StringTreeNeutralElement
import org.opalj.br.fpcf.properties.string_definition.StringTreeNode
import org.opalj.br.fpcf.properties.string_definition.StringTreeOr
import org.opalj.fpcf.PropertyStore
import org.opalj.fpcf.UBP
import org.opalj.tac.fpcf.analyses.string_analysis.interpretation.InterpretationHandler

/**
 * Transforms a [[Path]] into a string tree of [[StringTreeNode]]s.
 *
 * @author Maximilian Rüsch
 */
object PathTransformer {

    private def pathToTreeAcc(subpath: SubPath)(implicit
        state: ComputationState,
        ps:    PropertyStore
    ): Option[StringTreeNode] = {
        subpath match {
            case fpe: FlatPathElement =>
                val sci = ps(
                    InterpretationHandler.getEntityForPC(fpe.pc, state.dm, state.tac, state.entity._1),
                    StringConstancyProperty.key
                ) match {
                    case UBP(scp) => scp.sci
                    case _        => StringConstancyInformation.lb
                }
                Option.unless(sci.isTheNeutralElement)(sci.tree)
            case _ => None
        }
    }

    def pathsToStringTree(paths: Seq[Path])(implicit
        state: ComputationState,
        ps:    PropertyStore
    ): StringTreeNode = {
        if (paths.isEmpty) {
            StringTreeNeutralElement
        } else {
            val nodes = paths.map { path =>
                path.elements.size match {
                    case 1 =>
                        pathToTreeAcc(path.elements.head).getOrElse(StringTreeDynamicString)
                    case _ =>
                        StringTreeConcat(path.elements.flatMap(pathToTreeAcc(_)))
                }
            }

            StringTreeOr(nodes)
        }
    }
}

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
import org.opalj.br.fpcf.properties.string_definition.StringTreeCond
import org.opalj.br.fpcf.properties.string_definition.StringTreeDynamicString
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

    /**
     * Accumulator function for transforming a path into a StringTree element.
     */
    private def pathToTreeAcc(subpath: SubPath)(implicit
        state: ComputationState,
        ps:    PropertyStore
    ): Option[StringTreeNode] = {
        subpath match {
            case fpe: FlatPathElement =>
                val sci = ps(
                    InterpretationHandler.getEntityForPC(fpe.pc, state.dm, state.tac),
                    StringConstancyProperty.key
                ) match {
                    case UBP(scp) => scp.sci
                    case _        => StringConstancyInformation.lb
                }
                Option.unless(sci.isTheNeutralElement)(sci.tree)
            case npe: NestedPathElement =>
                if (npe.elementType.isDefined) {
                    npe.elementType.get match {
                        case _ =>
                            val processedSubPaths = npe.element.flatMap { ne => pathToTreeAcc(ne) }
                            if (processedSubPaths.nonEmpty) {
                                npe.elementType.get match {
                                    case NestedPathType.CondWithAlternative =>
                                        if (npe.element.size == processedSubPaths.size) {
                                            Some(StringTreeOr(processedSubPaths))
                                        } else {
                                            Some(StringTreeCond(StringTreeOr(processedSubPaths)))
                                        }
                                    case _ => None
                                }
                            } else {
                                None
                            }
                    }
                } else {
                    npe.element.size match {
                        case 0 => None
                        case 1 => pathToTreeAcc(npe.element.head)
                        case _ =>
                            val processed = npe.element.flatMap { ne => pathToTreeAcc(ne) }
                            if (processed.isEmpty) {
                                None
                            } else {
                                Some(StringTreeConcat(processed))
                            }
                    }
                }
            case _ => None
        }
    }

    /**
     * Takes a [[Path]] and transforms it into a [[StringTreeNode]]. This implies an interpretation of
     * how to handle methods called on the object of interest (like `append`).
     *
     * @param path             The path element to be transformed.
     * @return If an empty [[Path]] is given, `None` will be returned. Otherwise, the transformed
     *         [[StringTreeNode]] will be returned. Note that
     *         all elements of the tree will be defined, i.e., if `path` contains sites that could
     *         not be processed (successfully), they will not occur in the tree.
     */
    def pathToStringTree(path: Path)(implicit
        state: ComputationState,
        ps:    PropertyStore
    ): StringTreeNode = {
        path.elements.size match {
            case 1 =>
                pathToTreeAcc(path.elements.head).getOrElse(StringTreeDynamicString)
            case _ =>
                val children = path.elements.flatMap { pathToTreeAcc(_) }
                if (children.size == 1) {
                    // The concatenation of one child is the child itself
                    children.head
                } else {
                    StringTreeConcat(children)
                }
        }
    }
}

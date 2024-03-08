/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package string_analysis
package preprocessing

import scala.collection.mutable.ListBuffer

import org.opalj.br.fpcf.properties.StringConstancyProperty
import org.opalj.br.fpcf.properties.string_definition.StringConstancyInformation
import org.opalj.br.fpcf.properties.string_definition.StringTree
import org.opalj.br.fpcf.properties.string_definition.StringTreeConcat
import org.opalj.br.fpcf.properties.string_definition.StringTreeCond
import org.opalj.br.fpcf.properties.string_definition.StringTreeConst
import org.opalj.br.fpcf.properties.string_definition.StringTreeOr
import org.opalj.br.fpcf.properties.string_definition.StringTreeRepetition
import org.opalj.fpcf.FinalP
import org.opalj.fpcf.PropertyStore
import org.opalj.tac.fpcf.analyses.string_analysis.interpretation.InterpretationHandler

/**
 * Transforms a [[Path]] into a [[org.opalj.br.fpcf.properties.string_definition.StringTree]].
 *
 * @author Maximilian Rüsch
 */
object PathTransformer {

    /**
     * Accumulator function for transforming a path into a StringTree element.
     */
    private def pathToTreeAcc[State <: ComputationState](subpath: SubPath)(implicit
        state: State,
        ps:    PropertyStore
    ): Option[StringTree] = {
        subpath match {
            case fpe: FlatPathElement =>
                val sci = ps(InterpretationHandler.getEntityFromDefSitePC(fpe.pc), StringConstancyProperty.key) match {
                    case FinalP(scp) => scp.sci
                    case _           => StringConstancyInformation.lb
                }
                Option.unless(sci.isTheNeutralElement)(StringTreeConst(sci))
            case npe: NestedPathElement =>
                if (npe.elementType.isDefined) {
                    npe.elementType.get match {
                        case NestedPathType.Repetition =>
                            val processedSubPath = pathToStringTree(Path(npe.element.toList))
                            Some(StringTreeRepetition(processedSubPath))
                        case _ =>
                            val processedSubPaths = npe.element.flatMap { ne => pathToTreeAcc(ne) }
                            if (processedSubPaths.nonEmpty) {
                                npe.elementType.get match {
                                    case NestedPathType.TryCatchFinally =>
                                        // In case there is only one element in the sub path, transform it into a
                                        // conditional element (as there is no alternative)
                                        if (processedSubPaths.tail.nonEmpty) {
                                            Some(StringTreeOr(processedSubPaths))
                                        } else {
                                            Some(StringTreeCond(processedSubPaths.head))
                                        }
                                    case NestedPathType.SwitchWithDefault |
                                        NestedPathType.CondWithAlternative =>
                                        if (npe.element.size == processedSubPaths.size) {
                                            Some(StringTreeOr(processedSubPaths))
                                        } else {
                                            Some(StringTreeCond(StringTreeOr(processedSubPaths)))
                                        }
                                    case NestedPathType.SwitchWithoutDefault =>
                                        Some(StringTreeCond(StringTreeOr(processedSubPaths)))
                                    case NestedPathType.CondWithoutAlternative =>
                                        Some(StringTreeCond(StringTreeOr(processedSubPaths)))
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
     * Takes a [[Path]] and transforms it into a [[StringTree]]. This implies an interpretation of
     * how to handle methods called on the object of interest (like `append`).
     *
     * @param path             The path element to be transformed.
     * @return If an empty [[Path]] is given, `None` will be returned. Otherwise, the transformed
     *         [[org.opalj.br.fpcf.properties.string_definition.StringTree]] will be returned. Note that
     *         all elements of the tree will be defined, i.e., if `path` contains sites that could
     *         not be processed (successfully), they will not occur in the tree.
     */
    def pathToStringTree[State <: ComputationState](path: Path)(implicit
        state: State,
        ps:    PropertyStore
    ): StringTree = {
        val tree = path.elements.size match {
            case 1 =>
                // It might be that for some expressions, a neutral element is produced which is
                // filtered out by pathToTreeAcc; return the lower bound in such cases
                pathToTreeAcc(path.elements.head).getOrElse(StringTreeConst(StringConstancyInformation.lb))
            case _ =>
                val children = ListBuffer.from(path.elements.flatMap { pathToTreeAcc(_) })
                if (children.size == 1) {
                    // The concatenation of one child is the child itself
                    children.head
                } else {
                    StringTreeConcat(children)
                }
        }
        tree
    }
}

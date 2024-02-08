/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package string_analysis
package preprocessing

import scala.collection.mutable.ListBuffer
import scala.collection.mutable.Map

import org.opalj.br.fpcf.properties.string_definition.StringConstancyInformation
import org.opalj.br.fpcf.properties.string_definition.StringTree
import org.opalj.br.fpcf.properties.string_definition.StringTreeConcat
import org.opalj.br.fpcf.properties.string_definition.StringTreeCond
import org.opalj.br.fpcf.properties.string_definition.StringTreeConst
import org.opalj.br.fpcf.properties.string_definition.StringTreeOr
import org.opalj.br.fpcf.properties.string_definition.StringTreeRepetition
import org.opalj.fpcf.FinalP
import org.opalj.fpcf.InterimUBP
import org.opalj.tac.fpcf.analyses.string_analysis.interpretation.InterpretationHandler

/**
 * [[PathTransformer]] is responsible for transforming a [[Path]] into another representation, such
 * as [[org.opalj.br.fpcf.properties.string_definition.StringTree]]s for example.
 * An instance can handle several consecutive transformations of different paths as long as they
 * refer to the underlying control flow graph. If this is no longer the case, create a new instance
 * of this class with the corresponding (new) `cfg?`.
 *
 * @param interpretationHandler An concrete instance of [[InterpretationHandler]] that is used to
 *                              process expressions / definition sites.
 *
 * @author Patrick Mell
 */
class PathTransformer[State <: ComputationState[State]](val interpretationHandler: InterpretationHandler[State]) {

    /**
     * Accumulator function for transforming a path into a StringTree element.
     */
    private def pathToTreeAcc(
        subpath: SubPath,
        fpe2Sci: Map[Int, ListBuffer[StringConstancyInformation]]
    )(implicit state: State): Option[StringTree] = {
        subpath match {
            case fpe: FlatPathElement =>
                val sci = if (fpe2Sci.contains(fpe.element)) {
                    StringConstancyInformation.reduceMultiple(fpe2Sci(fpe.element))
                } else {
                    val sciToAdd = interpretationHandler.processDefSite(fpe.element) match {
                        case FinalP(p)      => p.stringConstancyInformation
                        case InterimUBP(ub) => ub.stringConstancyInformation
                        case _              => StringConstancyInformation.lb
                    }

                    fpe2Sci(fpe.element) = ListBuffer(sciToAdd)
                    sciToAdd
                }
                Option.unless(sci.isTheNeutralElement)(StringTreeConst(sci))
            case npe: NestedPathElement =>
                if (npe.elementType.isDefined) {
                    npe.elementType.get match {
                        case NestedPathType.Repetition =>
                            val processedSubPath = pathToStringTree(
                                Path(npe.element.toList),
                                fpe2Sci,
                                resetExprHandler = false
                            )
                            Some(StringTreeRepetition(processedSubPath))
                        case _ =>
                            val processedSubPaths = npe.element.flatMap { ne => pathToTreeAcc(ne, fpe2Sci) }
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
                        case 1 => pathToTreeAcc(npe.element.head, fpe2Sci)
                        case _ =>
                            val processed = npe.element.flatMap { ne => pathToTreeAcc(ne, fpe2Sci) }
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
     * @param fpe2Sci          A mapping from [[FlatPathElement.element]] values to [[StringConstancyInformation]]. Make
     *                         use of this mapping if some StringConstancyInformation need to be used that the
     *                         [[InterpretationHandler]] cannot infer / derive. For instance, if the exact value of an
     *                         expression needs to be determined by calling the
     *                         [[org.opalj.tac.fpcf.analyses.string_analysis.l0.L0StringAnalysis]]
     *                         on another instance, store this information in fpe2Sci.
     * @param resetExprHandler Whether to reset the underlying [[InterpretationHandler]] or not. When calling this
     *                         function from outside, the default value should do fine in most of the cases. For further
     *                         information, see [[InterpretationHandler.reset]].
     * @return If an empty [[Path]] is given, `None` will be returned. Otherwise, the transformed
     *         [[org.opalj.br.fpcf.properties.string_definition.StringTree]] will be returned. Note that
     *         all elements of the tree will be defined, i.e., if `path` contains sites that could
     *         not be processed (successfully), they will not occur in the tree.
     */
    def pathToStringTree(
        path:             Path,
        fpe2Sci:          Map[Int, ListBuffer[StringConstancyInformation]] = Map.empty,
        resetExprHandler: Boolean                                          = true
    )(implicit state: State): StringTree = {
        val tree = path.elements.size match {
            case 1 =>
                // It might be that for some expressions, a neutral element is produced which is
                // filtered out by pathToTreeAcc; return the lower bound in such cases
                pathToTreeAcc(path.elements.head, fpe2Sci).getOrElse(StringTreeConst(StringConstancyInformation.lb))
            case _ =>
                val children = ListBuffer.from(path.elements.flatMap { pathToTreeAcc(_, fpe2Sci) })
                if (children.size == 1) {
                    // The concatenation of one child is the child itself
                    children.head
                } else {
                    StringTreeConcat(children)
                }
        }
        if (resetExprHandler) {
            interpretationHandler.reset()
        }
        tree
    }
}

/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.tac.fpcf.analyses.string_analysis.preprocessing

import scala.collection.mutable.ListBuffer

import org.opalj.br.cfg.CFG
import org.opalj.br.fpcf.properties.properties.StringTree
import org.opalj.br.fpcf.properties.string_definition.StringConstancyInformation
import org.opalj.br.fpcf.properties.string_definition.StringTreeConcat
import org.opalj.br.fpcf.properties.string_definition.StringTreeCond
import org.opalj.br.fpcf.properties.string_definition.StringTreeConst
import org.opalj.br.fpcf.properties.string_definition.StringTreeOr
import org.opalj.br.fpcf.properties.string_definition.StringTreeRepetition
import org.opalj.tac.Stmt
import org.opalj.tac.TACStmts
import org.opalj.tac.fpcf.analyses.string_analysis.V
import org.opalj.tac.fpcf.analyses.string_analysis.interpretation.InterpretationHandler

/**
 * [[PathTransformer]] is responsible for transforming a [[Path]] into another representation, such
 * as [[org.opalj.br.fpcf.properties.properties.StringTree]]s for example.
 * An instance can handle several consecutive transformations of different paths as long as they
 * refer to the underlying control flow graph. If this is no longer the case, create a new instance
 * of this class with the corresponding (new) `cfg?`.
 *
 * @param cfg Objects of this class require a control flow graph that is used for transformations.
 *
 * @author Patrick Mell
 */
class PathTransformer(val cfg: CFG[Stmt[V], TACStmts[V]]) {

    private val exprHandler = InterpretationHandler(cfg)

    /**
     * Accumulator function for transforming a path into a StringTree element.
     */
    private def pathToTreeAcc(
        subpath: SubPath, fpe2Sci: Map[Int, StringConstancyInformation]
    ): Option[StringTree] = {
        subpath match {
            case fpe: FlatPathElement ⇒
                val sciList = if (fpe2Sci.contains(fpe.element)) List(fpe2Sci(fpe.element)) else
                    exprHandler.processDefSite(fpe.element)
                sciList.length match {
                    case 0 ⇒ None
                    case 1 ⇒ Some(StringTreeConst(sciList.head))
                    case _ ⇒
                        val treeElements = ListBuffer[StringTree]()
                        treeElements.appendAll(sciList.map(StringTreeConst).to[ListBuffer])
                        if (treeElements.nonEmpty) {
                            Some(StringTreeOr(treeElements))
                        } else {
                            None
                        }
                }
            case npe: NestedPathElement ⇒
                if (npe.elementType.isDefined) {
                    npe.elementType.get match {
                        case NestedPathType.Repetition ⇒
                            val processedSubPath = pathToStringTree(
                                Path(npe.element.toList), resetExprHandler = false
                            )
                            Some(StringTreeRepetition(processedSubPath))
                        case _ ⇒
                            val processedSubPaths = npe.element.map { ne ⇒
                                pathToTreeAcc(ne, fpe2Sci)
                            }.filter(_.isDefined).map(_.get)
                            if (processedSubPaths.nonEmpty) {
                                npe.elementType.get match {
                                    case NestedPathType.CondWithAlternative |
                                        NestedPathType.TryCatchFinally ⇒
                                        // In case there is only one element in the sub path,
                                        // transform it into a conditional element (as there is no
                                        // alternative)
                                        if (processedSubPaths.tail.nonEmpty) {
                                            Some(StringTreeOr(processedSubPaths))
                                        } else {
                                            Some(StringTreeCond(processedSubPaths))
                                        }
                                    case NestedPathType.CondWithoutAlternative ⇒
                                        Some(StringTreeCond(processedSubPaths))
                                    case _ ⇒ None
                                }
                            } else {
                                None
                            }
                    }
                } else {
                    npe.element.size match {
                        case 0 ⇒ None
                        case 1 ⇒ pathToTreeAcc(npe.element.head, fpe2Sci)
                        case _ ⇒
                            val processed = npe.element.map { ne ⇒
                                pathToTreeAcc(ne, fpe2Sci)
                            }.filter(_.isDefined).map(_.get)
                            if (processed.isEmpty) {
                                None
                            } else {
                                Some(StringTreeConcat(processed))
                            }
                    }
                }
            case _ ⇒ None
        }
    }

    /**
     * Takes a [[Path]] and transforms it into a [[StringTree]]. This implies an interpretation of
     * how to handle methods called on the object of interest (like `append`).
     *
     * @param path The path element to be transformed.
     * @param fpe2Sci A mapping from [[FlatPathElement.element]] values to
     *                [[StringConstancyInformation]]. Make use of this mapping if some
     *                StringConstancyInformation need to be used that the [[InterpretationHandler]]
     *                cannot infer / derive. For instance, if the exact value of an expression needs
     *                to be determined by calling the
     *                [[org.opalj.tac.fpcf.analyses.string_analysis.LocalStringAnalysis]]
     *                on another instance, store this information in fpe2Sci.
     * @param resetExprHandler Whether to reset the underlying [[InterpretationHandler]] or not.
     *                         When calling this function from outside, the default value should do
     *                         fine in most of the cases. For further information, see
     *                         [[InterpretationHandler.reset]].
     * @return If an empty [[Path]] is given, `None` will be returned. Otherwise, the transformed
     *         [[org.opalj.br.fpcf.properties.properties.StringTree]] will be returned. Note that all elements of the tree will be defined,
     *         i.e., if `path` contains sites that could not be processed (successfully), they will
     *         not occur in the tree.
     */
    def pathToStringTree(
        path:             Path,
        fpe2Sci:          Map[Int, StringConstancyInformation] = Map.empty,
        resetExprHandler: Boolean                              = true
    ): StringTree = {
        val tree = path.elements.size match {
            case 1 ⇒ pathToTreeAcc(path.elements.head, fpe2Sci).get
            case _ ⇒
                val concatElement = StringTreeConcat(
                    path.elements.map { ne ⇒
                        pathToTreeAcc(ne, fpe2Sci)
                    }.filter(_.isDefined).map(_.get).to[ListBuffer]
                )
                // It might be that concat has only one child (because some interpreters might have
                // returned an empty list => In case of one child, return only that one
                if (concatElement.children.size == 1) {
                    concatElement.children.head
                } else {
                    concatElement
                }
        }
        if (resetExprHandler) {
            exprHandler.reset()
        }
        tree
    }

}

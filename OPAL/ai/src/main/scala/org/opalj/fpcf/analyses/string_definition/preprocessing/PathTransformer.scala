/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.analyses.string_definition.preprocessing

import org.opalj.br.cfg.CFG
import org.opalj.fpcf.analyses.string_definition.V
import org.opalj.fpcf.analyses.string_definition.interpretation.ExprHandler
import org.opalj.fpcf.string_definition.properties.StringTree
import org.opalj.fpcf.string_definition.properties.StringTreeConcat
import org.opalj.fpcf.string_definition.properties.StringTreeCond
import org.opalj.fpcf.string_definition.properties.StringTreeConst
import org.opalj.fpcf.string_definition.properties.StringTreeOr
import org.opalj.fpcf.string_definition.properties.StringTreeRepetition
import org.opalj.tac.Stmt
import org.opalj.tac.TACStmts

import scala.collection.mutable.ListBuffer

/**
 * [[PathTransformer]] is responsible for transforming a [[Path]] into another representation, such
 * as [[StringTree]]s for example.
 * An instance can handle several consecutive transformations of different paths as long as they
 * refer to the underlying control flow graph. If this is no longer the case, create a new instance
 * of this class with the corresponding (new) `cfg?`.
 *
 * @param cfg Objects of this class require a control flow graph that is used for transformations.
 *
 * @author Patrick Mell
 */
class PathTransformer(val cfg: CFG[Stmt[V], TACStmts[V]]) {

    private val exprHandler = ExprHandler(cfg)

    /**
     * Accumulator function for transforming a path into a StringTree element.
     */
    private def pathToTreeAcc(subpath: SubPath): Option[StringTree] = {
        subpath match {
            case fpe: FlatPathElement ⇒
                val sciList = exprHandler.processDefSite(fpe.element)
                sciList.length match {
                    case 0 ⇒ None
                    case 1 ⇒ Some(StringTreeConst(sciList.head))
                    case _ ⇒
                        val treeElements = ListBuffer[StringTree]()
                        treeElements.appendAll(sciList.map(StringTreeConst).to[ListBuffer])
                        Some(StringTreeOr(treeElements))
                }
            case npe: NestedPathElement ⇒
                if (npe.elementType.isDefined) {
                    npe.elementType.get match {
                        case NestedPathType.Repetition ⇒
                            val processedSubPath = pathToStringTree(
                                Path(npe.element.toList), resetExprHandler = false
                            )
                            if (processedSubPath.isDefined) {
                                Some(StringTreeRepetition(processedSubPath.get))
                            } else {
                                None
                            }
                        case _ ⇒
                            val processedSubPaths = npe.element.map(
                                pathToTreeAcc
                            ).filter(_.isDefined).map(_.get)
                            npe.elementType.get match {
                                case NestedPathType.CondWithAlternative ⇒
                                    Some(StringTreeOr(processedSubPaths))
                                case NestedPathType.CondWithoutAlternative ⇒
                                    Some(StringTreeCond(processedSubPaths))
                                case _ ⇒ None
                            }
                    }
                } else {
                    npe.element.size match {
                        case 0 ⇒ None
                        case 1 ⇒ pathToTreeAcc(npe.element.head)
                        case _ ⇒ Some(StringTreeConcat(
                            npe.element.map(pathToTreeAcc).filter(_.isDefined).map(_.get)
                        ))
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
     * @param resetExprHandler Whether to reset the underlying [[ExprHandler]] or not. When calling
     *                         this function from outside, the default value should do fine in most
     *                         of the cases. For further information, see [[ExprHandler.reset]].
     * @return If an empty [[Path]] is given, `None` will be returned. Otherwise, the transformed
     *         [[StringTree]] will be returned. Note that all elements of the tree will be defined,
     *         i.e., if `path` contains sites that could not be processed (successfully), they will
     *         not occur in the tree.
     */
    def pathToStringTree(path: Path, resetExprHandler: Boolean = true): Option[StringTree] = {
        val tree = path.elements.size match {
            case 0 ⇒ None
            case 1 ⇒ pathToTreeAcc(path.elements.head)
            case _ ⇒
                val concatElement = Some(StringTreeConcat(
                    path.elements.map(pathToTreeAcc).filter(_.isDefined).map(_.get).to[ListBuffer]
                ))
                // It might be that concat has only one child (because some interpreters might have
                // returned an empty list => In case of one child, return only that one
                if (concatElement.isDefined && concatElement.get.children.size == 1) {
                    Some(concatElement.get.children.head)
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

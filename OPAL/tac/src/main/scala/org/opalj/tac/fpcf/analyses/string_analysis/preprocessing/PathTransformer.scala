/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.tac.fpcf.analyses.string_analysis.preprocessing

import scala.collection.mutable.ListBuffer
import scala.collection.mutable.Map

import org.opalj.br.fpcf.properties.properties.StringTree
import org.opalj.br.fpcf.properties.string_definition.StringConstancyInformation
import org.opalj.br.fpcf.properties.string_definition.StringTreeConcat
import org.opalj.br.fpcf.properties.string_definition.StringTreeCond
import org.opalj.br.fpcf.properties.string_definition.StringTreeConst
import org.opalj.br.fpcf.properties.string_definition.StringTreeOr
import org.opalj.br.fpcf.properties.string_definition.StringTreeRepetition
import org.opalj.br.fpcf.properties.StringConstancyProperty
import org.opalj.tac.fpcf.analyses.string_analysis.interpretation.InterpretationHandler

/**
 * [[PathTransformer]] is responsible for transforming a [[Path]] into another representation, such
 * as [[org.opalj.br.fpcf.properties.properties.StringTree]]s for example.
 * An instance can handle several consecutive transformations of different paths as long as they
 * refer to the underlying control flow graph. If this is no longer the case, create a new instance
 * of this class with the corresponding (new) `cfg?`.
 *
 * @param interpretationHandler An concrete instance of [[InterpretationHandler]] that is used to
 *                              process expressions / definition sites.
 *
 * @author Patrick Mell
 */
class PathTransformer(val interpretationHandler: InterpretationHandler) {

    /**
     * Accumulator function for transforming a path into a StringTree element.
     */
    private def pathToTreeAcc(
        subpath: SubPath, fpe2Sci: Map[Int, ListBuffer[StringConstancyInformation]]
    ): Option[StringTree] = {
        subpath match {
            case fpe: FlatPathElement ⇒
                val sci = if (fpe2Sci.contains(fpe.element)) {
                    StringConstancyInformation.reduceMultiple(fpe2Sci(fpe.element))
                } else {
                    val r = interpretationHandler.processDefSite(fpe.element)
                    val sciToAdd = if (r.isFinal) {
                        r.asFinal.p.asInstanceOf[StringConstancyProperty].stringConstancyInformation
                    } else {
                        // processDefSite is not guaranteed to return a StringConstancyProperty =>
                        // fall back to lower bound is necessary
                        if (r.isEPK || r.isEPS) {
                            StringConstancyInformation.lb
                        } else {
                            r.asInterim.ub match {
                                case property: StringConstancyProperty ⇒
                                    property.stringConstancyInformation
                                case _ ⇒
                                    StringConstancyInformation.lb
                            }
                        }
                    }
                    fpe2Sci(fpe.element) = ListBuffer(sciToAdd)
                    sciToAdd
                }
                if (sci.isTheNeutralElement) {
                    None
                } else {
                    Some(StringTreeConst(sci))
                }
            case npe: NestedPathElement ⇒
                if (npe.elementType.isDefined) {
                    npe.elementType.get match {
                        case NestedPathType.Repetition ⇒
                            val processedSubPath = pathToStringTree(
                                Path(npe.element.toList), fpe2Sci, resetExprHandler = false
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
     * @param path             The path element to be transformed.
     * @param fpe2Sci          A mapping from [[FlatPathElement.element]] values to
     *                         [[StringConstancyInformation]]. Make use of this mapping if some
     *                         StringConstancyInformation need to be used that the
     *                         [[org.opalj.tac.fpcf.analyses.string_analysis.interpretation.intraprocedural.IntraproceduralInterpretationHandler]]
     *                         cannot infer / derive. For instance, if the exact value of an
     *                         expression needs to be determined by calling the
     *                         [[org.opalj.tac.fpcf.analyses.string_analysis.IntraproceduralStringAnalysis]]
     *                         on another instance, store this information in fpe2Sci.
     * @param resetExprHandler Whether to reset the underlying
     *                         [[org.opalj.tac.fpcf.analyses.string_analysis.interpretation.intraprocedural.IntraproceduralInterpretationHandler]]
     *                         or not. When calling this function from outside, the default value
     *                         should do fine in most of the cases. For further information, see
     *                         [[org.opalj.tac.fpcf.analyses.string_analysis.interpretation.intraprocedural.IntraproceduralInterpretationHandler.reset]].
     *
     * @return If an empty [[Path]] is given, `None` will be returned. Otherwise, the transformed
     *         [[org.opalj.br.fpcf.properties.properties.StringTree]] will be returned. Note that
     *         all elements of the tree will be defined, i.e., if `path` contains sites that could
     *         not be processed (successfully), they will not occur in the tree.
     */
    def pathToStringTree(
        path:             Path,
        fpe2Sci:          Map[Int, ListBuffer[StringConstancyInformation]] = Map.empty,
        resetExprHandler: Boolean                                          = true
    ): StringTree = {
        val tree = path.elements.size match {
            case 1 ⇒
                // It might be that for some expressions, a neutral element is produced which is
                // filtered out by pathToTreeAcc; return the lower bound in such cases
                pathToTreeAcc(path.elements.head, fpe2Sci).getOrElse(
                    StringTreeConst(StringConstancyProperty.lb.stringConstancyInformation)
                )
            case _ ⇒
                val concatElement = StringTreeConcat(path.elements.map { ne ⇒
                    pathToTreeAcc(ne, fpe2Sci)
                }.filter(_.isDefined).map(_.get).to[ListBuffer])
                // It might be that concat has only one child (because some interpreters might have
                // returned an empty list => In case of one child, return only that one
                if (concatElement.children.size == 1) {
                    concatElement.children.head
                } else {
                    concatElement
                }
        }
        if (resetExprHandler) {
            interpretationHandler.reset()
        }
        tree
    }

}

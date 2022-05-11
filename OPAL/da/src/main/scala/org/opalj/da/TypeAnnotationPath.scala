/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package da

import scala.xml.Node

/**
 * @author Michael Eichberg
 * @author Wael Alkhatib
 * @author Isbel Isbel
 * @author Noorulla Sharief
 */
trait TypeAnnotationPath {

    def attribute_length: Int

    def toXHTML(implicit cp: Constant_Pool): Node
}

case object TypeAnnotationDirectlyOnType extends TypeAnnotationPath {

    final override def attribute_length: Int = 1

    def toXHTML(implicit cp: Constant_Pool): Node = {
        <div class="type_annotation_path"><b>Path</b> DirectlyOnType</div>
    }
}

trait TypeAnnotationPathElement {

    def type_path_kind: Int

    def type_argument_index: Int

    def toXHTML(implicit cp: Constant_Pool): Node
}

case class TypeAnnotationPathElements(
        path: TypeAnnotationPathElementsTable
) extends TypeAnnotationPath {

    final override def attribute_length: Int = 1 + path.length * 2

    def toXHTML(implicit cp: Constant_Pool): Node = {
        val path = <ol>{ this.path.map(pe => <li>{ pe.toXHTML(cp) }</li>) }</ol>

        // return node (this comment is a necessary technical artifact...)
        <div class="type_annotation_path"><b>Path</b>{ path }</div>
    }
}

/**
 * The `type_path_kind` was `0` (and the type_argument_index was also `0`).
 */
case object TypeAnnotationDeeperInArrayType extends TypeAnnotationPathElement {

    final override def type_path_kind: Int = 0

    final override def type_argument_index: Int = 0

    def toXHTML(implicit cp: Constant_Pool): Node = {
        <div class="type_annotation_path_element">DeeperInArrayType</div>
    }
}

case object TypeAnnotationDeeperInNestedType extends TypeAnnotationPathElement {

    final override def type_path_kind: Int = 1

    final override def type_argument_index: Int = 0

    def toXHTML(implicit cp: Constant_Pool): Node = {
        <div class="type_annotation_path_element">DeeperInNestedType</div>
    }
}

case object TypeAnnotationOnBoundOfWildcardType extends TypeAnnotationPathElement {

    final override def type_path_kind: Int = 2

    final override def type_argument_index: Int = 0

    def toXHTML(implicit cp: Constant_Pool): Node = {
        <div class="type_annotation_path_element">OnBoundOfWildcardType</div>
    }
}

case class TypeAnnotationOnTypeArgument(type_argument_index: Int) extends TypeAnnotationPathElement {

    final override def type_path_kind: Int = 3

    def toXHTML(implicit cp: Constant_Pool): Node = {
        <div class="type_annotation_path_element">OnTypeArgument: { type_argument_index }</div>
    }
}

/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package da

import scala.xml.Node

/**
 * <pre>
 * InnerClasses_attribute {
 *  u2 attribute_name_index;
 *  u4 attribute_length;
 *  u2 number_of_classes; // => Seq[InnerClasses_attribute.Class]
 *  {   u2 inner_class_info_index;
 *      u2 outer_class_info_index;
 *      u2 inner_name_index;
 *      u2 inner_class_access_flags;
 *  } classes[number_of_classes];
 * }
 * </pre>
 *
 * @author Michael Eichberg
 * @author Wael Alkhatib
 * @author Isbel Isbel
 * @author Noorulla Sharief
 * @author Andre Pacak
 */
case class InnerClasses_attribute(
        attribute_name_index: Int,
        classes:              Seq[InnerClassesEntry]
) extends Attribute {

    final override def attribute_length = 2 + (classes.size * 8)

    override def toXHTML(implicit cp: Constant_Pool): Node = {
        throw new UnsupportedOperationException(
            "use \"toXHTML(definingClassFQN: String)(implicit cp: Constant_Pool): Node\""
        )
    }

    def toXHTML(definingClass: FieldTypeInfo)(implicit cp: Constant_Pool): Node = {
        <div id="inner_classes">
            <details>
                <summary class="attribute_name">InnerClasses [size: { classes.size } item(s)]</summary>
                { classes.map(_.toXHTML(definingClass)) }
            </details>
        </div>
    }

}

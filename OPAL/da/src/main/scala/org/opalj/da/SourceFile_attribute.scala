/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package da

import scala.xml.Node

/**
 * @author Michael Eichberg
 * @author Wael Alkhatib
 * @author Isbel Isbel
 * @author Noorulla Sharief
 * @author Andre Pacak
 */
case class SourceFile_attribute(
        attribute_name_index: Constant_Pool_Index,
        sourceFile_index:     Constant_Pool_Index
) extends Attribute {

    final override def attribute_length = 2

    def sourceFile(implicit cp: Constant_Pool): String = cp(sourceFile_index).asString

    override def toXHTML(implicit cp: Constant_Pool): Node = {
        <div class="simple_attribute">
            <span class="attribute_name">SourceFile</span>
            -
            { sourceFile }
        </div>
    }

}

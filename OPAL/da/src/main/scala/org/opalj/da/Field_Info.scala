/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package da

import scala.xml.Node
import scala.xml.NodeSeq

import org.opalj.bi.AccessFlagsContexts.FIELD

/**
 * @author Michael Eichberg
 * @author Wael Alkhatib
 * @author Isbel Isbel
 * @author Noorulla Sharief
 * @author Andre Pacak
 */
case class Field_Info(
        access_flags:     Int,
        name_index:       Constant_Pool_Index,
        descriptor_index: Constant_Pool_Index,
        attributes:       Attributes          = NoAttributes
) extends ClassMember {

    def size: Int = 2 + 2 + 2 + 2 /* attributes_count*/ + attributes.view.map(_.size).sum

    /**
     * The field's name.
     */
    def fieldName(implicit cp: Constant_Pool): String = cp(name_index).asString

    /**
     * The type of the field.
     */
    def fieldType(implicit cp: Constant_Pool): FieldTypeInfo = {
        parseFieldType(cp(descriptor_index).asString)
    }

    def toXHTML(implicit cp: Constant_Pool): Node = {
        val (accessFlags, explicitAccessFlags) = accessFlagsToXHTML(access_flags, FIELD)
        val (constantValue, otherAttributes) =
            attributes.partition(a => a.attribute_name == "ConstantValue")
        val fieldName = this.fieldName
        val fieldDeclaration =
            <span class="field_declaration">
                { accessFlags }
                { fieldType.asSpan("field_type") }
                <span class="name">{ fieldName }</span>
                { if (constantValue.nonEmpty) constantValue.head.toXHTML else NodeSeq.Empty }
            </span>

        if (otherAttributes.isEmpty) {
            <div class="field details" data-name={ fieldName } data-access-flags={ explicitAccessFlags }>
                { fieldDeclaration }
            </div>
        } else {
            <details class="field" data-name={ fieldName } data-access-flags={ explicitAccessFlags }>
                <summary>{ fieldDeclaration }</summary>
                { otherAttributes.map(_.toXHTML) }
            </details>
        }
    }

    // def attributesToXHTML(implicit cp: Constant_Pool): Seq[Node] = {
    //    attributes.map(_.toXHTML)
    // }

}

/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package da

import scala.xml.Text
import scala.xml.NodeSeq
import scala.xml.Node

import org.opalj.bi.AccessFlagsContexts.METHOD

/**
 * @author Michael Eichberg
 * @author Wael Alkhatib
 * @author Isbel Isbel
 * @author Noorulla Sharief
 * @author Andre Pacak
 */
case class Method_Info(
        access_flags:     Int,
        name_index:       Constant_Pool_Index,
        descriptor_index: Constant_Pool_Index,
        attributes:       Attributes          = NoAttributes
) extends ClassMember {

    /**
     * The number of bytes required to store this method info object.
     */
    def size: Int = 2 + 2 + 2 + 2 /*attributes_count*/ + attributes.view.map(_.size).sum

    /**
     * The simple name of the method.
     */
    def name(implicit cp: Constant_Pool): String = cp(name_index).toString(cp)

    /**
     * The method descriptor as used by the Java VM. E.g., the method `void doIt()`
     * would have the descriptor `()V`.
     */
    def descriptor(implicit cp: Constant_Pool): String = cp(descriptor_index).asString

    /**
     * @param methodIndex The index of the method in the methods table. Required to create unique
     *                    ids/anchors.
     */
    def toXHTML(methodIndex: Int)(implicit cp: Constant_Pool): Node = {
        val (accessFlags, explicitAccessFlags) = accessFlagsToXHTML(access_flags, METHOD)

        val name = this.name
        val jvmDescriptor = this.descriptor
        val index = methodIndex.toString

        val (exceptionsAttributes, attributes0) = partitionByType(attributes, classOf[Exceptions_attribute])
        val (methodParametersAttributes, attributes1) = partitionByType(attributes0, classOf[MethodParameters_attribute])
        val declarationNode =
            <span class="method_declaration">
                { accessFlags }{
                    methodDescriptorAsInlineNode(
                        name,
                        jvmDescriptor,
                        methodParametersAttributes.headOption.map(_.parameters)
                    )
                }{
                    exceptionsAttributes.headOption.map(_.exceptionsSpan).getOrElse(NodeSeq.Empty)
                }
            </span>

        val (codeAttributes, attributes2) = partitionByType(attributes1, classOf[Code_attribute])
        val (signatureAttributes, attributes3) = partitionByType(attributes2, classOf[Signature_attribute])
        val signatureNode =
            if (signatureAttributes.nonEmpty) {
                val signatureAttribute = signatureAttributes.head
                Seq(<br/>, signatureAttribute.signatureSpan)
            } else {
                NodeSeq.Empty
            }
        if (codeAttributes.nonEmpty) {
            val codeAttribute = codeAttributes.head
            val code = codeAttribute.code
            val codeSize = code.instructions.length
            val maxStack = codeAttribute.max_stack
            val maxLocals = codeAttribute.max_locals
            val (lntAttributes, otherAttributes) = partitionByType(codeAttribute.attributes, classOf[LineNumberTable_attribute])
            val methodBodyHeader =
                s"[size: $codeSize bytes, max Stack: $maxStack, max Locals: $maxLocals]"
            <details open="" class="method" id={ name + jvmDescriptor } data-index={ index } data-name={ name } data-access-flags={ explicitAccessFlags }>
                <summary>
                    { Seq(declarationNode, Text(methodBodyHeader)) }
                    { signatureNode }
                </summary>
                <div>
                    {
                        code.toXHTML(
                            methodIndex,
                            codeAttribute.exceptionTable,
                            lntAttributes.headOption.map(_.line_number_table)
                        )
                    }
                    { codeAttribute.exception_handlersAsXHTML }
                    { otherAttributes.map(_.toXHTML) }
                </div>
                { attributes3.map(_.toXHTML) }
            </details>
        } else if (attributes3.nonEmpty) {
            <details class="method native_or_abstract" id={ name + jvmDescriptor } data-index={ index } data-name={ name } data-access-flags={ explicitAccessFlags }>
                <summary>
                    { declarationNode }
                    { signatureNode }
                </summary>
                { attributes3.map(_.toXHTML) }
            </details>
        } else {
            <div class="details method native_or_abstract" id={ name + jvmDescriptor } data-index={ index } data-name={ name } data-access-flags={ explicitAccessFlags }>
                { declarationNode }
                { signatureNode }
            </div>
        }
    }
}

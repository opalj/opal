/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package da

import org.opalj.bi.AccessFlagsContexts.METHOD

import scala.xml.Text
import scala.xml.NodeSeq
import scala.xml.Node

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
        attributes:       Attributes          = IndexedSeq.empty
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

        val (exceptionsAttributes, attributes0) = attributes.partition(_.isInstanceOf[Exceptions_attribute])
        val (methodParametersAttributes, attributes1) = attributes0.partition(_.isInstanceOf[MethodParameters_attribute])
        val declarationNode =
            <span class="method_declaration">
                { accessFlags }{
                    methodDescriptorAsInlineNode(
                        name,
                        jvmDescriptor,
                        methodParametersAttributes.headOption.map(_.asInstanceOf[MethodParameters_attribute].parameters)
                    )
                }{
                    exceptionsAttributes.headOption.map { ea ⇒
                        ea.asInstanceOf[Exceptions_attribute].exceptionsSpan
                    }.getOrElse(NodeSeq.Empty)
                }
            </span>

        val (codeAttributes, attributes2) = attributes1.partition(_.isInstanceOf[Code_attribute])
        val (signatureAttributes, attributes3) = attributes2.partition(_.isInstanceOf[Signature_attribute])
        val signatureNode =
            if (signatureAttributes.nonEmpty) {
                val signatureAttribute = signatureAttributes.head.asInstanceOf[Signature_attribute]
                Seq(<br/>, signatureAttribute.signatureSpan)
            } else {
                NodeSeq.Empty
            }
        if (codeAttributes.nonEmpty) {
            val codeAttribute = codeAttributes.head.asInstanceOf[Code_attribute]

            val code = codeAttribute.code
            val codeSize = code.instructions.length
            val maxStack = codeAttribute.max_stack
            val maxLocals = codeAttribute.max_locals
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
                            codeAttribute.attributes.collectFirst {
                                case LineNumberTable_attribute(_, lnt)⇒ lnt
                            }
                        )
                    }
                    { codeAttribute.exception_handlersAsXHTML }
                    {
                        codeAttribute.attributes.
                            filterNot(_.isInstanceOf[LineNumberTable_attribute]).
                            map(_.toXHTML)
                    }
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

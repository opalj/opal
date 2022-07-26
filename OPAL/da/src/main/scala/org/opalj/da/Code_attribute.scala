/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package da

import scala.xml.Node
import scala.xml.NodeSeq

/**
 * @author Michael Eichberg
 * @author Wael Alkhatib
 * @author Isbel Isbel
 * @author Noorulla Sharief
 * @author Andre Pacak
 */
case class Code_attribute(
        attribute_name_index: Constant_Pool_Index,
        max_stack:            Int,
        max_locals:           Int,
        code:                 Code,
        exceptionTable:       ExceptionTable      = NoExceptionTable,
        attributes:           Attributes          = NoAttributes
) extends Attribute {

    override def attribute_length: Int = {
        2 + 2 +
            4 /*code_length*/ + code.instructions.length +
            2 /*exception_table_length*/ + 8 * exceptionTable.length +
            2 /*attributes_count*/ + attributes.foldLeft(0)(_ + _.size)
    }

    /**
     * @see `toXHTML(Int)(implicit Constant_Pool)`
     */
    @throws[UnsupportedOperationException]("always")
    override def toXHTML(implicit cp: Constant_Pool): Node = {
        val message = "use toXHTML(methodIndex: Int)(implicit cp: Constant_Pool)"
        throw new UnsupportedOperationException(message)
    }

    def toXHTML(methodIndex: Int)(implicit cp: Constant_Pool): Node = {
        val codeSize = code.instructions.length
        val methodBodyHeader =
            s"Method Body (Size: $codeSize bytes, Max Stack: $max_stack, Max Locals: $max_locals)"
        <details class="method_body">
            <summary>{ methodBodyHeader }</summary>
            {
                code.toXHTML(
                    methodIndex,
                    exceptionTable,
                    attributes collectFirst { case LineNumberTable_attribute(_, lnt) => lnt }
                )
            }
            { exception_handlersAsXHTML }
            { attributesAsXHTML }
        </details>

    }

    def attributesAsXHTML(implicit cp: Constant_Pool): Seq[Node] = attributes.map(_.toXHTML)

    /** Can only be called if the exception table is non-emtpy! */
    def exception_handlersAsXHTML(implicit cp: Constant_Pool): NodeSeq = {
        if (exceptionTable.nonEmpty)
            <details class="exception_table">
                <summary>Exception Table:</summary>
                <ol class="exception_table">
                    { exceptionTable.map(_.toXHTML) }
                </ol>
            </details>
        else
            NodeSeq.Empty
    }
}

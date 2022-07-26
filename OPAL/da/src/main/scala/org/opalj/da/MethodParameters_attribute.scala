/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package da

import scala.xml.Node
import org.opalj.bi.AccessFlagsContexts.METHOD_PARAMETERS

import scala.collection.immutable.ArraySeq

/**
 * @author Michael Eichberg
 * @author Wael Alkhatib
 * @author Isbel Isbel
 * @author Noorulla Sharief
 */
case class MethodParameters_attribute(
        attribute_name_index: Constant_Pool_Index,
        parameters:           ArraySeq[MethodParameter]
) extends Attribute {

    final override def attribute_length: Int = 1 /*parameters_count*/ + parameters.size * 4

    // Primarily implemented to handle the case if the attribute is not found where expected.
    override def toXHTML(implicit cp: Constant_Pool): Node = {
        <details class="attribute method_paramaters">
            <summary class="attribute_name">Method Parameters</summary>
            <ol>{ parameters.map[Node] { p => <li>{ p.toXHTML(cp) }</li> } }</ol>
        </details>
    }

}

case class MethodParameter(
        name_index:   Constant_Pool_Index,
        access_flags: Int
) {

    def toXHTML(implicit cp: Constant_Pool): Seq[Node] = {
        val (accessFlags, _) = accessFlagsToXHTML(access_flags, METHOD_PARAMETERS)
        val name = if (name_index == 0) "<Formal Parameter>" else cp(name_index).toString(cp)
        <li>{ accessFlags }<span>{ name }</span></li>
    }

    def toXHTML(parameterTypeInfo: FieldTypeInfo)(implicit cp: Constant_Pool): Node = {
        val (accessFlags, _) = accessFlagsToXHTML(access_flags, METHOD_PARAMETERS)
        val name = if (name_index == 0) "<Formal Parameter>" else cp(name_index).toString(cp)
        <span>{ List(accessFlags, parameterTypeInfo.asSpan(""), <span> { name }</span>) }</span>
    }
}

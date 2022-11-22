/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org
package opalj
package tac
package fpcf
package properties

import org.opalj.br.ObjectType
import org.opalj.ifds.{AbstractIFDSFact, AbstractIFDSNullFact, Callable}

trait TaintFact extends AbstractIFDSFact

trait extraInfor

case object TaintNullFact extends TaintFact with AbstractIFDSNullFact

/**
 * A tainted variable.
 *
 * @param index The variable's definition site.
 */
case class Variable(index: Int) extends TaintFact

/**
 * A tainted array element.
 *
 * @param index The array's definition site.
 * @param element The index of the tainted element in the array.
 */
case class ArrayElement(index: Int, element: Int) extends TaintFact

/**
 * A tainted static field.
 *
 * @param classType The field's class.
 * @param fieldName The field's name.
 */
case class StaticField(classType: ObjectType, fieldName: String) extends TaintFact

/**
 * A tainted instance field.
 *
 * @param index The definition site of the field's value.
 * @param classType The field's type.
 * @param fieldName The field's value.
 */
case class InstanceField(index: Int, classType: ObjectType, fieldName: String) extends TaintFact

/**
 * A path of method calls, originating from the analyzed method, over which a tainted variable
 * reaches the sink.
 *
 * @param flow A sequence of method calls, originating from but not including this method.
 */
case class FlowFact(flow: Seq[Callable]) extends TaintFact {
    override val hashCode: Int = {
        var r = 1
        flow.foreach(f => r = (r + f.hashCode()) * 31)
        r
    }
}
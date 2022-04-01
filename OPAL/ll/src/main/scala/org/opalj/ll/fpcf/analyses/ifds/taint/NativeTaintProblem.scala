/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.ll.fpcf.analyses.ifds.taint

import org.opalj.br.ObjectType
import org.opalj.ifds.{AbstractIFDSFact, AbstractIFDSNullFact, Callable}
import org.opalj.ll.llvm.value.Value

trait NativeFact extends AbstractIFDSFact

case object NativeNullFact extends NativeFact with AbstractIFDSNullFact

/**
 * A tainted variable.
 *
 * @param index The variable's definition site.
 */
case class JavaVariable(index: Int) extends NativeFact
case class NativeVariable(value: Value) extends NativeFact

/**
 * A tainted array element.
 *
 * @param index The array's definition site.
 * @param element The index of the tainted element in the array.
 */
case class JavaArrayElement(index: Int, element: Int) extends NativeFact
case class NativeArrayElement(value: Value, element: Int) extends NativeFact

/**
 * A tainted static field.
 *
 * @param classType The field's class.
 * @param fieldName The field's name.
 */
case class JavaStaticField(classType: ObjectType, fieldName: String) extends NativeFact

/**
 * A tainted instance field.
 *
 * @param index The definition site of the field's value.
 * @param classType The field's type.
 * @param fieldName The field's value.
 */
case class JavaInstanceField(index: Int, classType: ObjectType, fieldName: String) extends NativeFact

/**
 * A path of method calls, originating from the analyzed method, over which a tainted variable
 * reaches the sink.
 *
 * @param flow A sequence of method calls, originating from but not including this method.
 */
case class NativeFlowFact(flow: Seq[Callable]) extends NativeFact {
    override val hashCode: Int = {
        var r = 1
        flow.foreach(f ⇒ r = (r + f.hashCode()) * 31)
        r
    }
}

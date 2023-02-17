/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ll
package fpcf
package analyses
package ifds
package taint

import org.opalj.br.ObjectType
import org.opalj.ifds.AbstractIFDSFact
import org.opalj.ifds.AbstractIFDSNullFact
import org.opalj.ifds.Callable
import org.opalj.ll.llvm.value.Value

trait NativeTaintFact extends AbstractIFDSFact

object NativeTaintNullFact extends NativeTaintFact with AbstractIFDSNullFact

/**
 * A tainted variable.
 *
 * @param index The variable's definition site.
 */
case class JavaVariable(index: Int) extends NativeTaintFact
case class NativeVariable(value: Value) extends NativeTaintFact

/**
 * A tainted array element.
 *
 * @param index The array's definition site.
 * @param element The index of the tainted element in the array.
 */
case class JavaArrayElement(index: Int, element: Int) extends NativeTaintFact
case class NativeArrayElement(base: Value, indices: Iterable[Long]) extends NativeTaintFact

/**
 * A tainted static field.
 *
 * @param classType The field's class.
 * @param fieldName The field's name.
 */
case class JavaStaticField(classType: ObjectType, fieldName: String) extends NativeTaintFact

/**
 * A tainted instance field.
 *
 * @param index The definition site of the field's value.
 * @param classType The field's type.
 * @param fieldName The field's value.
 */
case class JavaInstanceField(index: Int, classType: ObjectType, fieldName: String) extends NativeTaintFact

/**
 * A path of method calls, originating from the analyzed method, over which a tainted variable
 * reaches the sink.
 *
 * @param flow A sequence of method calls, originating from but not including this method.
 */
case class NativeFlowFact(flow: Seq[Callable]) extends NativeTaintFact {
    override val hashCode: Int = {
        var r = 1
        flow.foreach(f => r = (r + f.hashCode()) * 31)
        r
    }
}

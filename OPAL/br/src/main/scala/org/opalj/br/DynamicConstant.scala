/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br

import org.opalj.bytecode.BytecodeProcessingFailedException

/**
 * Represents a dynamic constant.
 *
 * `DynamicConstant` is used to represent dynamic constant arguments of bootstrap methods.
 */
final class DynamicConstant(
        val name:             String,
        val descriptor:       FieldType,
        private val bsmIndex: Int
) extends ConstantValue[Any] {
    private var bsm: BootstrapMethod = null

    def fillInBootstrapMethod(bootstrapMethods: BootstrapMethods): Unit = {
        if (bsm eq null) bsm = bootstrapMethods(bsmIndex)
    }

    def bootstrapMethod: BootstrapMethod = {
        if (bsm eq null)
            throw BytecodeProcessingFailedException("this dynamic constant is incomplete")
        else bsm
    }

    override def value = throw BytecodeProcessingFailedException("value is dynamic")

    override def valueToString = {
        (Iterator(name, descriptor.toJava+".class") ++
            bootstrapMethod.arguments.iterator.map(_.toJava))
            .mkString(bootstrapMethod.handle.toJava+"(", ",", ")")
    }

    def toJava = valueToString

    override def runtimeValueType = descriptor

    override def equals(other: Any): Boolean = {
        other match {
            case DynamicConstant(thatBSM, thatName, thatDescriptor) =>
                (bsm ne null) && thatBSM == bsm && thatName == name && thatDescriptor == descriptor
            case _ => false
        }
    }
}

object DynamicConstant {
    def unapply(dc: DynamicConstant): Some[(BootstrapMethod, String, FieldType)] = {
        Some((dc.bootstrapMethod, dc.name, dc.descriptor))
    }
}
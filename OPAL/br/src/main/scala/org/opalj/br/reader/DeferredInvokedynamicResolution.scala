/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package reader

import org.opalj.br.instructions.Instruction
import org.opalj.br.instructions.DEFAULT_INVOKEDYNAMIC

/**
 * Mixin this trait to resolve links between [[org.opalj.br.instructions.INVOKEDYNAMIC]]
 * instructions and the [[BootstrapMethodTable]].
 */
trait DeferredInvokedynamicResolution extends ConstantPoolBinding with CodeBinding {

    /**
     * Resolves an [[org.opalj.br.instructions.INCOMPLETE_INVOKEDYNAMIC]] instruction using the
     * [[BootstrapMethodTable]] of the class.
     *
     * Deferred resolution is necessary since the [[BootstrapMethodTable]] – which
     * is an attribute of the class file – is loaded after the methods.
     *
     * @note    This method is called (back) after the class file was completely loaded.
     *          Registration as a callback method happens whenever an `invokedynamic`
     *          instruction is found in a method's byte code.
     *
     * ==Overriding this Method==
     * To perform additional analyses on `invokedynamic` instructions, e.g., to
     * fully resolve the call target, a subclass may override this method to do so.
     * When you override this method, you should call this method
     * (`super.deferredResolveInvokedynamicResolution`) to ensure that the default resolution
     * is carried out.
     *
     * @param   classFile The [[ClassFile]] with which the deferred action was registered.
     * @param   cp The class file's [[Constant_Pool]].
     * @param   invokeDynamicInfo The [[org.opalj.br.instructions.INVOKEDYNAMIC]] instruction's
     *          constant pool entry.
     * @param   instructions This method's array of [[instructions.Instruction]]s.
     *          (The array returned by the [[#Instructions]] method.)
     * @param   pc The program counter of the `invokedynamic` instruction.
     */
    protected def deferredInvokedynamicResolution(
        classFile:           ClassFile,
        cp:                  Constant_Pool,
        ap_name_index:       Constant_Pool_Index,
        ap_descriptor_index: Constant_Pool_Index,
        invokeDynamicInfo:   CONSTANT_InvokeDynamic_info,
        instructions:        Array[Instruction],
        pc:                  PC
    ): ClassFile = {

        val bootstrapMethods = classFile.attributes collectFirst {
            case BootstrapMethodTable(bms) => bms
        }
        val invokeDynamic = DEFAULT_INVOKEDYNAMIC(
            bootstrapMethods.get(invokeDynamicInfo.bootstrapMethodAttributeIndex),
            invokeDynamicInfo.methodName(cp),
            invokeDynamicInfo.methodDescriptor(cp)
        )
        instructions(pc) = invokeDynamic
        classFile
    }
}

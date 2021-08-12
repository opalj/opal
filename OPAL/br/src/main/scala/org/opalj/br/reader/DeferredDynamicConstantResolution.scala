/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package reader

import org.opalj.br.instructions.INCOMPLETE_LDC
import org.opalj.br.instructions.INCOMPLETE_LDC2_W
import org.opalj.br.instructions.INCOMPLETE_LDC_W
import org.opalj.br.instructions.Instruction
import org.opalj.br.instructions.LoadDynamic
import org.opalj.br.instructions.LoadDynamic2_W
import org.opalj.br.instructions.LoadDynamic_W

/**
 * Mixin this trait to resolve links between [[org.opalj.br.instructions.LDC]]
 * instructions and the [[BootstrapMethodTable]].
 */
trait DeferredDynamicConstantResolution extends ConstantPoolBinding with CodeBinding {

    /**
     * Resolves an [[org.opalj.br.instructions.INCOMPLETE_LDC]] instruction using the
     * [[BootstrapMethodTable]] of the class.
     *
     * Deferred resolution is necessary since the [[BootstrapMethodTable]] – which
     * is an attribute of the class file – is loaded after the methods.
     *
     * @note    This method is called (back) after the class file was completely loaded.
     *          Registration as a callback method happens whenever an `ldc` instruction is found in
     *          a method's byte code that refers to a dynamic constant.
     *
     * ==Overriding this Method==
     * To perform additional analyses on dynamic constant loading `ldc` instructions, e.g., to
     * fully resolve the constant, a subclass may override this method to do so.
     * When you override this method, you should call this method
     * (`super.deferredResolveDynamicConstantResolution`) to ensure that the default resolution
     * is carried out.
     *
     * @param   classFile The [[ClassFile]] with which the deferred action was registered.
     * @param   cp The class file's [[Constant_Pool]].
     * @param   dynamicInfo The consttant pool entry describing the dynamic constant.
     * @param   instructions This method's array of [[instructions.Instruction]]s.
     *          (The array returned by the [[#Instructions]] method.)
     * @param   pc The program counter of the `invokedynamic` instruction.
     */
    protected def deferredDynamicConstantResolution(
        classFile:           ClassFile,
        cp:                  Constant_Pool,
        ap_name_index:       Constant_Pool_Index,
        ap_descriptor_index: Constant_Pool_Index,
        dynamicInfo:         CONSTANT_Dynamic_info,
        instructions:        Array[Instruction],
        pc:                  PC
    ): ClassFile = {

        val bootstrapMethods = classFile.attributes collectFirst {
            case BootstrapMethodTable(bms) => bms
        }

        val ldc = instructions(pc) match {
            case INCOMPLETE_LDC =>
                LoadDynamic(
                    bootstrapMethods.get(dynamicInfo.bootstrapMethodAttributeIndex),
                    dynamicInfo.name(cp),
                    dynamicInfo.descriptor(cp)
                )

            case INCOMPLETE_LDC_W =>
                LoadDynamic_W(
                    bootstrapMethods.get(dynamicInfo.bootstrapMethodAttributeIndex),
                    dynamicInfo.name(cp),
                    dynamicInfo.descriptor(cp)
                )

            case INCOMPLETE_LDC2_W =>
                LoadDynamic2_W(
                    bootstrapMethods.get(dynamicInfo.bootstrapMethodAttributeIndex),
                    dynamicInfo.name(cp),
                    dynamicInfo.descriptor(cp)
                )
        }

        instructions(pc) = ldc

        classFile
    }
}

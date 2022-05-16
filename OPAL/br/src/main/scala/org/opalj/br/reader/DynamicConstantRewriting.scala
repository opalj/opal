/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package reader

import com.typesafe.config.Config
import com.typesafe.config.ConfigValueFactory
import org.opalj.log.OPALLogger.error
import org.opalj.log.OPALLogger.info
import org.opalj.bi.ACC_PRIVATE
import org.opalj.bi.ACC_STATIC
import org.opalj.bi.ACC_SYNTHETIC
import org.opalj.br.collection.mutable.InstructionsBuilder
import org.opalj.br.instructions.Instruction
import org.opalj.br.instructions.INVOKESTATIC
import org.opalj.br.instructions.LDC
import org.opalj.br.instructions.LDCDynamic
import org.opalj.br.instructions.LoadDynamic2_W
import org.opalj.br.instructions.LoadDynamic_W
import org.opalj.br.instructions.NOP
import org.opalj.br.instructions.ReturnInstruction

import scala.collection.immutable.ArraySeq

/**
 * Provides support for rewriting Java 11 dynamic constant loading instructions.
 * This trait should be mixed in alongside a [[BytecodeReaderAndBinding]], which extracts
 * basic dynamic constant information from the [[BootstrapMethodTable]].
 *
 * Specifically, whenever an `ldc`, `ldc_w` or `ldc2_w` instruction is encountered that loads a
 * dynamic constant, this trait tries to rewrite it to a simpler expression providing the same
 * constant or an invocation of a method providing the constant from a lazily-initialized field.
 *
 * @note This rewriting is best-effort only. As `ldc` instructions take only two bytes, they cannot
 *       easily be rewritten to invocations, etc. `ldc_w` and `ldc2_w` instructions, however, are
 *       rewritten more aggressively.
 *
 * @see [[https://www.javacodegeeks.com/2018/08/hands-on-java-constantdynamic.html Hands on Java 11's constantdynamic]]
 *      for further information.
 *
 * @author Dominik Helm
 */
trait DynamicConstantRewriting
    extends DeferredDynamicConstantResolution
    with BootstrapArgumentLoading {

    this: ClassFileBinding =>

    import org.opalj.br.reader.DynamicConstantRewriting._

    val performRewriting: Boolean = {
        val rewrite: Boolean =
            try {
                config.getBoolean(RewritingConfigKey)
            } catch {
                case t: Throwable =>
                    error("class file reader", s"couldn't read: $RewritingConfigKey", t)
                    false
            }
        info(
            "class file reader",
            s"dynamic constants are ${if (rewrite) "" else "not "}rewritten"
        )
        rewrite
    }

    val logRewrites: Boolean = {
        val logRewrites: Boolean =
            try {
                config.getBoolean(LogRewritingsConfigKey)
            } catch {
                case t: Throwable =>
                    error("class file reader", s"couldn't read: $LogRewritingsConfigKey", t)
                    false
            }
        info(
            "class file reader",
            s"rewrites of dynamic constants are ${if (logRewrites) "" else "not "}logged"
        )
        logRewrites
    }

    val logUnknownDynamicConstants: Boolean = {
        val logUnknown: Boolean =
            try {
                config.getBoolean(LogUnknownDynamicConstantsConfigKey)
            } catch {
                case t: Throwable =>
                    error(
                        "class file reader",
                        s"couldn't read: $LogUnknownDynamicConstantsConfigKey", t
                    )
                    false
            }
        info(
            "class file reader",
            s"unknown dynamic constants are ${if (logUnknown) "" else "not "}logged"
        )
        logUnknown
    }

    val logUnresolvedDynamicConstants: Boolean = {
        val logUnresolved: Boolean =
            try {
                config.getBoolean(LogUnresolvedDynamicConstantsConfigKey)
            } catch {
                case t: Throwable =>
                    error(
                        "class file reader",
                        s"couldn't read: $LogUnresolvedDynamicConstantsConfigKey", t
                    )
                    false
            }
        info(
            "class file reader",
            s"unresolved dynamic constants are ${if (logUnresolved) "" else "not "}logged"
        )
        logUnresolved
    }

    override def deferredDynamicConstantResolution(
        classFile:             ClassFile,
        cp:                    Constant_Pool,
        methodNameIndex:       Constant_Pool_Index,
        methodDescriptorIndex: Constant_Pool_Index,
        dynamicInfo:           CONSTANT_Dynamic_info,
        instructions:          Array[Instruction],
        pc:                    PC
    ): ClassFile = {
        // gather complete information about ldc/ldc(2)_w instruction from bootstrap method table
        var updatedClassFile =
            super.deferredDynamicConstantResolution(
                classFile,
                cp,
                methodNameIndex,
                methodDescriptorIndex,
                dynamicInfo,
                instructions,
                pc
            )

        if (!performRewriting)
            return updatedClassFile;

        val load = instructions(pc)
        val LDCDynamic(bootstrapMethod, name, descriptor) = load
        val instructionLength = if (load.opcode == LDC.opcode) 2 else 3

        // Generate instructions to load the constant and add matching return
        val instructionsBuilder = new InstructionsBuilder(3)
        val (maxStack, newClassFile) =
            loadDynamicConstant(bootstrapMethod, name, descriptor, instructionsBuilder, classFile)
        updatedClassFile = newClassFile
        instructionsBuilder ++= ReturnInstruction(descriptor)

        val newInstructions = instructionsBuilder.result()
        val newLength = newInstructions.length - 1 // Don't count the return

        val head = newInstructions.head
        if (newLength == 3 && // There might not be an actual replacement
            (head.isInstanceOf[LoadDynamic_W] || head.isInstanceOf[LoadDynamic2_W])) {
            if (logUnknownDynamicConstants) {
                val t = classFile.thisType.toJava
                info(
                    "load-time transformation",
                    s"$t - unresolved ${load.mnemonic.toUpperCase}: $load"
                )
            }
        } else if (newLength <= instructionLength) { // Short enough, use directly
            var i = 0
            while (i < instructionLength) {
                instructions(pc + i) = if (i < newLength) newInstructions(i) else NOP
                i += 1
            }
            if (logRewrites)
                info("rewriting dynamic constant", s"Java: $load => $head")
        } else if (instructionLength == 3) { // Replace ldc(2)_w with invocation
            val newMethodName = newTargetMethodName(
                cp, methodNameIndex, methodDescriptorIndex, pc, "load_dynamic_contstant"
            )
            val newMethod = Method(
                ACC_SYNTHETIC.mask | ACC_PRIVATE.mask | ACC_STATIC.mask,
                newMethodName,
                MethodDescriptor.withNoArgs(descriptor),
                ArraySeq(Code(maxStack, 0, newInstructions, NoExceptionHandlers, NoAttributes))
            )
            updatedClassFile = updatedClassFile._UNSAFE_addMethod(newMethod)

            val newInvoke = INVOKESTATIC(
                classFile.thisType,
                isInterface = classFile.isInterfaceDeclaration,
                newMethodName,
                MethodDescriptor.withNoArgs(descriptor)
            )
            instructions(pc) = newInvoke

            if (logRewrites)
                info("rewriting dynamic constant", s"Java: $load => $newInvoke")
        } else { // Can't replace ldc with invocation, it has only 2 bytes
            if (logUnresolvedDynamicConstants) {
                val t = updatedClassFile.thisType.toJava
                info("load-time transformation", s"$t - unresolved LDC (not enough bytes): $load")
            }
        }
        updatedClassFile
    }

}

object DynamicConstantRewriting {

    final val DynamicConstantKeyPrefix = {
        ClassFileReaderConfiguration.ConfigKeyPrefix+"DynamicConstants."
    }

    final val RewritingConfigKey = DynamicConstantKeyPrefix+"rewrite"
    final val LogRewritingsConfigKey = DynamicConstantKeyPrefix+"logRewrites"
    final val LogUnknownDynamicConstantsConfigKey =
        DynamicConstantKeyPrefix+"logUnknownDynamicConstants"
    final val LogUnresolvedDynamicConstantsConfigKey =
        DynamicConstantKeyPrefix+"logUnresolvedDynamicConstants"

    /**
     * Returns the default config where the settings for rewriting and logging rewrites are
     * set to the specified values.
     */
    def defaultConfig(rewrite: Boolean, logRewrites: Boolean): Config = {
        BaseConfig.
            withValue(RewritingConfigKey, ConfigValueFactory.fromAnyRef(rewrite)).
            withValue(LogRewritingsConfigKey, ConfigValueFactory.fromAnyRef(logRewrites))
    }
}

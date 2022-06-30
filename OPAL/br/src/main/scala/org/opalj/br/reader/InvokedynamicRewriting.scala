/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package reader

import java.lang.invoke.LambdaMetafactory
import com.typesafe.config.Config
import com.typesafe.config.ConfigValueFactory
import org.opalj.log.Info
import org.opalj.log.OPALLogger
import org.opalj.log.OPALLogger.error
import org.opalj.log.OPALLogger.info
import org.opalj.log.StandardLogMessage
import org.opalj.collection.immutable.UIDSet
import org.opalj.bi.ACC_PRIVATE
import org.opalj.bi.ACC_PUBLIC
import org.opalj.bi.ACC_STATIC
import org.opalj.bi.ACC_SYNTHETIC
import org.opalj.bi.AccessFlags
import org.opalj.br.MethodDescriptor.JustReturnsString
import org.opalj.br.MethodDescriptor.LambdaAltMetafactoryDescriptor
import org.opalj.br.MethodDescriptor.LambdaMetafactoryDescriptor
import org.opalj.br.collection.mutable.InstructionsBuilder
import org.opalj.br.instructions._
import org.opalj.br.instructions.ClassFileFactory.AlternativeFactoryMethodName
import org.opalj.br.instructions.ClassFileFactory.DefaultFactoryMethodName

import scala.collection.IndexedSeqView
import scala.collection.immutable.ArraySeq

/**
 * Provides support for rewriting Java 8/Scala lambda or method reference expressions that
 * were compiled to [[org.opalj.br.instructions.INVOKEDYNAMIC]] instructions.
 * This trait should be mixed in alongside a [[BytecodeReaderAndBinding]], which extracts
 * basic `invokedynamic` information from the [[BootstrapMethodTable]].
 *
 * Specifically, whenever an `invokedynamic` instruction is encountered that is the result
 * of a lambda/method reference expression compiled by Oracle's JDK8, it creates a proxy
 * class file that represents the synthetic object that the JVM generates after executing
 * the `invokedynamic` call site. This proxy is then stored in the temporary ClassFile
 * attribute [[SynthesizedClassFiles]]. All such ClassFiles will
 * be picked up later for inclusion in the project.
 *
 * @see [[https://mydailyjava.blogspot.de/2015/03/dismantling-invokedynamic.html DismantlingInvokeDynamic]]
 *      for further information.
 *
 * @author Arne Lottmann
 * @author Michael Eichberg
 * @author Andreas Muttscheller
 * @author Dominik Helm
 */
trait InvokedynamicRewriting
    extends DeferredInvokedynamicResolution
    with BootstrapArgumentLoading {
    this: ClassFileBinding =>

    import InvokedynamicRewriting._

    val performInvokedynamicRewriting: Boolean = {
        import InvokedynamicRewriting.{InvokedynamicRewritingConfigKey => Key}
        val rewrite: Boolean =
            try {
                config.getBoolean(Key)
            } catch {
                case t: Throwable =>
                    error("class file reader", s"couldn't read: $Key", t)
                    false
            }
        if (rewrite) {
            info(
                "class file reader",
                "invokedynamics are rewritten"
            )
        } else {
            info(
                "class file reader",
                "invokedynamics are not rewritten"
            )
        }
        rewrite
    }

    val logLambdaExpressionsRewrites: Boolean = {
        import InvokedynamicRewriting.{LambdaExpressionsLogRewritingsConfigKey => Key}
        val logRewrites: Boolean =
            try {
                config.getBoolean(Key)
            } catch {
                case t: Throwable =>
                    error("class file reader", s"couldn't read: $Key", t)
                    false
            }
        if (logRewrites) {
            info(
                "class file reader",
                "rewrites of LambdaMetaFactory based invokedynamics are logged"
            )
        } else {
            info(
                "class file reader",
                "rewrites of LambdaMetaFactory based invokedynamics are not logged"
            )
        }
        logRewrites
    }

    val logStringConcatRewrites: Boolean = {
        import InvokedynamicRewriting.{StringConcatLogRewritingsConfigKey => Key}
        val logRewrites: Boolean =
            try {
                config.getBoolean(Key)
            } catch {
                case t: Throwable =>
                    error("class file reader", s"couldn't read: $Key", t)
                    false
            }
        if (logRewrites) {
            info(
                "class file reader",
                "rewrites of StringConcatFactory based invokedynamics are logged"
            )
        } else {
            info(
                "class file reader",
                "rewrites of StringConcatFactory based invokedynamics are not logged"
            )
        }
        logRewrites
    }

    val logObjectMethodsRewrites: Boolean = {
        import InvokedynamicRewriting.{ObjectMethodsLogRewritingsConfigKey => Key}
        val logRewrites: Boolean =
            try {
                config.getBoolean(Key)
            } catch {
                case t: Throwable =>
                    error("class file reader", s"couldn't read: $Key", t)
                    false
            }
        if (logRewrites) {
            info(
                "class file reader",
                "rewrites of StringConcatFactory based invokedynamics are logged"
            )
        } else {
            info(
                "class file reader",
                "rewrites of StringConcatFactory based invokedynamics are not logged"
            )
        }
        logRewrites
    }

    val logUnknownInvokeDynamics: Boolean = {
        import InvokedynamicRewriting.{InvokedynamicLogUnknownInvokeDynamicsConfigKey => Key}
        val logUnknownInvokeDynamics: Boolean =
            try {
                config.getBoolean(Key)
            } catch {
                case t: Throwable =>
                    error("class file reader", s"couldn't read: $Key", t)
                    false
            }
        if (logUnknownInvokeDynamics) {
            info("class file reader", "unknown invokedynamics are logged")
        } else {
            info("class file reader", "unknown invokedynamics are not logged")
        }
        logUnknownInvokeDynamics
    }

    val ScalaRuntimeObject: ObjectType = ObjectType("scala/runtime/ScalaRunTime$")

    /**
     * Generates a new, internal name for the proxy class for a rewritten invokedynamic.
     *
     * It follows the pattern:
     * `{surroundingType.simpleName}${surroundingMethodName}{surroundingMethodDescriptor}:pc$Lambda`
     * where surroundingMethodDescriptor is the JVM descriptor of the method sanitized to not
     * contain characters illegal in type names (replacing /, [ and ; by $, ] and : respectively)
     * and where pc is the pc of the invokedynamic that is rewritten.
     * For example: `Foo$bar()I:5` would refer to the invokedynamic instruction at pc 5 of the
     * method `bar` in class `Foo` that takes no parameters and returns an int.
     *
     * @param surroundingType the type in which the Lambda expression has been found
     */
    private def newLambdaTypeName(
        surroundingType:             ObjectType,
        surroundingMethodName:       String,
        surroundingMethodDescriptor: MethodDescriptor,
        pc:                          Int
    ): String = {
        val descriptor = surroundingMethodDescriptor.toJVMDescriptor
        val sanitizedDescriptor = replaceChars(descriptor, "/[;", "$]:")
        s"${surroundingType.packageName}/${surroundingType.simpleName}$$"+
            s"$surroundingMethodName$sanitizedDescriptor:$pc$$Lambda"
    }

    override def deferredInvokedynamicResolution(
        classFile:             ClassFile,
        cp:                    Constant_Pool,
        methodNameIndex:       Constant_Pool_Index,
        methodDescriptorIndex: Constant_Pool_Index,
        invokeDynamicInfo:     CONSTANT_InvokeDynamic_info,
        instructions:          Array[Instruction],
        pc:                    PC
    ): ClassFile = {
        // gather complete information about invokedynamic instructions from the bootstrap
        // method table
        val updatedClassFile =
            super.deferredInvokedynamicResolution(
                classFile,
                cp,
                methodNameIndex,
                methodDescriptorIndex,
                invokeDynamicInfo,
                instructions,
                pc
            )

        if (!performInvokedynamicRewriting)
            return updatedClassFile;

        val invokedynamic = instructions(pc).asInstanceOf[INVOKEDYNAMIC]
        if (InvokedynamicRewriting.isJava8LikeLambdaExpression(invokedynamic)) {
            java8LambdaResolution(
                cp: Constant_Pool,
                methodNameIndex: Constant_Pool_Index,
                methodDescriptorIndex: Constant_Pool_Index,
                updatedClassFile,
                instructions,
                pc,
                invokedynamic
            )
        } else if (isJava10StringConcatInvokedynamic(invokedynamic)) {
            java10StringConcatResolution(
                cp: Constant_Pool,
                methodNameIndex: Constant_Pool_Index,
                methodDescriptorIndex: Constant_Pool_Index,
                updatedClassFile,
                instructions,
                pc,
                invokedynamic
            )
        } else if (isObjectMethodsInvokedynamic(invokedynamic)) {
            objectMethodsResolution(
                cp: Constant_Pool,
                methodNameIndex: Constant_Pool_Index,
                methodDescriptorIndex: Constant_Pool_Index,
                updatedClassFile,
                instructions,
                pc,
                invokedynamic
            )
        } else if (isScalaLambdaDeserializeExpression(invokedynamic)) {
            scalaLambdaDeserializeResolution(
                cp: Constant_Pool,
                methodNameIndex: Constant_Pool_Index,
                methodDescriptorIndex: Constant_Pool_Index,
                updatedClassFile,
                instructions,
                pc,
                invokedynamic
            )
        } else if (isScalaSymbolExpression(invokedynamic)) {
            scalaSymbolResolution(
                updatedClassFile,
                instructions,
                pc,
                invokedynamic,
                cp,
                methodNameIndex,
                methodDescriptorIndex
            )
        } else if (isScalaStructuralCallSite(invokedynamic, instructions, pc)) {
            scalaStructuralCallSiteResolution(
                updatedClassFile,
                instructions,
                pc,
                invokedynamic,
                cp,
                methodNameIndex,
                methodDescriptorIndex
            )
        } else if (isGroovyInvokedynamic(invokedynamic)) {
            if (logUnknownInvokeDynamics) {
                OPALLogger.logOnce(StandardLogMessage(
                    Info,
                    Some("load-time transformation"),
                    "Groovy's INVOKEDYNAMICs are not rewritten"
                ))
            }
            updatedClassFile
        } else if (isDynamoInvokedynamic(invokedynamic)) {
            if (logUnknownInvokeDynamics) {
                OPALLogger.logOnce(StandardLogMessage(
                    Info,
                    Some("load-time transformation"),
                    "org.dynamo's INVOKEDYNAMICs are not rewritten"
                ))
            }
            updatedClassFile
        } else {
            if (logUnknownInvokeDynamics) {
                val t = classFile.thisType.toJava
                info("load-time transformation", s"$t - unresolvable INVOKEDYNAMIC: $invokedynamic")
            }
            updatedClassFile
        }
    }

    /**
     * Resolution of Java 10 string concat expressions.
     *
     * @see More information about string concat factory:
     *      [https://docs.oracle.com/javase/10/docs/api/java/lang/invoke/StringConcatFactory.html]
     *
     * @param classFile The classfile to parse.
     * @param instructions The instructions of the method we are currently parsing.
     * @param pc The program counter of the current instruction.
     * @param invokedynamic The INVOKEDYNAMIC instruction we want to replace.
     * @return A classfile which has the INVOKEDYNAMIC instruction replaced.
     */
    private def java10StringConcatResolution(
        cp:                    Constant_Pool,
        methodNameIndex:       Constant_Pool_Index,
        methodDescriptorIndex: Constant_Pool_Index,
        classFile:             ClassFile,
        instructions:          Array[Instruction],
        pc:                    PC,
        invokedynamic:         INVOKEDYNAMIC
    ): ClassFile = {
        val INVOKEDYNAMIC(bootstrapMethod, _, factoryDescriptor) = invokedynamic

        val args = bootstrapMethod.arguments

        // Extract the recipe and static arguments if present (if not, the dynamic arguments are
        // simply concatenated)
        val (recipe, staticArgs) =
            if (args.isEmpty)
                (
                    None,
                    ArraySeq.empty[ConstantValue[_]].view
                )
            else args.head match {
                case recipe: ConstantString =>
                    (
                        Some(recipe),
                        args.view.slice(from = 1, until = args.length).asInstanceOf[IndexedSeqView[ConstantValue[_]]]
                    )
                case _ =>
                    if (logUnknownInvokeDynamics) {
                        val t = classFile.thisType.toJava
                        info(
                            "load-time transformation",
                            s"$t - unresolvable INVOKEDYNAMIC (unknown string recipe): "+
                                invokedynamic
                        )
                    }
                    return classFile;
            }

        var updatedClassFile = classFile

        // Creates concat method
        def createConcatMethod(
            name:       String,
            descriptor: MethodDescriptor,
            recipeO:    Option[ConstantString],
            constants:  IndexedSeqView[ConstantValue[_]]
        ): MethodTemplate = {
            // A guess on the number of append operations required, need not be precise
            val numEntries =
                if (recipeO.isDefined) recipeO.get.value.length else descriptor.parametersCount
            val body: InstructionsBuilder = new InstructionsBuilder(11 + 6 * numEntries)

            body ++= NEW(ObjectType.StringBuilder)
            body ++= DUP
            body ++= INVOKESPECIAL(
                ObjectType.StringBuilder,
                isInterface = false,
                "<init>",
                MethodDescriptor.NoArgsAndReturnVoid
            )

            def appendType(t: Type): FieldType = {
                if (t.isBaseType) t.asBaseType
                else if (t eq ObjectType.String) ObjectType.String
                else ObjectType.Object
            }

            // Generate instructions to append a parameter to the StringBuilder
            def appendParam(paramType: FieldType, index: Int): Unit = {
                val isWide = index > 255
                if (isWide) body ++= WIDE

                val load = LoadLocalVariableInstruction(paramType, index)
                body ++= (load, load.indexOfNextInstruction(0, modifiedByWide = isWide))

                body ++= INVOKEVIRTUAL(
                    ObjectType.StringBuilder,
                    "append",
                    MethodDescriptor(appendType(paramType), ObjectType.StringBuilder)
                )
            }

            // Generate instructions to append a static constant to the StringBuilder
            def appendConstant(constant: ConstantValue[_]): Int = {
                val (constantStack, newClassFile) =
                    loadBootstrapArgument(constant, body, updatedClassFile)
                updatedClassFile = newClassFile

                body ++= INVOKEVIRTUAL(
                    ObjectType.StringBuilder,
                    "append",
                    MethodDescriptor(
                        appendType(constant.runtimeValueType),
                        ObjectType.StringBuilder
                    )
                )

                constantStack
            }

            // Generate code to append a substring of the recipe verbatim to the StringBuilder
            def appendString(startIndex: Int, endIndex: Int): Unit = {
                // Use `substring` to extract the relevant part of the recipe

                // Loading the recipe with `LDC` is safe, as the removal of the bootstrapMethod will
                // free one index in the constant pool where the recipe can be moved on
                // serialization of the class
                body ++= LDC(recipeO.get)

                body ++= LoadConstantInstruction(startIndex)
                body ++= LoadConstantInstruction(endIndex)

                body ++= INVOKEVIRTUAL(
                    ObjectType.String,
                    "substring",
                    MethodDescriptor(ArraySeq(IntegerType, IntegerType), ObjectType.String)
                )

                body ++= INVOKEVIRTUAL(
                    ObjectType.StringBuilder,
                    "append",
                    MethodDescriptor(ObjectType.String, ObjectType.StringBuilder)
                )
            }

            var lvIndex = 0 // Local variable index for the next parameter

            // At least 2 (for StringBuilder + param for append), but increased below if necessary
            var maxStack = 2

            if (recipeO.isDefined) {
                val recipe = recipeO.get.value

                // Index of next parameter (counting +1, while lvIndex respects operand sizes
                var paramIndex = 0
                // Index of next constant (from bootstrap arguments)
                var constantIndex = 0
                // Character position in the recipe where processing continues
                var recipeIndex = 0

                var nextParam = recipe.indexOf('\u0001') // Next parameter insertion point
                var nextConstant = recipe.indexOf('\u0002') // Next constant insertion point
                var nextInsert = 0 // Next insertion point (parameter or constant)

                while (recipeIndex < recipe.length) {
                    // Next insertion point is smaller of nextParam/nextConstant if each of those
                    // exist. If they don't exist, use recipe.length to append last verbatim part
                    // (if any) and terminate
                    nextInsert =
                        if (nextParam == -1)
                            if (nextConstant == -1) recipe.length
                            else nextConstant
                        else if (nextConstant == -1) nextParam
                        else Math.min(nextParam, nextConstant)

                    // Append parts from recipe between insertion points verbatim
                    if (nextInsert > recipeIndex) {
                        appendString(recipeIndex, nextInsert)
                        maxStack = 4 // StringBuilder, String, BeginIndex, EndIndex
                    }

                    if (nextInsert < recipe.length) {
                        if (nextInsert == nextParam) {
                            assert(recipe.charAt(nextInsert) == '\u0001')
                            val paramType = descriptor.parameterType(paramIndex)
                            appendParam(paramType, lvIndex)
                            val opSize = paramType.computationalType.operandSize
                            if (maxStack == 2 && opSize == 2) maxStack = 3
                            lvIndex += opSize
                            paramIndex += 1
                            nextParam = recipe.indexOf('\u0001', nextParam + 1)
                        } else {
                            assert(recipe.charAt(nextInsert) == '\u0002')
                            val constant = constants(constantIndex)
                            val constantStack = appendConstant(constant)
                            maxStack = Math.max(maxStack, constantStack + 1)
                            constantIndex += 1
                            nextConstant = recipe.indexOf('\u0002', nextConstant + 1)
                        }
                    }

                    recipeIndex = nextInsert + 1 // skip after current insertion point
                }
            } else {
                descriptor.parameterTypes.foreach { paramType =>
                    appendParam(paramType, lvIndex)
                    val opSize = paramType.computationalType.operandSize
                    if (maxStack == 2 && opSize == 2) maxStack = 3
                    lvIndex += opSize
                }
            }

            // Finally, turn StringBuilder to String
            body ++= INVOKEVIRTUAL(ObjectType.StringBuilder, "toString", JustReturnsString)
            body ++= ARETURN

            val maxLocals = descriptor.requiredRegisters

            val code = Code(maxStack, maxLocals, body.result(), NoExceptionHandlers, NoAttributes)

            // Access flags for the concat are `/* SYNTHETIC */ private static`
            val accessFlags = ACC_SYNTHETIC.mask | ACC_PRIVATE.mask | ACC_STATIC.mask

            Method(accessFlags, name, descriptor, ArraySeq(code))
        }

        val concatName =
            newTargetMethodName(cp, methodNameIndex, methodDescriptorIndex, pc, "string_concat")
        val concatMethod =
            createConcatMethod(concatName, factoryDescriptor, recipe, staticArgs)

        updatedClassFile = updatedClassFile._UNSAFE_addMethod(concatMethod)

        val newInvokestatic = INVOKESTATIC(
            classFile.thisType,
            isInterface = classFile.isInterfaceDeclaration,
            concatName,
            factoryDescriptor
        )

        if (logStringConcatRewrites) {
            info("rewriting invokedynamic", s"Java: $invokedynamic => $newInvokestatic")
        }

        instructions(pc) = newInvokestatic
        // since invokestatic is two bytes shorter than invokedynamic, we need to fill the
        // two-byte gap following the invokestatic with NOPs
        instructions(pc + 3) = NOP
        instructions(pc + 4) = NOP

        updatedClassFile
    }

    /**
     * Resolution of bootstrap methods from java.lang.runtime.ObjectMethods used for Records (Java
     * 16).
     *
     * @see More information about ObjectMethods:
     *      [https://docs.oracle.com/en/java/javase/16/docs/api/java.base/java/lang/runtime/ObjectMethods.html]
     *
     * @param classFile The classfile to parse.
     * @param instructions The instructions of the method we are currently parsing.
     * @param pc The program counter of the current instruction.
     * @param invokedynamic The INVOKEDYNAMIC instruction we want to replace.
     * @return A classfile which has the INVOKEDYNAMIC instruction replaced.
     */
    private def objectMethodsResolution(
        cp:                    Constant_Pool,
        methodNameIndex:       Constant_Pool_Index,
        methodDescriptorIndex: Constant_Pool_Index,
        classFile:             ClassFile,
        instructions:          Array[Instruction],
        pc:                    PC,
        invokedynamic:         INVOKEDYNAMIC
    ): ClassFile = {
        val INVOKEDYNAMIC(bootstrapMethod, targetMethodName, _) = invokedynamic

        val newMethodName =
            newTargetMethodName(cp, methodNameIndex, methodDescriptorIndex, pc, "object_methods")

        val (updatedClassFile, newMethodDescriptor) = createObjectMethodsTarget(
            bootstrapMethod.arguments, targetMethodName, newMethodName, classFile
        ).getOrElse {
                if (logUnknownInvokeDynamics) {
                    val t = classFile.thisType.toJava
                    info(
                        "load-time transformation",
                        s"$t - unresolvable INVOKEDYNAMIC: $invokedynamic"
                    )
                }
                return classFile;
            }

        val newInvokestatic = INVOKESTATIC(
            classFile.thisType,
            isInterface = classFile.isInterfaceDeclaration,
            newMethodName,
            newMethodDescriptor
        )

        if (logObjectMethodsRewrites) {
            info("rewriting invokedynamic", s"Java: $invokedynamic => $newInvokestatic")
        }

        instructions(pc) = newInvokestatic
        // since invokestatic is two bytes shorter than invokedynamic, we need to fill the
        // two-byte gap following the invokestatic with NOPs
        instructions(pc + 3) = NOP
        instructions(pc + 4) = NOP

        updatedClassFile
    }

    /**
     * Resolve invokedynamic instructions introduced by scala.deprecatedName.
     *
     * @param classFile The classfile to parse
     * @param instructions The instructions of the method we are currently parsing
     * @param pc The program counter of the current instuction
     * @param invokedynamic The INVOKEDYNAMIC instruction we want to replace
     * @return A classfile which has the INVOKEDYNAMIC instruction replaced
     */
    private def scalaSymbolResolution(
        classFile:             ClassFile,
        instructions:          Array[Instruction],
        pc:                    PC,
        invokedynamic:         INVOKEDYNAMIC,
        cp:                    Constant_Pool,
        methodNameIndex:       Constant_Pool_Index,
        methodDescriptorIndex: Constant_Pool_Index
    ): ClassFile = {
        // IMPROVE Rewrite to avoid that we have to use a constant pool entry in the range [0..255]
        val INVOKEDYNAMIC(
            bootstrapMethod, _, _ // functionalInterfaceMethodName, factoryDescriptor
            ) = invokedynamic
        val bootstrapArguments = bootstrapMethod.arguments

        val newInvokestatic =
            INVOKESTATIC(
                ObjectType.ScalaSymbol,
                isInterface = false, // the created proxy class is always a concrete class
                "apply",
                // the invokedynamic's methodDescriptor (factoryDescriptor) determines
                // the parameters that are actually pushed and popped from/to the stack
                MethodDescriptor(ArraySeq(ObjectType.String), ObjectType.ScalaSymbol)
            )

        if (logLambdaExpressionsRewrites) {
            info("rewriting invokedynamic", s"Scala: $invokedynamic => $newInvokestatic")
        }

        val instructionsBuilder = new InstructionsBuilder(7)
        val (maxStack, newClassFile) = loadBootstrapArgument(
            bootstrapArguments.head.asInstanceOf[ConstantValue[_]], instructionsBuilder, classFile
        )
        var updatedClassFile = newClassFile

        instructionsBuilder ++= newInvokestatic
        instructionsBuilder ++= ARETURN

        val newInstructions = instructionsBuilder.result()

        val firstInstruction = newInstructions.head
        if (newInstructions.length == 7 && firstInstruction.opcode == LDC_W.opcode) {
            instructions(pc) = firstInstruction match {
                case LoadString_W(s)                      => LoadString(s)
                case LoadDynamic_W(bsm, name, descriptor) => LoadDynamic(bsm, name, descriptor)
            }
            instructions(pc + 2) = newInvokestatic
        } else {
            val newMethodName =
                newTargetMethodName(cp, methodNameIndex, methodDescriptorIndex, pc, "scala_symbol")
            val newMethod = Method(
                ACC_SYNTHETIC.mask | ACC_PRIVATE.mask | ACC_STATIC.mask,
                newMethodName,
                MethodDescriptor.withNoArgs(ObjectType.ScalaSymbol),
                ArraySeq(Code(maxStack, 0, newInstructions, NoExceptionHandlers, NoAttributes))
            )
            updatedClassFile = updatedClassFile._UNSAFE_addMethod(newMethod)

            val newInvoke = INVOKESTATIC(
                classFile.thisType,
                isInterface = classFile.isInterfaceDeclaration,
                newMethodName,
                MethodDescriptor.withNoArgs(ObjectType.ScalaSymbol)
            )
            instructions(pc) = newInvoke
            instructions(pc + 3) = NOP
            instructions(pc + 4) = NOP
        }

        updatedClassFile
    }

    /**
     * Remove invokedynamic instructions that use Scala's StructuralCallSite to implement caching
     * of resolved methods.
     *
     * @param classFile The classfile to parse
     * @param instructions The instructions of the method we are currently parsing
     * @param pc The program counter of the current instuction
     * @param invokedynamic The INVOKEDYNAMIC instruction we want to replace
     * @return A classfile which has the INVOKEDYNAMIC instruction replaced
     */
    private def scalaStructuralCallSiteResolution(
        classFile:             ClassFile,
        instructions:          Array[Instruction],
        pc:                    PC,
        invokedynamic:         INVOKEDYNAMIC,
        cp:                    Constant_Pool,
        methodNameIndex:       Constant_Pool_Index,
        methodDescriptorIndex: Constant_Pool_Index
    ): ClassFile = {
        val methodType = invokedynamic.bootstrapMethod.arguments.head.asInstanceOf[ConstantValue[_]]

        val body = new InstructionsBuilder(18)

        body ++= GETSTATIC(ScalaRuntimeObject, "MODULE$", ScalaRuntimeObject)

        body ++= ALOAD_0

        body ++= instructions(22).asInstanceOf[LoadString]

        val (methodTypeStack, updatedClassFile) = loadBootstrapArgument(methodType, body, classFile)

        body ++= INVOKEVIRTUAL(
            ObjectType.MethodType,
            "parameterArray",
            MethodDescriptor.withNoArgs(ArrayType(ObjectType.Class))
        )

        body ++= instructions(28).asMethodInvocationInstruction // .getMethod

        body ++= instructions(31).asMethodInvocationInstruction // .ensureAccessible

        body ++= ARETURN

        if (logLambdaExpressionsRewrites) {
            info("rewriting invokedynamic", s"Scala: Removed $invokedynamic")
        }

        val maxStack = 3 + methodTypeStack

        val methodName = cp(methodNameIndex).asString
        val methodDescriptor = cp(methodDescriptorIndex).asMethodDescriptor

        val methodToRewrite = classFile.findMethod(methodName, methodDescriptor).get

        val code = Code(maxStack, 1, body.result(), NoExceptionHandlers, NoAttributes)

        val rewrittenMethod =
            Method(methodToRewrite.accessFlags, methodName, methodDescriptor, ArraySeq(code))

        updatedClassFile._UNSAFE_replaceMethod(methodToRewrite, rewrittenMethod)
    }

    /**
     * The scala compiler (and possibly other compilers targeting JVM bytecode) add a
     * `$deserializeLambda$`, which handles validation of lambda methods if the lambda is
     * Serializable. This method is called when the serialized lambda is deserialized.
     * For scala 2.12, it includes an INVOKEDYNAMIC instruction. This one has to be replaced with
     * calls to `LambdaDeserialize::deserializeLambda`, which is what the INVOKEDYNAMIC instruction
     * refers to, see [https://github.com/scala/scala/blob/v2.12.2/src/library/scala/runtime/LambdaDeserialize.java#L29].
     * This is done to reduce the size of `$deserializeLambda$`.
     *
     * @note   SerializedLambda has a readResolve method that looks for a (possibly private) static
     *         method called $deserializeLambda$(SerializedLambda) in the capturing class, invokes
     *         that with itself as the first argument, and returns the result. Lambda classes
     *         implementing $deserializeLambda$ are responsible for validating that the properties
     *         of the SerializedLambda are consistent with a lambda actually captured by that class.
     *          See: [https://docs.oracle.com/javase/8/docs/api/java/lang/invoke/SerializedLambda.html]
     *
     * @see     More information about lambda deserialization:
     *           - [https://zeroturnaround.com/rebellabs/java-8-revealed-lambdas-default-methods-and-bulk-data-operations/4/]
     *           - [http://mail.openjdk.java.net/pipermail/mlvm-dev/2016-August/006699.html]
     *           - [https://github.com/scala/scala/blob/v2.12.2/src/library/scala/runtime/LambdaDeserialize.java]
     *
     * @param classFile The classfile to parse
     * @param instructions The instructions of the method we are currently parsing
     * @param pc The program counter of the current instruction
     * @param invokedynamic The INVOKEDYNAMIC instruction we want to replace
     * @return A classfile which has the INVOKEDYNAMIC instruction replaced
     */
    private def scalaLambdaDeserializeResolution(
        cp:                    Constant_Pool,
        methodNameIndex:       Constant_Pool_Index,
        methodDescriptorIndex: Constant_Pool_Index,
        classFile:             ClassFile,
        instructions:          Array[Instruction],
        pc:                    PC,
        invokedynamic:         INVOKEDYNAMIC
    ): ClassFile = {
        val bootstrapArguments = invokedynamic.bootstrapMethod.arguments

        if (bootstrapArguments.nonEmpty) {
            if (bootstrapArguments exists {
                case _: InvokeStaticMethodHandle => false // As expected, static method handle
                case _                           => true // Unknown (dynamic) argument
            }) {
                if (logUnknownInvokeDynamics) {
                    val t = classFile.thisType.toJava
                    info(
                        "load-time transformation",
                        s"$t - unresolvable INVOKEDYNAMIC (unknown method handle): $invokedynamic"
                    )
                }
                return classFile;
            }
        }

        val methodName = cp(methodNameIndex).asString
        val methodDescriptor = cp(methodDescriptorIndex).asMethodDescriptor

        val typeDeclaration = TypeDeclaration(
            ObjectType(newLambdaTypeName(classFile.thisType, methodName, methodDescriptor, pc)),
            isInterfaceType = false,
            Some(LambdaMetafactoryDescriptor.returnType.asObjectType), // we basically create a "CallSiteObject"
            UIDSet.empty
        )

        val proxy: ClassFile = ClassFileFactory.DeserializeLambdaProxy(
            typeDeclaration,
            bootstrapArguments,
            DefaultDeserializeLambdaStaticMethodName
        )

        val factoryMethod = proxy.findMethod(DefaultDeserializeLambdaStaticMethodName).head

        val newInvokestatic = INVOKESTATIC(
            proxy.thisType,
            isInterface = false, // the created proxy class is always a concrete class
            factoryMethod.name,
            MethodDescriptor(
                ArraySeq(ObjectType.SerializedLambda),
                ObjectType.Object
            )
        )

        if (logLambdaExpressionsRewrites) {
            info("rewriting invokedynamic", s"Scala: $invokedynamic => $newInvokestatic")
        }

        instructions(pc) = newInvokestatic
        // since invokestatic is two bytes shorter than invokedynamic, we need to fill
        // the two-byte gap following the invokestatic with NOPs
        instructions(pc + 3) = NOP
        instructions(pc + 4) = NOP

        val reason = Some((classFile, instructions, pc, invokedynamic, newInvokestatic))
        storeProxy(classFile, proxy, reason)
    }

    /**
     * Resolution of java 8 lambda and method reference expressions.
     *
     * @see More information about lambda deserialization and lambda meta factory:
     *      [https://docs.oracle.com/javase/8/docs/api/java/lang/invoke/LambdaMetafactory.html]
     *
     * @param classFile The classfile to parse.
     * @param instructions The instructions of the method we are currently parsing.
     * @param pc The program counter of the current instruction.
     * @param invokedynamic The INVOKEDYNAMIC instruction we want to replace.
     * @return A classfile which has the INVOKEDYNAMIC instruction replaced.
     */
    private def java8LambdaResolution(
        cp:                    Constant_Pool,
        methodNameIndex:       Constant_Pool_Index,
        methodDescriptorIndex: Constant_Pool_Index,
        classFile:             ClassFile,
        instructions:          Array[Instruction],
        pc:                    PC,
        invokedynamic:         INVOKEDYNAMIC
    ): ClassFile = {
        val INVOKEDYNAMIC(
            bootstrapMethod, functionalInterfaceMethodName, factoryDescriptor
            ) = invokedynamic
        val bootstrapArguments = bootstrapMethod.arguments

        // Extract the arguments of the lambda factory.
        val (
            samMethodType, // describing the implemented method type
            tempImplMethod, // the MethodHandle providing the implementation
            instantiatedMethodType, // allowing restrictions on invocation
            // The available information depends on the metafactory:
            // (e.g., about bridges or markers)
            altMetafactoryArgs
            ) = bootstrapArguments match {
            case Seq(smt: MethodDescriptor, tim: MethodCallMethodHandle, imt: MethodDescriptor, ama @ _*) =>
                (smt, tim, imt, ama)
            case _ =>
                if (logUnknownInvokeDynamics) {
                    val t = classFile.thisType.toJava
                    info(
                        "load-time transformation",
                        s"$t - unresolvable INVOKEDYNAMIC (unknown lambda factory argument): "+
                            invokedynamic
                    )
                }
                return classFile;
        }

        var implMethod = tempImplMethod

        val thisType = classFile.thisType

        val (markerInterfaces, bridges, serializable) =
            extractAltMetafactoryArguments(altMetafactoryArgs)

        val MethodCallMethodHandle(
            targetMethodOwner: ObjectType, targetMethodName, targetMethodDescriptor
            ) = implMethod

        // In case of nested classes, we have to change the invoke instruction from
        // invokespecial to invokevirtual, because the special handling used for private
        // methods doesn't apply anymore.
        implMethod match {
            case specialImplMethod: InvokeSpecialMethodHandle =>
                implMethod = if (specialImplMethod.isInterface)
                    InvokeInterfaceMethodHandle(
                        implMethod.receiverType,
                        implMethod.name,
                        implMethod.methodDescriptor
                    )
                else
                    InvokeVirtualMethodHandle(
                        implMethod.receiverType,
                        implMethod.name,
                        implMethod.methodDescriptor
                    )
            case _ =>
        }

        val superInterfaceTypesBuilder = UIDSet.newBuilder[ObjectType]
        superInterfaceTypesBuilder += factoryDescriptor.returnType.asObjectType
        if (serializable) {
            superInterfaceTypesBuilder += ObjectType.Serializable
        }
        markerInterfaces foreach { mi =>
            superInterfaceTypesBuilder += mi.asObjectType
        }

        val methodName = cp(methodNameIndex).asString
        val methodDescriptor = cp(methodDescriptorIndex).asMethodDescriptor

        val typeDeclaration = TypeDeclaration(
            ObjectType(newLambdaTypeName(thisType, methodName, methodDescriptor, pc)),
            isInterfaceType = false,
            Some(ObjectType.Object), // we basically create a "CallSiteObject"
            superInterfaceTypesBuilder.result()
        )

        var invocationInstruction = implMethod.opcodeOfUnderlyingInstruction

        /*
        Check the type of the invoke instruction using the instruction's opcode.

        targetMethodOwner identifies the class where the method is actually implemented.
        This is wrong for INVOKEVIRTUAL and INVOKEINTERFACE. The call to the proxy class
        is done with the actual class, not the class where the method is implemented.
        Therefore, the receiverType must be the class from the caller, not where the to-
        be-called method is implemented. E.g., LinkedHashSet.contains() is implemented in
        HashSet, but the receiverType and constructor parameter must be LinkedHashSet
        instead of HashSet.

        *** INVOKEVIRTUAL ***
        An INVOKEVIRTUAL is used when the method is defined by a class type
        (not an interface type). (e.g., LinkedHashSet.addAll()).
        This instruction requires a receiver object when the method reference uses a
        non-null object as a receiver.
        E.g.: LinkedHashSet<T> lhs = new LinkedHashSet<>();
              lhs::container()

        It does not have a receiver field in case of a class based method reference,
        e.g. LinkedHashSet::container()

        *** INVOKEINTERFACE ***
        It is similar to INVOKEVIRTUAL, but the method definition is defined in an
        interface. Therefore, the same rule like INVOKEVIRTUAL applies.

        *** INVOKESTATIC ***
        Because we call a static method, we don't have an instance. Therefore we don't
        need a receiver field.

        *** INVOKESPECIAL ***
        INVOKESPECIAL is used for:
        - instance initialization methods (i.e., constructors) -> Method is implemented
          in called class -> no rewrite necessary
        - private method invocation: The private method must be in the same class as
          the callee -> no rewrite needed
        - Invokation of methods using super keyword -> Not needed, because a synthetic
          method in the callee class is created which handles the INVOKESPECIAL.
          Therefore the receiverType is also the callee class.

          E.g.
              public static class Superclass {
                  protected String someMethod() {
                      return "someMethod";
                  }
              }

              public static class Subclass extends Superclass {
                  public String callSomeMethod() {
                      Supplier<String> s = super::someMethod;
                      return s.get();
                  }
              }

          The class Subclass contains a synthetic method `access`, which has an
          INVOKESPECIAL instruction calling Superclass.someMethod. The generated
          Lambda Proxyclass calls Subclass.access, so the receiverType must be
          Subclass insteaed of Superclass.

          More information:
            http://www.javaworld.com/article/2073578/java-s-synthetic-methods.html
        */
        var receiverType =
            if (invocationInstruction != INVOKEVIRTUAL.opcode &&
                invocationInstruction != INVOKEINTERFACE.opcode) {
                targetMethodOwner
            } else if (invokedynamic.methodDescriptor.parameterTypes.nonEmpty &&
                invokedynamic.methodDescriptor.parameterTypes.head.isObjectType) {
                // If we have an instance of a object and use a method reference,
                // get the receiver type from the invokedynamic instruction.
                // It is the first parameter of the functional interface parameter
                // list.
                invokedynamic.methodDescriptor.parameterTypes.head.asObjectType
            } else if (instantiatedMethodType.parameterTypes.nonEmpty &&
                instantiatedMethodType.parameterTypes.head.isObjectType) {
                // If we get a instance method reference like `LinkedHashSet::addAll`, get
                // the receiver type from the functional interface. The first parameter is
                // the instance where the method should be called.
                instantiatedMethodType.parameterTypes.head.asObjectType
            } else {
                targetMethodOwner
            }

        /*
        It is possible for the receiverType to be different from classFile. In this case,
        check if the receiverType is an interface instead of the classFile.

        The Proxy Factory must get the correct value to build the correct variant of the
        INVOKESTATIC instruction.
         */
        var receiverIsInterface =
            implMethod match {

                case _: InvokeInterfaceMethodHandle    => true
                case _: InvokeVirtualMethodHandle      => false
                case _: NewInvokeSpecialMethodHandle   => false
                case handle: InvokeSpecialMethodHandle => handle.isInterface

                case handle: InvokeStaticMethodHandle =>
                    // The following test was added to handle a case where the Scala
                    // compiler generated invalid bytecode (the Scala compiler generated
                    // a MethodRef instead of an InterfaceMethodRef which led to the
                    // wrong kind of InvokeStaticMethodHandle).
                    // See https://github.com/scala/bug/issues/10429 for further details.
                    if (implMethod.receiverType eq classFile.thisType) {
                        classFile.isInterfaceDeclaration
                    } else {
                        handle.isInterface
                    }

                case other => throw new UnknownError("unexpected handle: "+other)
            }

        // Creates forwarding method for private method `m` that can be accessed by the proxy class.
        def createForwardingMethod(
            m:    Method,
            name: String
        ): MethodTemplate = {

            // Access flags for the forwarder are the same as the target method but without private
            // modifier. For interfaces, ACC_PUBLIC is required and constructors are forwarded by
            // a static method.
            val accessFlags =
                if (receiverIsInterface) (m.accessFlags & ~ACC_PRIVATE.mask) | ACC_PUBLIC.mask
                else m.accessFlags & ~ACC_PRIVATE.mask

            // Instructions to push the parameters onto the stack
            val forwardParameters = ClassFileFactory.parameterForwardingInstructions(
                m.descriptor, m.descriptor, 1, Seq.empty, classFile.thisType
            )

            val body = new InstructionsBuilder(7 + forwardParameters.length)

            // if the receiver method is not static, we need to push the receiver object
            // onto the stack by an ALOAD_0 unless we have a method reference where the receiver
            // will be explicitly provided
            body ++= ALOAD_0

            body ++= forwardParameters

            val recType = classFile.thisType

            // The call instruction itself TODO what happens for nested classes?
            body ++= INVOKESPECIAL(recType, receiverIsInterface, m.name, m.descriptor)

            // The return instruction matching the return type
            body ++= ReturnInstruction(m.descriptor.returnType)

            val parametersStackSize = m.descriptor.requiredRegisters

            val maxLocals = 1 + parametersStackSize // parameters + `this`
            val maxStack = math.max(maxLocals, m.descriptor.returnType.operandSize)

            val code = Code(maxStack, maxLocals, body.result(), NoExceptionHandlers, NoAttributes)

            Method(accessFlags, name, m.descriptor, ArraySeq(code))
        }

        val isInterface = classFile.isInterfaceDeclaration

        def lift(method: Method): MethodTemplate = {
            val accessFlags =
                if (isInterface) {
                    // Interface methods must be private or public => public so proxy can
                    // access it
                    (method.accessFlags & ~ACC_PRIVATE.mask) | ACC_PUBLIC.mask
                } else {
                    method.accessFlags & ~ACC_PRIVATE.mask // Package private, i.e. no modifier
                }
            method.copy(accessFlags = accessFlags)
        }

        var updatedClassFile = classFile

        if (targetMethodOwner eq classFile.thisType) {
            // If the target method is private, we have to generate a forwarding method that is
            // accessible by the proxy class, i.e. not private, or we have to lift the target method
            // (only for static methods and constructors, where the lifted method can not interfere with
            // inherited methods).
            val targetMethod = updatedClassFile.methods.find { m =>
                m.name == targetMethodName && m.descriptor == targetMethodDescriptor
            }

            assert(targetMethod.isDefined)
            val m = targetMethod.get

            if (m.isPrivate) {
                if (m.isStatic || m.isConstructor) {
                    updatedClassFile = updatedClassFile._UNSAFE_replaceMethod(
                        targetMethod.get, lift(targetMethod.get)
                    )
                } else {
                    val forwardingName = "$forward$"+m.name

                    // Update the implMethod and other information to match the forwarder
                    implMethod = if (isInterface) {
                        InvokeInterfaceMethodHandle(thisType, forwardingName, m.descriptor)
                    } else {
                        InvokeVirtualMethodHandle(thisType, forwardingName, m.descriptor)
                    }
                    invocationInstruction = implMethod.opcodeOfUnderlyingInstruction
                    receiverType = thisType
                    receiverIsInterface = isInterface

                    val forwarderO = updatedClassFile.findMethod(forwardingName, m.descriptor)
                    if (forwarderO.isEmpty) { // Add forwarder if not already present
                        val forwarder = createForwardingMethod(m, forwardingName)
                        updatedClassFile = updatedClassFile._UNSAFE_addMethod(forwarder)
                    }
                }
            }
        }

        // If the class is serializable, the $deserializeLambda$ method may need to be lifted
        if (serializable) {
            val deserialize = updatedClassFile.methods.find { m =>
                m.hasFlags(AccessFlags.ACC_SYNTHETIC_STATIC_PRIVATE) &&
                    m.name == "$deserializeLambda$"
            }
            if (deserialize.isDefined)
                updatedClassFile = updatedClassFile._UNSAFE_replaceMethod(
                    deserialize.get, lift(deserialize.get)
                )
        }

        val needsBridgeMethod = samMethodType != instantiatedMethodType

        val bridgeMethodDescriptorBuilder = ArraySeq.newBuilder[MethodDescriptor]
        if (needsBridgeMethod) {
            bridgeMethodDescriptorBuilder += samMethodType
        }
        // If the bridge has the same method descriptor as the instantiatedMethodType or
        // samMethodType, they are already present in the proxy class. Do not add them again.
        // This happens in scala patternmatching for example.
        bridgeMethodDescriptorBuilder ++= bridges
            .filterNot(_ == samMethodType)
            .filterNot(_ == instantiatedMethodType)
        val bridgeMethodDescriptors = bridgeMethodDescriptorBuilder.result()

        val proxy: ClassFile = ClassFileFactory.Proxy(
            thisType,
            updatedClassFile.isInterfaceDeclaration,
            typeDeclaration,
            functionalInterfaceMethodName,
            instantiatedMethodType,
            receiverType,
            receiverIsInterface = receiverIsInterface,
            implMethod,
            invocationInstruction,
            samMethodType,
            bridgeMethodDescriptors
        )
        val factoryMethod = {
            if (functionalInterfaceMethodName == DefaultFactoryMethodName)
                proxy.findMethod(AlternativeFactoryMethodName).head
            else
                proxy.findMethod(DefaultFactoryMethodName).head
        }

        val newInvokestatic = INVOKESTATIC(
            proxy.thisType,
            isInterface = false, // the created proxy class is always a concrete class
            factoryMethod.name,
            factoryMethod.descriptor
        )

        if (logLambdaExpressionsRewrites) {
            info("rewriting invokedynamic", s"Java: $invokedynamic => $newInvokestatic")
        }

        instructions(pc) = newInvokestatic
        // since invokestatic is two bytes shorter than invokedynamic, we need to fill
        // the two-byte gap following the invokestatic with NOPs
        instructions(pc + 3) = NOP
        instructions(pc + 4) = NOP

        val reason = Some((updatedClassFile, instructions, pc, invokedynamic, newInvokestatic))
        storeProxy(updatedClassFile, proxy, reason)
    }

    /**
     * Extract the parameters of `altMetafactory` calls.
     *
     * CallSite altMetafactory(MethodHandles.Lookup caller,
     *                   String invokedName,
     *                   MethodType invokedType,
     *                   Object... args)
     *
     * Object... args evaluates to the following argument list:
     *                   int flags,
     *                   int markerInterfaceCount,  // IF flags has MARKERS set
     *                   Class... markerInterfaces, // IF flags has MARKERS set
     *                   int bridgeCount,           // IF flags has BRIDGES set
     *                   MethodType... bridges      // IF flags has BRIDGES set
     *
     * flags is a bitwise OR of the desired flags FLAG_MARKERS, FLAG_BRIDGES and FLAG_SERIALIZABLE.
     *
     * @see https://docs.oracle.com/javase/8/docs/api/java/lang/invoke/LambdaMetafactory.html#altMetafactory-java.lang.invoke.MethodHandles.Lookup-java.lang.String-java.lang.invoke.MethodType-java.lang.Object...-
     *
     * @param altMetafactoryArgs `Object... args` of altMetafactory parameters
     * @return A tuple containing an IndexSeq of markerInterfaces, bridges and a boolean indicating
     *         if the class must be serializable.
     */
    def extractAltMetafactoryArguments(
        altMetafactoryArgs: Seq[BootstrapArgument]
    ): (IndexedSeq[ReferenceType], IndexedSeq[MethodDescriptor], Boolean) = {
        var markerInterfaces = IndexedSeq.empty[ReferenceType]
        var bridges = IndexedSeq.empty[MethodDescriptor]
        var isSerializable = false

        if (altMetafactoryArgs.isEmpty) {
            return (markerInterfaces, bridges, isSerializable);
        }

        var argCount = 0
        val ConstantInteger(flags) = altMetafactoryArgs(argCount)
        argCount += 1

        // Extract the marker interfaces. They are the first in the argument list if the flag
        // FLAG_MARKERS is present.
        if ((flags & LambdaMetafactory.FLAG_MARKERS) > 0) {
            val ConstantInteger(markerCount) = altMetafactoryArgs(argCount)
            argCount += 1
            markerInterfaces = altMetafactoryArgs.iterator
                .slice(argCount, argCount + markerCount)
                .map { case ConstantClass(value) => value }
                .toIndexedSeq
            argCount += markerCount
        }

        // bridge methods come afterwards if FLAG_BRIDGES is set.
        if ((flags & LambdaMetafactory.FLAG_BRIDGES) > 0) {
            val ConstantInteger(bridgesCount) = altMetafactoryArgs(argCount)
            argCount += 1
            bridges = altMetafactoryArgs.iterator
                .slice(argCount, argCount + bridgesCount)
                .map { case md: MethodDescriptor => md }
                .toIndexedSeq
            argCount += bridgesCount
        }

        // Check if the FLAG_SERIALIZABLE is set.
        isSerializable = (flags & LambdaMetafactory.FLAG_SERIALIZABLE) > 0

        (markerInterfaces, bridges, isSerializable)
    }

    def storeProxy(
        classFile: ClassFile,
        proxy:     ClassFile,
        reason:    Option[AnyRef]
    ): ClassFile = {
        classFile.synthesizedClassFiles match {
            case Some(scf @ SynthesizedClassFiles(cfs)) =>
                val newScf = new SynthesizedClassFiles((proxy, reason) :: cfs)
                val newAttributes = classFile.attributes.filter(_ ne scf) :+ newScf
                classFile._UNSAFE_replaceAttributes(newAttributes)
            case None =>
                val attributes = classFile.attributes
                val newAttributes = attributes :+ new SynthesizedClassFiles(List((proxy, reason)))
                classFile._UNSAFE_replaceAttributes(newAttributes)
        }
    }
}

object InvokedynamicRewriting {

    final val DefaultDeserializeLambdaStaticMethodName = "$deserializeLambda"

    final val LambdaNameRegEx = "[^.;\\[]*:[0-9]+\\$Lambda$"

    final val TargetMethodNameRegEx = "\\$[A-Za-z_]+\\$[^.;\\[/<>]*:[0-9]+$"

    final val InvokedynamicKeyPrefix = {
        ClassFileReaderConfiguration.ConfigKeyPrefix+"Invokedynamic."
    }

    final val InvokedynamicRewritingConfigKey = {
        InvokedynamicKeyPrefix+"rewrite"
    }

    final val LambdaExpressionsLogRewritingsConfigKey = {
        InvokedynamicKeyPrefix+"logLambdaRewrites"
    }

    final val StringConcatLogRewritingsConfigKey = {
        InvokedynamicKeyPrefix+"logStringConcatRewrites"
    }

    final val ObjectMethodsLogRewritingsConfigKey = {
        InvokedynamicKeyPrefix+"logObjectMethodsRewrites"
    }

    final val InvokedynamicLogUnknownInvokeDynamicsConfigKey = {
        InvokedynamicKeyPrefix+"logUnknownInvokeDynamics"
    }

    def isJava8LikeLambdaExpression(invokedynamic: INVOKEDYNAMIC): Boolean = {
        import ObjectType.LambdaMetafactory
        invokedynamic.bootstrapMethod.handle match {
            case InvokeStaticMethodHandle(LambdaMetafactory, false, name, descriptor) =>
                if (name == "metafactory") {
                    descriptor == LambdaMetafactoryDescriptor
                } else {
                    name == "altMetafactory" && descriptor == LambdaAltMetafactoryDescriptor
                }
            case _ => false
        }
    }

    def isScalaLambdaDeserializeExpression(invokedynamic: INVOKEDYNAMIC): Boolean = {
        import MethodDescriptor.ScalaLambdaDeserializeDescriptor
        import ObjectType.ScalaLambdaDeserialize
        invokedynamic.bootstrapMethod.handle match {
            case InvokeStaticMethodHandle(
                ScalaLambdaDeserialize, false, "bootstrap", ScalaLambdaDeserializeDescriptor
                ) => true
            case _ => false
        }
    }

    def isScalaSymbolExpression(invokedynamic: INVOKEDYNAMIC): Boolean = {
        import MethodDescriptor.ScalaSymbolLiteralDescriptor
        import ObjectType.ScalaSymbolLiteral
        invokedynamic.bootstrapMethod.handle match {
            case InvokeStaticMethodHandle(
                ScalaSymbolLiteral, false, "bootstrap", ScalaSymbolLiteralDescriptor
                ) => true
            case _ => false
        }
    }

    def isScalaStructuralCallSite(
        invokedynamic: INVOKEDYNAMIC,
        instructions:  Array[Instruction],
        pc:            Int
    ): Boolean = {
        import MethodDescriptor.ScalaStructuralCallSiteDescriptor
        import ObjectType.ScalaStructuralCallSite
        invokedynamic.bootstrapMethod.handle match {
            case InvokeStaticMethodHandle(
                ScalaStructuralCallSite, false, "bootstrap", ScalaStructuralCallSiteDescriptor
                ) =>
                pc == 0 &&
                    instructions.length == 44 &&
                    instructions(22).isInstanceOf[LoadString] &&
                    instructions(28) == INVOKEVIRTUAL(
                        ObjectType.Class,
                        "getMethod",
                        MethodDescriptor(
                            ArraySeq(ObjectType.String, ArrayType(ObjectType.Class)),
                            ObjectType.Method
                        )
                    ) &&
                        instructions(31) == INVOKEVIRTUAL(
                            ObjectType("scala/runtime/ScalaRunTime$"),
                            "ensureAccessible",
                            MethodDescriptor(ObjectType.Method, ObjectType.Method)
                        )

            case _ => false
        }
    }

    def isGroovyInvokedynamic(invokedynamic: INVOKEDYNAMIC): Boolean = {
        invokedynamic.bootstrapMethod.handle match {
            case ismh: InvokeStaticMethodHandle if ismh.receiverType.isObjectType =>
                ismh.receiverType.asObjectType.packageName.startsWith("org/codehaus/groovy")
            case _ => false
        }
    }

    def isDynamoInvokedynamic(invokedynamic: INVOKEDYNAMIC): Boolean = {
        invokedynamic.bootstrapMethod.handle match {
            case ismh: InvokeStaticMethodHandle if ismh.receiverType.isObjectType =>
                ismh.receiverType.asObjectType.fqn == "org/dynamo/rt/DynamoBootstrap"
            case _ => false
        }
    }

    def isJava10StringConcatInvokedynamic(invokedynamic: INVOKEDYNAMIC): Boolean = {
        invokedynamic.bootstrapMethod.handle match {
            case InvokeStaticMethodHandle(ObjectType.StringConcatFactory, _, _, _) => true
            case _ => false
        }
    }

    def isObjectMethodsInvokedynamic(invokedynamic: INVOKEDYNAMIC): Boolean = {
        invokedynamic.bootstrapMethod.handle match {
            case InvokeStaticMethodHandle(ObjectType.ObjectMethods, _, _, _) => true
            case _ => false
        }
    }

    /**
     * Returns the default config where the settings for rewriting and logging rewrites are
     * set to the specified values.
     */
    def defaultConfig(rewrite: Boolean, logRewrites: Boolean): Config = {
        val rewritingConfigKey = InvokedynamicRewritingConfigKey
        val logLambdaConfigKey = LambdaExpressionsLogRewritingsConfigKey
        val logConcatConfigKey = StringConcatLogRewritingsConfigKey
        val logObjectMethodsConfigKey = ObjectMethodsLogRewritingsConfigKey
        BaseConfig.
            withValue(rewritingConfigKey, ConfigValueFactory.fromAnyRef(rewrite)).
            withValue(logLambdaConfigKey, ConfigValueFactory.fromAnyRef(logRewrites)).
            withValue(logConcatConfigKey, ConfigValueFactory.fromAnyRef(logRewrites)).
            withValue(logObjectMethodsConfigKey, ConfigValueFactory.fromAnyRef(logRewrites))
    }
}

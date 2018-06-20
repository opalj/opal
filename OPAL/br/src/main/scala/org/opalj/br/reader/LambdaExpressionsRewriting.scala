/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2017
 * Software Technology Group
 * Department of Computer Science
 * Technische Universität Darmstadt
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  - Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package org.opalj
package br
package reader

import java.lang.invoke.LambdaMetafactory
import java.util.concurrent.atomic.AtomicInteger

import scala.annotation.switch
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import com.typesafe.config.ConfigValueFactory
import org.opalj.bi.AccessFlags
import org.opalj.bi.ACC_PRIVATE
import org.opalj.bi.ACC_PUBLIC
import org.opalj.bi.ACC_STATIC
import org.opalj.collection.immutable.UIDSet
import org.opalj.log.OPALLogger.info
import org.opalj.log.Info
import org.opalj.log.OPALLogger.error
import org.opalj.log.OPALLogger
import org.opalj.log.StandardLogMessage
import org.opalj.br.MethodDescriptor.LambdaMetafactoryDescriptor
import org.opalj.br.MethodDescriptor.LambdaAltMetafactoryDescriptor
import org.opalj.br.instructions._
import org.opalj.br.instructions.ClassFileFactory.DefaultFactoryMethodName
import org.opalj.br.instructions.ClassFileFactory.AlternativeFactoryMethodName

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
 *      for furhter information.
 *
 * @author Arne Lottmann
 * @author Michael Eichberg
 * @author Andreas Muttscheller
 */
trait LambdaExpressionsRewriting extends DeferredInvokedynamicResolution {
    this: ClassFileBinding ⇒

    import LambdaExpressionsRewriting._

    val performLambdaExpressionsRewriting: Boolean = {
        import LambdaExpressionsRewriting.{LambdaExpressionsRewritingConfigKey ⇒ Key}
        val rewrite: Boolean =
            try {
                config.getBoolean(Key)
            } catch {
                case t: Throwable ⇒
                    error("class file reader", s"couldn't read: $Key", t)
                    false
            }
        if (rewrite) {
            info(
                "class file reader",
                "invokedynamics using LambdaMetaFactory are rewritten"
            )
        } else {
            info(
                "class file reader",
                "invokedynamics using LambdaMetaFactory are not rewritten"
            )
        }
        rewrite
    }

    val logLambdaExpressionsRewrites: Boolean = {
        import LambdaExpressionsRewriting.{LambdaExpressionsLogRewritingsConfigKey ⇒ Key}
        val logRewrites: Boolean =
            try {
                config.getBoolean(Key)
            } catch {
                case t: Throwable ⇒
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

    val logUnknownInvokeDynamics: Boolean = {
        import LambdaExpressionsRewriting.{LambdaExpressionsLogUnknownInvokeDynamicsConfigKey ⇒ Key}
        val logUnknownInvokeDynamics: Boolean =
            try {
                config.getBoolean(Key)
            } catch {
                case t: Throwable ⇒
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

    /**
     * Counter to ensure that the generated types have unique names.
     */
    private final val jreLikeLambdaTypeIdGenerator = new AtomicInteger(0)

    /**
     * Generates a new, internal name for a lambda expression found in the given
     * `surroundingType`.
     *
     * It follows the pattern: `{surroundingType.simpleName}Lambda${surroundingType.id}:{uniqueId}`,
     * where `uniqueId` is simply a run-on counter. For example: `Lambda$17:4` would refer to
     * the fourth Lambda INVOKEDYNAMIC parsed during the analysis of the project, which
     * is defined in the [[ClassFile]] with the type id `17`.
     *
     * @param surroundingType the type in which the Lambda expression has been found
     */
    private def newLambdaTypeName(surroundingType: ObjectType): String = {
        val nextId = jreLikeLambdaTypeIdGenerator.getAndIncrement()
        s"${surroundingType.packageName}/${surroundingType.simpleName}$$Lambda$$+" +
            s"${surroundingType.id.toHexString}:${nextId.toHexString}"
    }

    override def deferredInvokedynamicResolution(
        classFile:         ClassFile,
        cp:                Constant_Pool,
        invokeDynamicInfo: CONSTANT_InvokeDynamic_info,
        instructions:      Array[Instruction],
        pc:                PC
    ): ClassFile = {
        // gather complete information about invokedynamic instructions from the bootstrap
        // method table
        var updatedClassFile =
            super.deferredInvokedynamicResolution(
                classFile,
                cp,
                invokeDynamicInfo,
                instructions,
                pc
            )

        if (!performLambdaExpressionsRewriting)
            return updatedClassFile;

        // We have to rewrite the synthetic lambda methods to be accessible from the same package.
        // This is necessary, because the java / scala compiler introduces a private synthetic
        // method which handles primitive type handling. Since the new proxyclass is a different
        // class, we have to make the synthetic method accessible from the proxy class.
        updatedClassFile = updatedClassFile.copy(
            methods = classFile.methods.map { m ⇒
                val name = m.name
                if ((name.startsWith("lambda$") || name.equals("$deserializeLambda$")) &&
                    m.hasFlags(AccessFlags.ACC_SYNTHETIC_STATIC_PRIVATE)) {
                    val syntheticLambdaMethodAccessFlags =
                        if (updatedClassFile.isInterfaceDeclaration) {
                            // An interface method must have either ACC_PUBLIC or ACC_PRIVATE and
                            // NOT have ACC_FINAL set. Since the lambda class is in a different
                            // class in the same package, we have to declare the method as public.
                            // For more information see:
                            //   https://docs.oracle.com/javase/specs/jvms/se8/html/jvms-4.html#jvms-4.6
                            (m.accessFlags & ~ACC_PRIVATE.mask) | ACC_PUBLIC.mask
                        } else {
                            // Make the class package private if it is private, so the lambda can
                            // access its methods.
                            // Note: No access modifier means package private
                            m.accessFlags & ~ACC_PRIVATE.mask
                        }
                    m.copy(accessFlags = syntheticLambdaMethodAccessFlags)
                } else {
                    m.copy()
                }
            }
        )

        val invokedynamic = instructions(pc).asInstanceOf[INVOKEDYNAMIC]
        if (LambdaExpressionsRewriting.isJava8LikeLambdaExpression(invokedynamic)) {
            java8LambdaResolution(updatedClassFile, instructions, pc, invokedynamic)
        } else if (isScalaLambdaDeserializeExpression(invokedynamic)) {
            scalaLambdaDeserializeResolution(updatedClassFile, instructions, pc, invokedynamic)
        } else if (isScalaSymbolExpression(invokedynamic)) {
            scalaSymbolResolution(updatedClassFile, instructions, pc, invokedynamic)
        } else if (isGroovyInvokedynamic(invokedynamic)) {
            if (logUnknownInvokeDynamics) {
                OPALLogger.logOnce(StandardLogMessage(
                    Info,
                    Some("load-time transformation"),
                    "Groovy's INVOKEDYNAMICs are not rewritten"
                ))
            }
            updatedClassFile
        } else if (isJava10StringConcatInvokedynamic(invokedynamic)) {
            if (logUnknownInvokeDynamics) {
                OPALLogger.logOnce(StandardLogMessage(
                    Info,
                    Some("load-time transformation"),
                    "Java10's StringConcatFactory INVOKEDYNAMICs are not yet rewritten"
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
     * Resolve invokedynamic instructions introduced by scala.deprecatedName.
     *
     * @param classFile The classfile to parse
     * @param instructions The instructions of the method we are currently parsing
     * @param pc The program counter of the current instuction
     * @param invokedynamic The INVOKEDYNAMIC instruction we want to replace
     * @return A classfile which has the INVOKEDYNAMIC instruction replaced
     */
    private def scalaSymbolResolution(
        classFile:     ClassFile,
        instructions:  Array[Instruction],
        pc:            PC,
        invokedynamic: INVOKEDYNAMIC
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
                MethodDescriptor(IndexedSeq(ObjectType.String), ObjectType.ScalaSymbol)
            )

        if (logLambdaExpressionsRewrites) {
            info("rewriting invokedynamic", s"Scala: $invokedynamic ⇒ $newInvokestatic")
        }

        instructions(pc) = LDC(bootstrapArguments.head.asInstanceOf[ConstantString])
        instructions(pc + 2) = newInvokestatic

        classFile
    }

    /**
     * The scala compiler (and possibly other comilers targeting JVM bytecode) add a
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
        classFile:     ClassFile,
        instructions:  Array[Instruction],
        pc:            PC,
        invokedynamic: INVOKEDYNAMIC
    ): ClassFile = {
        val bootstrapArguments = invokedynamic.bootstrapMethod.arguments

        assert(
            bootstrapArguments.isEmpty || bootstrapArguments.head
                .isInstanceOf[InvokeStaticMethodHandle],
            "ensures that the test isScalaLambdaDeserialize was executed"
        )

        val typeDeclaration = TypeDeclaration(
            ObjectType(newLambdaTypeName(classFile.thisType)),
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
                IndexedSeq(ObjectType.SerializedLambda),
                ObjectType.Object
            )
        )

        if (logLambdaExpressionsRewrites) {
            info("rewriting invokedynamic", s"Scala: $invokedynamic ⇒ $newInvokestatic")
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
        classFile:     ClassFile,
        instructions:  Array[Instruction],
        pc:            PC,
        invokedynamic: INVOKEDYNAMIC
    ): ClassFile = {
        val INVOKEDYNAMIC(
            bootstrapMethod, functionalInterfaceMethodName, factoryDescriptor
            ) = invokedynamic
        val bootstrapArguments = bootstrapMethod.arguments
        // Extract the arguments of the lambda factory.
        val Seq(
            samMethodType: MethodDescriptor, // describing the implemented method type
            tempImplMethod: MethodCallMethodHandle, // the MethodHandle providing the implementation
            instantiatedMethodType: MethodDescriptor, // allowing restrictions on invocation
            // The available information depends on the metafactory:
            // (e.g., about bridges or markers)
            altMetafactoryArgs @ _*
            ) = bootstrapArguments
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
            case specialImplMethod: InvokeSpecialMethodHandle ⇒
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
            case _ ⇒
        }

        val superInterfaceTypesBuilder = UIDSet.newBuilder[ObjectType]
        superInterfaceTypesBuilder += factoryDescriptor.returnType.asObjectType
        if (serializable) {
            superInterfaceTypesBuilder += ObjectType.Serializable
        }
        markerInterfaces foreach { mi ⇒
            superInterfaceTypesBuilder += mi.asObjectType
        }

        val typeDeclaration = TypeDeclaration(
            ObjectType(newLambdaTypeName(thisType)),
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

                case _: InvokeInterfaceMethodHandle    ⇒ true
                case _: InvokeVirtualMethodHandle      ⇒ false
                case _: NewInvokeSpecialMethodHandle   ⇒ false
                case handle: InvokeSpecialMethodHandle ⇒ handle.isInterface

                case handle: InvokeStaticMethodHandle ⇒
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

                case other ⇒ throw new UnknownError("unexpected handle: "+other)
            }

        // Creates forwarding method for private method `m` that can be accessed by the proxy class.
        def createForwardingMethod(
            m:          Method,
            name:       String,
            descriptor: MethodDescriptor
        ): MethodTemplate = {

            // Access flags for the forwarder are the same as the target method but without private
            // modifier. For interfaces, ACC_PUBLIC is required and constructors are forwarded by
            // a static method.
            var accessFlags = m.accessFlags & ~ACC_PRIVATE.mask
            if (receiverIsInterface) accessFlags |= ACC_PUBLIC.mask
            if (m.isConstructor) accessFlags |= ACC_STATIC.mask

            // if the receiver method is not static, we need to push the receiver object
            // onto the stack by an ALOAD_0 unless we have a method reference where the receiver
            // will be explicitly provided
            val loadReceiverObject: Array[Instruction] =
                if (invocationInstruction == INVOKESTATIC.opcode) {
                    Array()
                } else if (m.isConstructor) {
                    Array(NEW(receiverType), null, null, DUP)
                } else {
                    Array(ALOAD_0)
                }

            // If the forwarder is not static, `this` occupies variable 0.
            val variableOffset = if ((accessFlags & ACC_STATIC.mask) != 0) 0 else 1

            // Instructions to push the parameters onto the stack
            val forwardParameters = ClassFileFactory.parameterForwardingInstructions(
                descriptor, descriptor, variableOffset, Seq.empty, classFile.thisType
            )

            val recType = implMethod.receiverType.asObjectType

            // The call instruction itself
            val forwardCall: Array[Instruction] = (invocationInstruction: @switch) match {
                case INVOKESTATIC.opcode ⇒
                    val invoke = INVOKESTATIC(recType, receiverIsInterface, m.name, m.descriptor)
                    Array(invoke, null, null)

                case INVOKESPECIAL.opcode ⇒
                    val invoke = INVOKESPECIAL(recType, receiverIsInterface, m.name, m.descriptor)
                    Array(invoke, null, null)

                case INVOKEINTERFACE.opcode ⇒
                    val invoke = INVOKEINTERFACE(recType, m.name, m.descriptor)
                    Array(invoke, null, null, null, null)

                case INVOKEVIRTUAL.opcode ⇒
                    val invoke = INVOKEVIRTUAL(implMethod.receiverType, m.name, m.descriptor)
                    Array(invoke, null, null)
            }

            // The return instruction matching the return type
            val returnInst = ReturnInstruction(descriptor.returnType)

            val bytecodeInsts = loadReceiverObject ++ forwardParameters ++ forwardCall :+ returnInst

            val receiverObjectStackSize = if (invocationInstruction == INVOKESTATIC.opcode) 0 else 1
            val parametersStackSize = implMethod.methodDescriptor.requiredRegisters
            val returnValueStackSize = descriptor.returnType.operandSize

            val maxStack =
                1 + // Required if, e.g., we first have to create and initialize an object;
                    // which is done by "dup"licating the new created, but not yet initialized
                    // object reference on the stack.
                    math.max(receiverObjectStackSize + parametersStackSize, returnValueStackSize)

            val maxLocals = 1 + receiverObjectStackSize + parametersStackSize + returnValueStackSize

            val code = Code(maxStack, maxLocals, bytecodeInsts, IndexedSeq.empty, Seq.empty)

            Method(accessFlags, name, descriptor, Seq(code))
        }

        // If the target method is private, we have to generate a forwarding method that is
        // accessible by the proxy class, i.e. not private.
        val privateTargetMethod = classFile.methods.find { m ⇒
            m.isPrivate && m.name == targetMethodName && m.descriptor == targetMethodDescriptor
        }

        val updatedClassFile = privateTargetMethod match {
            case Some(m) ⇒
                val forwardingName = "$forward$"+m.name
                val descriptor =
                    if (m.isConstructor) m.descriptor.copy(returnType = receiverType)
                    else m.descriptor

                val forwarderO = classFile.findMethod(forwardingName, descriptor)

                val (forwarder, cf) = forwarderO match {
                    case Some(f) ⇒ (f, classFile)
                    case None ⇒
                        val f = createForwardingMethod(m, forwardingName, descriptor)
                        val cf = classFile.copy(methods = classFile.methods.map(_.copy()) :+ f)
                        (f, cf)
                }

                val isInterface = classFile.isInterfaceDeclaration

                // Update the implMethod and other information to match the forwarder
                implMethod = if (forwarder.isStatic) {
                    InvokeStaticMethodHandle(thisType, isInterface, forwardingName, descriptor)
                } else if (isInterface) {
                    InvokeInterfaceMethodHandle(thisType, forwardingName, descriptor)
                } else {
                    InvokeVirtualMethodHandle(thisType, forwardingName, descriptor)
                }
                invocationInstruction = implMethod.opcodeOfUnderlyingInstruction
                receiverType = thisType
                receiverIsInterface = classFile.isInterfaceDeclaration

                cf
            case None ⇒ classFile
        }

        val needsBridgeMethod = samMethodType != instantiatedMethodType

        val bridgeMethodDescriptorBuilder = IndexedSeq.newBuilder[MethodDescriptor]
        if (needsBridgeMethod) {
            bridgeMethodDescriptorBuilder += samMethodType
        }
        // If the bridge has the same method descriptor like the instantiatedMethodType or
        // samMethodType, they are already present in the proxy class. Do not add them again.
        // This happens in scala patternmatching for example.
        bridgeMethodDescriptorBuilder ++= bridges
            .filterNot(_.equals(samMethodType))
            .filterNot(_.equals(instantiatedMethodType))
        val bridgeMethodDescriptors = bridgeMethodDescriptorBuilder.result()

        val proxy: ClassFile = ClassFileFactory.Proxy(
            thisType,
            classFile.isInterfaceDeclaration,
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
            info("rewriting invokedynamic", s"Java: $invokedynamic ⇒ $newInvokestatic")
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
                .map { case ConstantClass(value) ⇒ value }
                .toIndexedSeq
            argCount += markerCount
        }

        // bridge methods come afterwards if FLAG_BRIDGES is set.
        if ((flags & LambdaMetafactory.FLAG_BRIDGES) > 0) {
            val ConstantInteger(bridgesCount) = altMetafactoryArgs(argCount)
            argCount += 1
            bridges = altMetafactoryArgs.iterator
                .slice(argCount, argCount + bridgesCount)
                .map { case md: MethodDescriptor ⇒ md }
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
            case Some(scf @ SynthesizedClassFiles(cfs)) ⇒
                val newScf = new SynthesizedClassFiles((proxy, reason) :: cfs)
                val newAttrs = newScf +: classFile.attributes.filter(_ ne scf)
                classFile.copy(attributes = newAttrs)
            case None ⇒
                val attributes = classFile.attributes
                val newAttrs = new SynthesizedClassFiles(List((proxy, reason))) +: attributes
                classFile.copy(attributes = newAttrs)
        }
    }
}

object LambdaExpressionsRewriting {

    final val DefaultDeserializeLambdaStaticMethodName = "$deserializeLambda"

    final val LambdaNameRegEx = "[a-z0-9\\/.]*Lambda\\$[0-9a-f]+:[0-9a-f]+$"

    final val LambdaExpressionsConfigKeyPrefix = {
        ClassFileReaderConfiguration.ConfigKeyPrefix+"LambdaExpressions."
    }

    final val LambdaExpressionsRewritingConfigKey = {
        LambdaExpressionsConfigKeyPrefix+"rewrite"
    }

    final val LambdaExpressionsLogRewritingsConfigKey = {
        LambdaExpressionsConfigKeyPrefix+"logRewrites"
    }

    final val LambdaExpressionsLogUnknownInvokeDynamicsConfigKey = {
        LambdaExpressionsConfigKeyPrefix+"logUnknownInvokeDynamics"
    }

    def isJava8LikeLambdaExpression(invokedynamic: INVOKEDYNAMIC): Boolean = {
        import ObjectType.LambdaMetafactory
        invokedynamic.bootstrapMethod.handle match {
            case InvokeStaticMethodHandle(LambdaMetafactory, false, name, descriptor) ⇒
                if (name == "metafactory") {
                    descriptor == LambdaMetafactoryDescriptor
                } else {
                    name == "altMetafactory" && descriptor == LambdaAltMetafactoryDescriptor
                }
            case _ ⇒ false
        }
    }

    def isScalaLambdaDeserializeExpression(invokedynamic: INVOKEDYNAMIC): Boolean = {
        import MethodDescriptor.ScalaLambdaDeserializeDescriptor
        import ObjectType.ScalaLambdaDeserialize
        invokedynamic.bootstrapMethod.handle match {
            case InvokeStaticMethodHandle(
                ScalaLambdaDeserialize, false, "bootstrap", ScalaLambdaDeserializeDescriptor
                ) ⇒ true
            case _ ⇒ false
        }
    }

    def isScalaSymbolExpression(invokedynamic: INVOKEDYNAMIC): Boolean = {
        import MethodDescriptor.ScalaSymbolLiteralDescriptor
        import ObjectType.ScalaSymbolLiteral
        invokedynamic.bootstrapMethod.handle match {
            case InvokeStaticMethodHandle(
                ScalaSymbolLiteral, false, "bootstrap", ScalaSymbolLiteralDescriptor
                ) ⇒ true
            case _ ⇒ false
        }
    }

    def isGroovyInvokedynamic(invokedynamic: INVOKEDYNAMIC): Boolean = {
        invokedynamic.bootstrapMethod.handle match {
            case ismh: InvokeStaticMethodHandle if ismh.receiverType.isObjectType ⇒
                ismh.receiverType.asObjectType.packageName.startsWith("org/codehaus/groovy")
            case _ ⇒ false
        }
    }

    def isJava10StringConcatInvokedynamic(invokedynamic: INVOKEDYNAMIC): Boolean = {
        invokedynamic.bootstrapMethod.handle match {
            case InvokeStaticMethodHandle(ObjectType.StringConcatFactory, _, _, _) ⇒ true
            case _ ⇒ false
        }
    }

    /**
     * Returns the default config where the settings for rewriting and logging rewrites are
     * set to the specified values.
     */
    def defaultConfig(rewrite: Boolean, logRewrites: Boolean): Config = {
        val baseConfig: Config = ConfigFactory.load()
        val rewritingConfigKey = LambdaExpressionsRewritingConfigKey
        val logRewritingsConfigKey = LambdaExpressionsLogRewritingsConfigKey
        baseConfig.
            withValue(rewritingConfigKey, ConfigValueFactory.fromAnyRef(rewrite)).
            withValue(logRewritingsConfigKey, ConfigValueFactory.fromAnyRef(logRewrites))
    }
}

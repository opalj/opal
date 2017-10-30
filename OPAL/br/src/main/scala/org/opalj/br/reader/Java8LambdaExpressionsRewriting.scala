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

import java.util.concurrent.atomic.AtomicInteger

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import com.typesafe.config.ConfigValueFactory
import net.ceedubs.ficus.Ficus._

import org.opalj.collection.immutable.UIDSet
import org.opalj.log.OPALLogger.info
import org.opalj.br.MethodDescriptor.LambdaMetafactoryDescriptor
import org.opalj.br.MethodDescriptor.LambdaAltMetafactoryDescriptor
import org.opalj.br.instructions._
import org.opalj.br.instructions.ClassFileFactory.DefaultFactoryMethodName
import org.opalj.br.instructions.ClassFileFactory.AlternativeFactoryMethodName

/**
 * Provides support for rewriting Java 8 lambda or method reference expressions that
 * werwe compiled to [[org.opalj.br.instructions.INVOKEDYNAMIC]] instructions.
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
 * @author Arne Lottmann
 * @author Michael Eichberg
 * @author Andreas Muttscheller
 */
trait Java8LambdaExpressionsRewriting extends DeferredInvokedynamicResolution {
    this: ClassFileBinding ⇒

    import Java8LambdaExpressionsRewriting._

    val performJava8LambdaExpressionsRewriting: Boolean = {
        import Java8LambdaExpressionsRewriting.{Java8LambdaExpressionsRewritingConfigKey ⇒ Key}
        val rewrite: Boolean = config.as[Option[Boolean]](Key).getOrElse(false)
        if (rewrite) {
            info("class file reader", "Java 8 invokedynamics are rewritten")
        } else {
            info("class file reader", "Java 8 invokedynamics are not rewritten")
        }
        rewrite
    }

    val logJava8LambdaExpressionsRewrites: Boolean = {
        import Java8LambdaExpressionsRewriting.{Java8LambdaExpressionsLogRewritingsConfigKey ⇒ Key}
        val logRewrites: Boolean = config.as[Option[Boolean]](Key).getOrElse(false)
        if (logRewrites) {
            info("class file reader", "Java 8 invokedynamic rewrites are logged")
        } else {
            info("class file reader", "Java 8 invokedynamic rewrites are not logged")
        }
        logRewrites
    }

    val logUnknownInvokeDynamics: Boolean = {
        import Java8LambdaExpressionsRewriting.{Java8LambdaExpressionsLogUnknownInvokeDynamicsConfigKey ⇒ Key}
        val logUnknownInvokeDynamics: Boolean = config.as[Option[Boolean]](Key).getOrElse(false)
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
     * It follows the pattern: `Lambda${surroundingType.id}:{uniqueId}`, where
     * `uniqueId` is simply a run-on counter. For example: `Lambda$17:4` would refer to
     * the fourth Lambda INVOKEDYNAMIC parsed during the analysis of the project, which
     * is defined in the [[ClassFile]] with the type id `17`.
     *
     * @param surroundingType the type in which the Lambda expression has been found
     */
    private def newLambdaTypeName(surroundingType: ObjectType): String = {
        val nextId = jreLikeLambdaTypeIdGenerator.getAndIncrement()
        s"Lambda$$${surroundingType.id.toHexString}:${nextId.toHexString}"
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
        val updatedClassFile =
            super.deferredInvokedynamicResolution(
                classFile,
                cp,
                invokeDynamicInfo,
                instructions,
                pc
            )

        if (!performJava8LambdaExpressionsRewriting)
            return updatedClassFile;

        val invokedynamic = instructions(pc).asInstanceOf[INVOKEDYNAMIC]
        if (Java8LambdaExpressionsRewriting.isJava8LikeLambdaExpression(invokedynamic)) {
            java8LambdaResolution(updatedClassFile, instructions, pc, invokedynamic)
        } else if (isScalaLambdaDeserializeExpression(invokedynamic)) {
            scalaLambdaDeserializeResolution(updatedClassFile, instructions, pc, invokedynamic)
        } else if (isScalaSymbolExpression(invokedynamic)) {
            scalaSymbolResolution(updatedClassFile, instructions, pc, invokedynamic)
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
        // IMPROVE Rewrite the code such that we are not forced to use a constant pool entry in
        // the range [0..255]
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

        if (logJava8LambdaExpressionsRewrites) {
            info(
                "load-time transformation",
                s"rewriting Scala Symbols related invokedynamic: $invokedynamic ⇒ $newInvokestatic"
            )
        }
        instructions(pc) = LDC(bootstrapArguments.head.asInstanceOf[ConstantString])
        instructions(pc + 2) = newInvokestatic

        classFile
    }

    /**
     * The scala compiler (and possibly other JVM bytecode compilers) add a
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
     * @param pc The program counter of the current instuction
     * @param invokedynamic The INVOKEDYNAMIC instruction we want to replace
     * @return A classfile which has the INVOKEDYNAMIC instruction replaced
     */
    private def scalaLambdaDeserializeResolution(
        classFile:     ClassFile,
        instructions:  Array[Instruction],
        pc:            PC,
        invokedynamic: INVOKEDYNAMIC
    ): ClassFile = {
        val INVOKEDYNAMIC(
            bootstrapMethod, _, _ // functionalInterfaceMethodName, factoryDescriptor
            ) = invokedynamic
        val bootstrapArguments = bootstrapMethod.arguments

        if (bootstrapArguments.nonEmpty)
            assert(bootstrapArguments.head.isInstanceOf[InvokeStaticMethodHandle])

        val superInterfaceTypes = UIDSet(LambdaMetafactoryDescriptor.returnType.asObjectType)
        val typeDeclaration = TypeDeclaration(
            ObjectType(newLambdaTypeName(classFile.thisType)),
            isInterfaceType = false,
            Some(ObjectType.Object), // we basically create a "CallSiteObject"
            superInterfaceTypes
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

        if (logJava8LambdaExpressionsRewrites) {
            info(
                "load-time transformation",
                s"rewriting Java 8 like invokedynamic: $invokedynamic ⇒ $newInvokestatic"
            )
        }

        instructions(pc) = newInvokestatic
        // since invokestatic is two bytes shorter than invokedynamic, we need to fill
        // the two-byte gap following the invokestatic with NOPs
        instructions(pc + 3) = NOP
        instructions(pc + 4) = NOP

        val reason = Some((classFile, instructions, pc, invokedynamic, newInvokestatic))
        storeProxy(classFile, proxy, reason)
    }

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
        // apparently there are cases in the JRE where there are more than just those
        // three parameters
        // TODO: Why can they be ignored
        val Seq(
            functionalInterfaceDescriptorAfterTypeErasure: MethodDescriptor,
            invokeTargetMethodHandle: MethodCallMethodHandle,
            functionalInterfaceDescriptorBeforeTypeErasure: MethodDescriptor, _*
            ) =
            bootstrapArguments

        val MethodCallMethodHandle(
            targetMethodOwner: ObjectType, targetMethodName, targetMethodDescriptor
            ) = invokeTargetMethodHandle

        val superInterfaceTypes = UIDSet(factoryDescriptor.returnType.asObjectType)
        val typeDeclaration = TypeDeclaration(
            // ObjectType(newLambdaTypeName(targetMethodOwner)),
            ObjectType(newLambdaTypeName(classFile.thisType)),
            isInterfaceType = false,
            Some(ObjectType.Object), // we basically create a "CallSiteObject"
            superInterfaceTypes
        )

        val invocationInstruction = invokeTargetMethodHandle.opcodeOfUnderlyingInstruction

        val receiverDescriptor: MethodDescriptor =
            if (invokeTargetMethodHandle.isInstanceOf[NewInvokeSpecialMethodHandle]) {
                MethodDescriptor(targetMethodDescriptor.parameterTypes, targetMethodOwner)
            } else {
                targetMethodDescriptor
            }

        val needsBridgeMethod = functionalInterfaceDescriptorAfterTypeErasure !=
            functionalInterfaceDescriptorBeforeTypeErasure

        val bridgeMethodDescriptor: Option[MethodDescriptor] =
            if (needsBridgeMethod) {
                Some(functionalInterfaceDescriptorAfterTypeErasure)
            } else {
                None
            }

        val receiverType =
            /*
            Check the type of the invoke instruction using the instruction's opcode.

            targetMethodOwner identifies the class where the method is actually implemented.
            This is wrong for INVOKEVIRTUAL and INVOKEINTERFACE. The call to the proxy class
            is done with the actual class, not the class where the method is implemented.
            Therefore, the receiverType must be the class from the caller, not where the to-
            be-called method is implemented. E.g. LinkedHashSet.contains() is implemented in
            HashSet, but the receiverType and constructor parameter must be LinkedHashSet
            instead of HashSet.

            *** INVOKEVIRTUAL ***
            An INVOKEVIRTUAL is used when the method is defined by a class type
            (not an interface type). (e.g. LinkedHashSet.addAll()).
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
            - instance initialization methods (i.e. constructors) -> Method is implemented
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
            } else if (functionalInterfaceDescriptorBeforeTypeErasure.parameterTypes.nonEmpty &&
                functionalInterfaceDescriptorBeforeTypeErasure.parameterTypes.head.isObjectType) {
                // If we get a instance method reference like `LinkedHashSet::addAll`, get
                // the receiver type from the functional interface. The first parameter is
                // the instance where the method should be called.
                functionalInterfaceDescriptorBeforeTypeErasure.parameterTypes.head.asObjectType
            } else {
                targetMethodOwner
            }

        /*
        It is possible for the receiverType to be different from classFile. In this case,
        check if the receiverType is an interface instead of the classFile.

        The Proxy Factory must get the correct value to build the correct variant of the
        INVOKESTATIC instruction.
         */
        val receiverIsInterface =
            invokeTargetMethodHandle match {

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
                    if (invokeTargetMethodHandle.receiverType eq classFile.thisType) {
                        classFile.isInterfaceDeclaration
                    } else {
                        handle.isInterface
                    }

                case other ⇒ throw new UnknownError("unexpected handle: "+other)
            }

        val proxy: ClassFile = ClassFileFactory.Proxy(
            typeDeclaration,
            functionalInterfaceMethodName,
            functionalInterfaceDescriptorBeforeTypeErasure,
            receiverType,
            // Note, a static lambda method in an interface needs
            // to be called using the correct variant of an invokestatic.
            receiverIsInterface = receiverIsInterface,
            targetMethodName,
            receiverDescriptor,
            invocationInstruction,
            bridgeMethodDescriptor
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
            // the invokedynamic's methodDescriptor (factoryDescriptor) determines
            // the parameters that are actually pushed and popped from/to the stack
            factoryDescriptor.copy(returnType = proxy.thisType)
        )

        if (logJava8LambdaExpressionsRewrites) {
            val m = s"rewriting invokedynamic: $invokedynamic ⇒ $newInvokestatic"
            info("analysis", m)
        }

        instructions(pc) = newInvokestatic
        // since invokestatic is two bytes shorter than invokedynamic, we need to fill
        // the two-byte gap following the invokestatic with NOPs
        instructions(pc + 3) = NOP
        instructions(pc + 4) = NOP

        val reason = Some((classFile, instructions, pc, invokedynamic, newInvokestatic))
        storeProxy(classFile, proxy, reason)
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

object Java8LambdaExpressionsRewriting {

    final val DefaultDeserializeLambdaStaticMethodName = "$deserializeLambda"

    final val LambdaNameRegEx = "^Lambda\\$[0-9a-f]+:[0-9a-f]+$"

    final val LambdaDeserializeNameRegEx = "^LambdaDeserialize\\$[0-9a-f]+:[0-9a-f]+$"

    final val Java8LambdaExpressionsConfigKeyPrefix = {
        ClassFileReaderConfiguration.ConfigKeyPrefix+"Java8LambdaExpressions."
    }

    final val Java8LambdaExpressionsRewritingConfigKey = {
        Java8LambdaExpressionsConfigKeyPrefix+"rewrite"
    }

    final val Java8LambdaExpressionsLogRewritingsConfigKey = {
        Java8LambdaExpressionsConfigKeyPrefix+"logRewrites"
    }

    final val Java8LambdaExpressionsLogUnknownInvokeDynamicsConfigKey = {
        Java8LambdaExpressionsConfigKeyPrefix+"logUnknownInvokeDynamics"
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

    /**
     * Returns the default config where the settings for rewriting and logging rewrites are
     * set to the specified values.
     */
    def defaultConfig(rewrite: Boolean, logRewrites: Boolean): Config = {
        val baseConfig: Config = ConfigFactory.load()
        val rewritingConfigKey = Java8LambdaExpressionsRewritingConfigKey
        val logRewritingsConfigKey = Java8LambdaExpressionsLogRewritingsConfigKey
        baseConfig.
            withValue(rewritingConfigKey, ConfigValueFactory.fromAnyRef(rewrite)).
            withValue(logRewritingsConfigKey, ConfigValueFactory.fromAnyRef(logRewrites))
    }
}

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

    import MethodDescriptor.LambdaMetafactoryDescriptor
    import MethodDescriptor.LambdaAltMetafactoryDescriptor

    val performJava8LambdaExpressionsRewriting: Boolean = {
        import Java8LambdaExpressionsRewriting.{Java8LambdaExpressionsRewritingConfigKey ⇒ Key}
        val rewrite: Boolean = config.as[Option[Boolean]](Key).getOrElse(false)
        if (rewrite) {
            info("project configuration", "Java 8 invokedynamics are rewritten")
        } else {
            info("project configuration", "Java 8 invokedynamics are not rewritten")
        }
        rewrite
    }

    val logJava8LambdaExpressionsRewrites: Boolean = {
        import Java8LambdaExpressionsRewriting.{Java8LambdaExpressionsLogRewritingsConfigKey ⇒ Key}
        val logRewrites: Boolean = config.as[Option[Boolean]](Key).getOrElse(false)
        if (logRewrites) {
            info("project configuration", "Java 8 invokedynamic rewrites are logged")
        } else {
            info("project configuration", "Java 8 invokedynamic rewrites are not logged")
        }
        logRewrites
    }

    /**
     * Counter to ensure that the generated types have unique names.
     */
    private final val typeIdGenerator = new AtomicInteger(0)

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
        val nextId = typeIdGenerator.getAndIncrement()
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
        var updatedClassFile =
            super.deferredInvokedynamicResolution(
                classFile,
                cp,
                invokeDynamicInfo,
                instructions,
                pc
            )

        if (!performJava8LambdaExpressionsRewriting)
            return updatedClassFile;

        val invokedynamic @ INVOKEDYNAMIC(
            bootstrapMethod, functionalInterfaceMethodName, factoryDescriptor
            ) = instructions(pc)
        if (isJava8LambdaExpression(invokedynamic)) {

            val bootstrapArguments = bootstrapMethod.arguments
            // apparently there are cases in the JRE where there are more than just those
            // three parameters
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
                    case handle: InvokeStaticMethodHandle ⇒
                        handle.isInterface
                    case _ ⇒
                        classFile.isInterfaceDeclaration
                }

            val proxy: ClassFile = ClassFileFactory.Proxy(
                typeDeclaration,
                functionalInterfaceMethodName,
                functionalInterfaceDescriptorBeforeTypeErasure,
                receiverType,
                // Note a static lambda method in an interface needs
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

            /*// DEBUG ---
            if (receiverType != targetMethodOwner) {
                println("Rewritten proxy class receiver type from targetMethodOwner to receiverType!\n")
                println("Creating Proxy Class:")
                println(s"\t\ttypeDeclaration = $typeDeclaration")
                println(s"\t\tfunctionalInterfaceMethodName = $functionalInterfaceMethodName")
                println(s"\t\tfunctionalInterfaceDescriptorBeforeTypeErasure = $functionalInterfaceDescriptorBeforeTypeErasure")
                println(s"\t\ttargetMethodOwner = $targetMethodOwner")
                println(s"\t\treceiverType =  $receiverType")
                println(s"\t\ttargetMethodName = $targetMethodName")
                println(s"\t\treceiverDescriptor = $receiverDescriptor")
                println(s"\t\tinvocationInstruction = $invocationInstruction")
                println(s"\t\tbridgeMethodDescriptor = $bridgeMethodDescriptor")
                println(s"$pc: factoryMethod md => ${factoryMethod.descriptor}")
                println(s"$pc: invokedynamic md => ${invokedynamic.methodDescriptor}")
                println(s"$pc:\n$invokedynamic\n=>\n$newInvokestatic\n")
                println()
            }
            // --- DEBUG*/

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
            updatedClassFile = storeProxy(updatedClassFile, proxy, reason)
        }

        updatedClassFile
    }

    def isJava8LambdaExpression(invokedynamic: INVOKEDYNAMIC): Boolean = {
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

    def storeProxy(
        classFile: ClassFile,
        proxy:     ClassFile,
        reason:    Option[AnyRef]
    ): ClassFile = {
        classFile.synthesizedClassFiles match {
            case Some(scf @ SynthesizedClassFiles(cfs)) ⇒
                val newScf = new SynthesizedClassFiles(((proxy, reason)) :: cfs)
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

    final val FactoryNamesRegEx = "^Lambda\\$[0-9a-f]+:[0-9a-f]+$"

    final val Java8LambdaExpressionsConfigKeyPrefix = {
        ClassFileReaderConfiguration.ConfigKeyPrefix+"Java8LambdaExpressions."
    }

    final val Java8LambdaExpressionsRewritingConfigKey = {
        Java8LambdaExpressionsConfigKeyPrefix+"rewrite"
    }

    final val Java8LambdaExpressionsLogRewritingsConfigKey = {
        Java8LambdaExpressionsConfigKeyPrefix+"logRewrites"
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

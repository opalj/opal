/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2014
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
import scala.collection.mutable.ArrayBuffer
import instructions._

/**
 * Provides full support for resolving JDK8 `invokedynamic` instructions that are generated
 * from lambda or method reference expressions. This trait should be mixed in alongside a
 * BytecodeReaderAndBinding, which extracts basic `invokedynamic` information from the
 * BootstrapMethodTable.
 *
 * Specifically, whenever an `invokedynamic` instruction is encountered that is the result
 * of a Lambda/Method reference expression compiled by Oracle's JDK8, it creates a proxy
 * class file that represents the synthetic object that the JVM generates after executing
 * the `invokedynamic` call size. This proxy is then stored in the temporary ClassFile
 * attribute `JDK8SynthesizedClassFiles`. All such ClassFiles will
 * be picked up later for inclusion in the project.
 *
 * @author Arne Lottmann
 */
trait FullJDK8DeferredInvokedynamicResolution extends DeferredInvokedynamicResolution {
    this: ClassFileBinding ⇒

    /**
     * Counter to ensure that the generated types have unique names.
     */
    private val generatedTypeId = new AtomicInteger

    /**
     * Generates a new, internal name for a lambda expression found in the given
     * `surroundingType`.
     *
     * It follows the pattern: `Lambda\$${surroundingType.id}:${uniqueId}`, where
     * `uniqueId` is simply a run-on counter. For example: `Lambda$17:4` would refer to
     * the fourth Lambda INVOKEDYNAMIC parsed during the analysis of the project, which
     * would have been found in the [[ClassFile]] containing the type with id `17`.
     *
     * @param surroundingType the type in which the Lambda expression has been found
     */
    private def newLambdaTypeName(surroundingType: ObjectType): String = {
        val nextId = generatedTypeId.getAndIncrement()
        new StringBuilder("Lambda$").append(surroundingType.id).append(':').append(nextId).toString
    }

    override def deferredInvokedynamicResolution(
        classFile: ClassFile,
        cp: Constant_Pool,
        cpEntry: CONSTANT_InvokeDynamic_info,
        instructions: Array[Instruction],
        index: Int): ClassFile = {

        // gather complete information about invokedynamic instructions from the bootstrap
        // method table
        var updatedClassFile = super.deferredInvokedynamicResolution(classFile, cp, cpEntry,
            instructions, index)

        val invokedynamic = instructions(index).asInstanceOf[INVOKEDYNAMIC]
        val INVOKEDYNAMIC(bootstrapMethod, indyName, indyDescriptor) = invokedynamic
        val bootstrapArguments = bootstrapMethod.bootstrapArguments
        if (isJDK8Invokedynamic(invokedynamic)) {
            // apparently there are cases in the JRE where there are more than just those
            // three parameters
            val Seq(genericDescriptor: MethodDescriptor,
                invokeTargetMethodHandle: MethodCallMethodHandle,
                preciseDescriptor: MethodDescriptor, _*) = bootstrapArguments
            val MethodCallMethodHandle(receiverType: ObjectType, name, descriptor) =
                invokeTargetMethodHandle

            val superInterface: Set[ObjectType] = Set(indyDescriptor.returnType.asObjectType)
            val typeDeclaration = TypeDeclaration(
                ObjectType(newLambdaTypeName(receiverType)),
                false,
                Some(ObjectType.Object),
                superInterface)

            val invocationInstruction = invokeTargetMethodHandle.opcodeOfUnderlyingInstruction

            val receiverDescriptor: MethodDescriptor =
                if (invokeTargetMethodHandle.isInstanceOf[NewInvokeSpecialMethodHandle]) {
                    MethodDescriptor(
                        descriptor.parameterTypes,
                        receiverType
                    )
                } else {
                    descriptor
                }

            var needsBridgeMethod = genericDescriptor.returnType == ObjectType.Object &&
                preciseDescriptor.returnType != ObjectType.Object

            var i = 0
            val genericParams = genericDescriptor.parameterTypes
            val preciseParams = preciseDescriptor.parameterTypes
            val count = genericDescriptor.parametersCount
            while (!needsBridgeMethod && i < count) {
                val gp = genericParams(i)
                val pp = preciseParams(i)
                if (gp != pp && gp == ObjectType.Object) {
                    needsBridgeMethod = true
                }
                i += 1
            }

            val bridgeMethodDescriptor: Option[MethodDescriptor] =
                if (needsBridgeMethod) {
                    Some(genericDescriptor)
                } else None

            val proxy: ClassFile = ClassFileFactory.Proxy(
                typeDeclaration,
                indyName,
                preciseDescriptor,
                receiverType,
                name,
                receiverDescriptor,
                invocationInstruction,
                bridgeMethodDescriptor
            )
            val factoryMethod =
                if (indyName == ClassFileFactory.DefaultFactoryMethodName)
                    proxy.findMethod(ClassFileFactory.AlternativeFactoryMethodName).get
                else
                    proxy.findMethod(ClassFileFactory.DefaultFactoryMethodName).get

            // TODO: log this replacement sometime in the future
            instructions(index) = INVOKESTATIC(
                proxy.thisType,
                factoryMethod.name,
                factoryMethod.descriptor
            )
            updatedClassFile = storeProxy(updatedClassFile, proxy)
        }

        updatedClassFile
    }

    /**
     * Descriptor of the method `java.lang.invoke.LambdaMetafactory.metafactory`.
     */
    val lambdaMetafactoryDescriptor = MethodDescriptor(
        IndexedSeq(ObjectType.MethodHandles$Lookup,
            ObjectType.String,
            ObjectType.MethodType,
            ObjectType.MethodType,
            ObjectType.MethodHandle,
            ObjectType.MethodType
        ),
        ObjectType.CallSite)

    /**
     * Descriptor of the method `java.lang.invoke.LambdaMetafactory.altMetafactory`.
     */
    val lambdaAltMetafactoryDescriptor = MethodDescriptor(
        IndexedSeq(ObjectType.MethodHandles$Lookup,
            ObjectType.String,
            ObjectType.MethodType,
            ArrayType.ArrayOfObjects
        ),
        ObjectType.CallSite)

    def isJDK8Invokedynamic(invokedynamic: INVOKEDYNAMIC): Boolean = {
        if (!invokedynamic.bootstrapMethod.methodHandle.isInstanceOf[InvokeStaticMethodHandle])
            return false

        val InvokeStaticMethodHandle(receiver, name, descriptor) = invokedynamic.bootstrapMethod.methodHandle

        return receiver == ObjectType.LambdaMetafactory &&
            (name == "metafactory" && descriptor == lambdaMetafactoryDescriptor) ||
            (name == "altMetafactory" && descriptor == lambdaAltMetafactoryDescriptor)
    }

    def storeProxy(classFile: ClassFile, proxy: ClassFile): ClassFile =
        classFile.attributes.collectFirst {
            case scf @ SynthesizedClassFiles(proxies) ⇒ {
                val newScf = new SynthesizedClassFiles(proxies :+ proxy)
                val attributes = classFile.attributes.filterNot(_ eq scf)
                classFile.updateAttributes(newScf +: attributes)
            }
            case _ ⇒ {
                val scf = new SynthesizedClassFiles(Seq(proxy))
                classFile.updateAttributes(scf +: classFile.attributes)
            }
        }.get
}
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
package ai
package analyses
package cg

import scala.util.control.ControlThrowable

import org.opalj.log.Info
import org.opalj.log.OPALLogger
import org.opalj.br.Method
import org.opalj.br.MethodDescriptor
import org.opalj.br.ObjectType
import org.opalj.br.ReferenceType
import org.opalj.ai.domain.MethodCallsHandling
import org.opalj.ai.domain.TheProject
import org.opalj.ai.domain.TheCode

/**
 *
 * @author Michael Eichberg
 */
trait MethodCallsDomainWithMethodLockup extends MethodCallsHandling with Callees {
    callingDomain: ValuesFactory with ReferenceValuesDomain with Configuration with TheProject with TheCode ⇒

    protected[this] def doInvoke(
        pc: PC,
        method: Method,
        operands: Operands,
        fallback: () ⇒ MethodCallResult): MethodCallResult

    /**
     * Currently, if we have multiple targets, `fallback` is called and that result is
     * returned.
     */
    protected[this] def doVirtualInvoke(
        pc: PC,
        declaringClassType: ObjectType,
        methodName: String,
        methodDescriptor: MethodDescriptor,
        operands: Operands,
        fallback: () ⇒ MethodCallResult): MethodCallResult = {

        try {
            val receiver = operands.last.asInstanceOf[IsAReferenceValue]
            val receiverUTB = receiver.upperTypeBound
            if (!receiverUTB.hasOneElement ||
                !receiver.upperTypeBound.first.isObjectType)
                return fallback()

            val receiverType = receiverUTB.first.asObjectType
            // We can resolve (statically) all calls where the type information is precise
            // or where the declaring class is final or where the called method is final.

            if (receiver.isPrecise) {
                doNonVirtualInvoke(
                    pc,
                    receiverType, methodName, methodDescriptor, operands,
                    fallback)
            } else {
                project.classFile(receiverType).map { receiverClassFile ⇒
                    if (receiverClassFile.isFinal)
                        doNonVirtualInvoke(
                            pc,
                            receiverType, methodName, methodDescriptor, operands, fallback)
                    else {
                        val targetMethod =
                            if (receiverClassFile.isInterfaceDeclaration)
                                classHierarchy.resolveInterfaceMethodReference(receiverType, methodName, methodDescriptor, project)
                            else
                                classHierarchy.resolveMethodReference(receiverType, methodName, methodDescriptor, project)

                        targetMethod match {
                            case Some(method) if method.isFinal ⇒
                                doInvoke(pc, method, operands, fallback)
                            case _ ⇒
                                fallback()
                        }
                    }
                }.getOrElse {
                    fallback()
                }
            }

        } catch {
            case ct: ControlThrowable ⇒ throw ct
            case t: Throwable ⇒
                OPALLogger.error(
                    "internal, project configuration",
                    "resolving the method reference resulted in an exception: "+
                        project.classFile(declaringClassType).map(cf ⇒ if (cf.isInterfaceDeclaration) "interface " else "class ").getOrElse("") +
                        declaringClassType.toJava+
                        "{ "+methodDescriptor.toJava(methodName)+" }",
                    t)
                fallback()
        }
    }

    protected[this] def doNonVirtualInvoke(
        pc: PC,
        declaringClassType: ObjectType,
        methodName: String,
        methodDescriptor: MethodDescriptor,
        operands: Operands,
        fallback: () ⇒ MethodCallResult): MethodCallResult = {

        try {
            val resolvedMethod =
                project.classFile(declaringClassType) match {
                    case Some(classFile) ⇒
                        if (classFile.isInterfaceDeclaration)
                            classHierarchy.resolveInterfaceMethodReference(
                                declaringClassType, methodName, methodDescriptor, project)
                        else
                            classHierarchy.resolveMethodReference(
                                declaringClassType, methodName, methodDescriptor, project)
                    case _ ⇒
                        return fallback();
                }

            resolvedMethod match {
                case Some(method) ⇒
                    if (method.body.isDefined)
                        doInvoke(pc, method, operands, fallback)
                    else
                        fallback()
                case _ ⇒
                    OPALLogger.logOnce(Info(
                        "project configuration",
                        "method reference cannot be resolved: "+
                            declaringClassType.toJava+
                            "{ /*non virtual*/ "+methodDescriptor.toJava(methodName)+"}"))
                    fallback()
            }
        } catch {
            case ct: ControlThrowable ⇒ throw ct
            case t: Throwable ⇒
                OPALLogger.error(
                    "internal, project configuration",
                    "resolving the method reference resulted in an exception: "+
                        project.classFile(declaringClassType).map(cf ⇒ if (cf.isInterfaceDeclaration) "interface " else "class ").getOrElse("") +
                        declaringClassType.toJava+
                        "{ /*non virtual*/ "+methodDescriptor.toJava(methodName)+"}",
                    t)
                fallback()
        }
    }

    // -----------------------------------------------------------------------------------
    //
    // Implementation of the invoke instructions
    //
    // -----------------------------------------------------------------------------------

    abstract override def invokevirtual(
        pc: PC,
        declaringClass: ReferenceType,
        methodName: String,
        methodDescriptor: MethodDescriptor,
        operands: Operands): MethodCallResult = {

        def fallback() =
            super.invokevirtual(pc, declaringClass, methodName, methodDescriptor, operands)

        if (declaringClass.isArrayType)
            fallback()
        else
            doVirtualInvoke(
                pc,
                declaringClass.asObjectType, methodName, methodDescriptor,
                operands,
                fallback
            )
    }

    abstract override def invokeinterface(
        pc: PC,
        declaringClass: ObjectType,
        methodName: String,
        methodDescriptor: MethodDescriptor,
        operands: Operands): MethodCallResult = {

        def fallback() =
            super.invokeinterface(pc, declaringClass, methodName, methodDescriptor, operands)

        doVirtualInvoke(
            pc, declaringClass, methodName, methodDescriptor, operands, fallback
        )
    }

    abstract override def invokespecial(
        pc: PC,
        declaringClass: ObjectType,
        methodName: String,
        methodDescriptor: MethodDescriptor,
        operands: Operands): MethodCallResult = {

        def fallback() =
            super.invokespecial(pc, declaringClass, methodName, methodDescriptor, operands)

        doNonVirtualInvoke(
            pc, declaringClass, methodName, methodDescriptor, operands, fallback
        )
    }

    /**
     * Those `invokestatic` calls for which we have no concrete method (e.g.,
     * the respective class file was never loaded or the method is native) or
     * if have a recursive invocation are delegated to the super class.
     */
    abstract override def invokestatic(
        pc: PC,
        declaringClass: ObjectType,
        methodName: String,
        methodDescriptor: MethodDescriptor,
        operands: Operands): MethodCallResult = {

        def fallback() =
            super.invokestatic(pc, declaringClass, methodName, methodDescriptor, operands)

        doNonVirtualInvoke(
            pc, declaringClass, methodName, methodDescriptor, operands, fallback
        )
    }

}


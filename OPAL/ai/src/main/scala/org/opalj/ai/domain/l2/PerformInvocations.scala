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
package domain
package l2

import org.opalj.log.OPALLogger
import org.opalj.log.Warn
import org.opalj.log.Error
import org.opalj.br._
import scala.util.control.ControlThrowable

/**
 * Mix in this trait if methods that are called by `invokeXYZ` instructions should
 * actually be interpreted using a custom domain.
 *
 * @author Michael Eichberg
 */
trait PerformInvocations extends MethodCallsHandling {
    callingDomain: ValuesFactory with ReferenceValuesDomain with Configuration with TheProject with TheCode ⇒

    /**
     * If `true` the exceptions thrown by the called method are will be used
     * during the evaluation of the calling method.
     */
    def useExceptionsThrownByCalledMethod: Boolean = false

    type CalledMethodDomain <: TargetDomain with MethodCallResults

    /**
     * The domain that will be used to perform the abstract interpretation of the
     * called method.
     *
     * In general, explicit support is required to identify recursive calls
     * if the domain also follows method invocations,
     */
    protected[this] def calledMethodDomain(classFile: ClassFile, method: Method): CalledMethodDomain

    /**
     *  The abstract interpreter that will be used for the abstract interpretation.
     */
    def calledMethodAI: AI[_ >: CalledMethodDomain]

    protected[this] def doInvoke(
        method: Method,
        calledMethodDomain: CalledMethodDomain)(
            parameters: calledMethodDomain.Locals): AIResult { val domain: calledMethodDomain.type } = {
        val noOperands = List.empty[calledMethodDomain.DomainValue]
        val isStrict = method.isStrict
        val code = method.body.get
        calledMethodAI.performInterpretation(
            isStrict, code, calledMethodDomain)(
                noOperands, parameters)
    }

    /**
     * Converts the results (`DomainValue`s) of the evaluation of the called
     * method into the calling domain.
     *
     * If the returned value is one of the parameters (determined using reference
     * identity), then the parameter is mapped back to the original operand.
     */
    protected[this] def transformResult(
        callerPC: PC,
        calledMethod: Method,
        originalOperands: callingDomain.Operands,
        calledMethodDomain: CalledMethodDomain)(
            passedParameters: calledMethodDomain.Locals,
            result: AIResult { val domain: calledMethodDomain.type }): MethodCallResult = {

        if (useExceptionsThrownByCalledMethod) {
            val domain = result.domain
            val thrownExceptions = domain.thrownExceptions(callingDomain, callerPC)
            if (!domain.returnedNormally) {
                // The method must have returned with an exception or not at all...
                if (thrownExceptions.nonEmpty)
                    ThrowsException(thrownExceptions)
                else
                    ComputationFailed
            } else {
                if (calledMethod.descriptor.returnType eq VoidType) {
                    if (thrownExceptions.nonEmpty) {
                        ComputationWithSideEffectOrException(thrownExceptions)
                    } else {
                        ComputationWithSideEffectOnly
                    }
                } else {
                    val returnedValue =
                        domain.returnedValueRemapped(
                            callingDomain, callerPC)(
                                originalOperands, passedParameters)
                    if (thrownExceptions.nonEmpty) {
                        ComputedValueOrException(returnedValue.get, thrownExceptions)
                    } else {
                        ComputedValue(returnedValue.get)
                    }
                }
            }
        } else {
            val returnedValue =
                calledMethodDomain.returnedValueRemapped(
                    callingDomain, callerPC)(
                        originalOperands, passedParameters)
            val exceptions = callingDomain.getPotentialExceptions(callerPC)

            if (calledMethod.descriptor.returnType eq VoidType) {
                MethodCallResult(exceptions)
            } else if (returnedValue.isEmpty /*the method always throws an exception*/ ) {
                ThrowsException(exceptions)
            } else {
                MethodCallResult(returnedValue.get, exceptions)
            }
        }
    }

    /**
     * Returns `true` if the given method should be invoked.
     */
    def shouldInvocationBePerformed(definingClass: ClassFile, method: Method): Boolean

    /**
     * Performs the invocation of the given method using the given operands.
     */
    protected[this] def doInvoke(
        pc: PC,
        definingClass: ClassFile,
        method: Method,
        operands: Operands,
        fallback: () ⇒ MethodCallResult): MethodCallResult = {

        assert(definingClass.methods.contains(method))
        assert(
            method.body.isDefined,
            s"the method ${project.source(definingClass.thisType)}: "+
                s"${method.toJava(definingClass)} does not have a body "+
                "(is the project self-consistent?)")

        val calledMethodDomain = this.calledMethodDomain(definingClass, method)
        val parameters = mapOperandsToParameters(operands, method, calledMethodDomain)
        val aiResult = doInvoke(method, calledMethodDomain)(parameters)

        if (aiResult.wasAborted)
            fallback();
        else
            transformResult(pc, method, operands, calledMethodDomain)(parameters, aiResult)
    }

    protected[this] def testAndDoInvoke(
        pc: PC,
        definingClass: ClassFile,
        method: Method,
        operands: Operands,
        fallback: () ⇒ MethodCallResult): MethodCallResult = {

        if (project.isLibraryType(definingClass.thisType))
            return fallback();

        if (method.isAbstract) {
            OPALLogger.logOnce(Error(
                "project configuration",
                "the resolved method on a concrete object is abstract: "+
                    method.toJava(definingClass)))
            fallback()
        } else if (!method.isNative) {
            if (!shouldInvocationBePerformed(definingClass, method))
                fallback()
            else {
                doInvoke(pc, definingClass, method, operands, fallback)
            }
        } else
            fallback()
    }

    // -----------------------------------------------------------------------------------
    //
    // Implementation of the invoke instructions
    //
    // -----------------------------------------------------------------------------------

    protected[this] def doInvokeNonVirtual(
        pc: PC,
        declaringClassType: ObjectType,
        methodName: String,
        methodDescriptor: MethodDescriptor,
        operands: Operands,
        fallback: () ⇒ MethodCallResult): MethodCallResult = {

        val methodOption =
            try {
                classHierarchy.resolveMethodReference(
                    // the cast is safe since arrays do not have any static/special methods
                    declaringClassType.asObjectType,
                    methodName,
                    methodDescriptor,
                    project)
            } catch {
                case ct: ControlThrowable ⇒
                    throw ct;

                case e: AssertionError ⇒
                    OPALLogger.logOnce(Error(
                        "internal error - recoverable",
                        "exception occured while resolving method reference: "+
                            declaringClassType.toJava+
                            "{ static "+methodDescriptor.toJava(methodName)+"}"+
                            ": "+e.getMessage))
                    return fallback();

                case e: Throwable ⇒
                    OPALLogger.error(
                        "internal error - recoverable",
                        "exception occured while resolving method reference: "+
                            declaringClassType.toJava+
                            "{ static "+methodDescriptor.toJava(methodName)+"}",
                        e
                    )
                    return fallback();
            }

        methodOption match {
            case Some(method) ⇒
                val classFile = project.classFile(method)
                testAndDoInvoke(pc, classFile, method, operands, fallback)
            case _ ⇒
                OPALLogger.logOnce(Warn(
                    "project configuration",
                    "method reference cannot be resolved: "+
                        declaringClassType.toJava+
                        "{ static "+methodDescriptor.toJava(methodName)+"}"))
                fallback()
        }
    }

    /**
     * The default implementation only supports the case where we can precisely
     * resolve the target.
     */
    def doInvokeVirtual(
        pc: PC,
        declaringClass: ReferenceType,
        name: String,
        descriptor: MethodDescriptor,
        operands: Operands,
        fallback: () ⇒ MethodCallResult): MethodCallResult = {
        val receiver = operands(descriptor.parametersCount)
        typeOfValue(receiver) match {
            case refValue: IsAReferenceValue if (
                refValue.isPrecise &&
                refValue.isNull.isNo && // TODO handle the case that null is unknown
                refValue.upperTypeBound.hasOneElement &&
                refValue.upperTypeBound.head.isObjectType) ⇒
                val receiverClass = refValue.upperTypeBound.head.asObjectType
                doInvokeNonVirtual(pc, receiverClass, name, descriptor, operands, fallback)

            case _ ⇒
                fallback();
        }

    }

    abstract override def invokevirtual(
        pc: PC,
        declaringClass: ReferenceType,
        name: String,
        descriptor: MethodDescriptor,
        operands: Operands): MethodCallResult = {

        def fallback() =
            super.invokevirtual(pc, declaringClass, name, descriptor, operands)

        doInvokeVirtual(pc, declaringClass, name, descriptor, operands, fallback)

    }

    abstract override def invokeinterface(
        pc: PC,
        declaringClass: ObjectType,
        name: String,
        descriptor: MethodDescriptor,
        operands: Operands): MethodCallResult = {

        def fallback() =
            super.invokeinterface(pc, declaringClass, name, descriptor, operands)

        doInvokeVirtual(pc, declaringClass, name, descriptor, operands, fallback)
    }

    abstract override def invokespecial(
        pc: PC,
        declaringClass: ObjectType,
        methodName: String,
        methodDescriptor: MethodDescriptor,
        operands: Operands): MethodCallResult = {

        def fallback() =
            super.invokespecial(pc, declaringClass, methodName, methodDescriptor, operands)

        doInvokeNonVirtual(
            pc, declaringClass, methodName, methodDescriptor, operands, fallback
        )
    }

    /**
     * For those `invokestatic` calls for which we have no concrete method (e.g.,
     * the respective class file was never loaded or the method is native) or
     * if we have a recursive invocation, the super implementation is called.
     */
    abstract override def invokestatic(
        pc: PC,
        declaringClass: ObjectType,
        methodName: String,
        methodDescriptor: MethodDescriptor,
        operands: Operands): MethodCallResult = {

        def fallback() =
            super.invokestatic(pc, declaringClass, methodName, methodDescriptor, operands)

        doInvokeNonVirtual(
            pc, declaringClass, methodName, methodDescriptor, operands, fallback
        )
    }

}


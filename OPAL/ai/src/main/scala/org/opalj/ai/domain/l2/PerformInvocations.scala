/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai
package domain
package l2

import org.opalj.log.OPALLogger
import org.opalj.log.Warn
import org.opalj.log.Error
import org.opalj.br.Method
import org.opalj.br.VoidType
import org.opalj.br.ObjectType
import org.opalj.br.ReferenceType
import org.opalj.br.MethodDescriptor

/**
 * Mix in this trait if methods that are called by `invokeXYZ` instructions should
 * actually be interpreted using a custom domain.
 *
 * @author Michael Eichberg
 */
trait PerformInvocations extends MethodCallsHandling {
    callingDomain: ValuesFactory with ReferenceValuesDomain with Configuration with TheProject with TheCode =>

    /**
     * If `true` the exceptions thrown by the called method will be used
     * during the evaluation of the calling method.
     */
    def useExceptionsThrownByCalledMethod: Boolean = false

    type CalledMethodDomain <: TargetDomain with MethodCallResults

    /**
     * The domain that will be used to perform the abstract interpretation of the
     * called method.
     *
     * In general, explicit support is required to identify recursive calls
     * if the domain also follows method invocations.
     */
    protected[this] def calledMethodDomain(method: Method): CalledMethodDomain

    /**
     *  The abstract interpreter that will be used for the abstract interpretation.
     */
    def calledMethodAI: AI[_ >: CalledMethodDomain]

    protected[this] def doInvoke(
        method:             Method,
        calledMethodDomain: CalledMethodDomain
    )(
        parameters: calledMethodDomain.Locals
    ): AIResult { val domain: calledMethodDomain.type } = {
        val noOperands: List[calledMethodDomain.DomainValue] = List.empty
        val code = method.body.get
        calledMethodAI.performInterpretation(code, calledMethodDomain)(noOperands, parameters)
    }

    /**
     * Converts the results (`DomainValue`s) of the evaluation of the called
     * method into the calling domain.
     *
     * If the returned value is one of the parameters (determined using reference
     * identity), then the parameter is mapped back to the original operand.
     */
    protected[this] def transformResult(
        callerPC:           Int,
        calledMethod:       Method,
        originalOperands:   callingDomain.Operands,
        calledMethodDomain: CalledMethodDomain
    )(
        passedParameters: calledMethodDomain.Locals,
        result:           AIResult { val domain: calledMethodDomain.type }
    ): MethodCallResult = {

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
                            callingDomain, callerPC
                        )(
                            originalOperands, passedParameters
                        )
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
                    callingDomain, callerPC
                )(
                    originalOperands, passedParameters
                )
            val exceptions = callingDomain.getPotentialExceptions(callerPC)

            if (calledMethod.descriptor.returnType eq VoidType) {
                MethodCallResult(exceptions)
            } else if (returnedValue.isEmpty /* <=> the method always throws an exception */ ) {
                ThrowsException(exceptions)
            } else {
                MethodCallResult(returnedValue.get, exceptions)
            }
        }
    }

    /**
     * Returns `true` if the given method should be invoked.
     */
    def shouldInvocationBePerformed(method: Method): Boolean

    /**
     * Performs the invocation of the given method using the given operands.
     */
    protected[this] def doInvoke(
        pc:       Int,
        method:   Method,
        operands: Operands,
        fallback: () => MethodCallResult
    ): MethodCallResult = {

        assert(
            method.body.isDefined,
            s"${project.source(method.classFile.thisType)} - the method: "+
                s"${method.toJava} does not have a body (is the project self-consistent?)"
        )

        val calledMethodDomain = this.calledMethodDomain(method)
        val parameters = mapOperandsToParameters(operands, method, calledMethodDomain)
        val aiResult = doInvoke(method, calledMethodDomain)(parameters)

        if (aiResult.wasAborted)
            fallback()
        else
            transformResult(pc, method, operands, calledMethodDomain)(parameters, aiResult)
    }

    protected[this] def testAndDoInvoke(
        pc:       Int,
        method:   Method,
        operands: Operands,
        fallback: () => MethodCallResult
    ): MethodCallResult = {

        if (project.libraryClassFilesAreInterfacesOnly &&
            project.isLibraryType(method.classFile.thisType))
            return fallback();

        if (method.isAbstract) {
            OPALLogger.logOnce(Error(
                "project configuration", "resolved method is abstract: "+method.classFile
            ))
            fallback()
        } else if (!method.isNative) {
            if (!shouldInvocationBePerformed(method))
                fallback()
            else {
                doInvoke(pc, method, operands, fallback)
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
        pc:             Int,
        declaringClass: ObjectType, // ... arrays do not have any static/special methods
        isInterface:    Boolean,
        name:           String,
        descriptor:     MethodDescriptor,
        operands:       Operands,
        fallback:       () => MethodCallResult
    ): MethodCallResult = {

        val resolvedMethod =
            if (isInterface)
                project.resolveInterfaceMethodReference(declaringClass, name, descriptor)
            else
                project.resolveMethodReference(declaringClass, name, descriptor)

        resolvedMethod match {
            case Some(method) => testAndDoInvoke(pc, method, operands, fallback)
            case _ =>
                OPALLogger.logOnce(Warn(
                    "project configuration",
                    "method reference cannot be resolved: "+
                        declaringClass.toJava+"{ (static?) "+descriptor.toJava(name)+"}"
                ))
                fallback()
        }
    }

    /**
     * The default implementation only supports the case where we can precisely
     * resolve the target.
     */
    def doInvokeVirtual(
        pc:             Int,
        declaringClass: ReferenceType,
        isInterface:    Boolean,
        name:           String,
        descriptor:     MethodDescriptor,
        operands:       Operands,
        fallback:       () => MethodCallResult
    ): MethodCallResult = {
        val receiver = operands(descriptor.parametersCount)
        receiver match {
            case DomainReferenceValueTag(refValue) if refValue.isPrecise &&
                refValue.isNull.isNo && // IMPROVE support the case that null is unknown
                refValue.upperTypeBound.isSingletonSet &&
                refValue.upperTypeBound.head.isObjectType =>

                val receiverClass = refValue.upperTypeBound.head.asObjectType
                classHierarchy.isInterface(receiverClass) match {
                    case Yes =>
                        doInvokeNonVirtual(
                            pc,
                            receiverClass, true, name, descriptor,
                            operands, fallback
                        )
                    case No =>
                        doInvokeNonVirtual(
                            pc,
                            receiverClass, false, name, descriptor,
                            operands, fallback
                        )
                    case Unknown =>
                        fallback()
                }

            case _ =>
                val resolvedMethod =
                    if (isInterface)
                        if (declaringClass.isObjectType)
                            project.resolveInterfaceMethodReference(
                                declaringClass.asObjectType, name, descriptor
                            )
                        else None
                    else
                        project.resolveMethodReference(declaringClass, name, descriptor)

                resolvedMethod match {
                    case Some(method) if method.isPrivate =>
                        testAndDoInvoke(pc, method, operands, fallback)
                    case _ => fallback()
                }
        }

    }

    abstract override def invokevirtual(
        pc:             Int,
        declaringClass: ReferenceType,
        name:           String,
        descriptor:     MethodDescriptor,
        operands:       Operands
    ): MethodCallResult = {
        def fallback(): MethodCallResult = {
            super.invokevirtual(pc, declaringClass, name, descriptor, operands)
        }
        doInvokeVirtual(pc, declaringClass, false, name, descriptor, operands, fallback _)
    }

    abstract override def invokeinterface(
        pc:             Int,
        declaringClass: ObjectType,
        name:           String,
        descriptor:     MethodDescriptor,
        operands:       Operands
    ): MethodCallResult = {
        def fallback(): MethodCallResult = {
            super.invokeinterface(pc, declaringClass, name, descriptor, operands)
        }
        doInvokeVirtual(pc, declaringClass, true, name, descriptor, operands, fallback _)
    }

    abstract override def invokespecial(
        pc:             Int,
        declaringClass: ObjectType,
        isInterface:    Boolean,
        name:           String,
        descriptor:     MethodDescriptor,
        operands:       Operands
    ): MethodCallResult = {
        def fallback(): MethodCallResult = {
            super.invokespecial(pc, declaringClass, isInterface, name, descriptor, operands)
        }
        doInvokeNonVirtual(pc, declaringClass, isInterface, name, descriptor, operands, fallback _)
    }

    /**
     * For those `invokestatic` calls for which we have no concrete method (e.g.,
     * the respective class file was never loaded or the method is native) or
     * if we have a recursive invocation, the super implementation is called.
     */
    abstract override def invokestatic(
        pc:             Int,
        declaringClass: ObjectType,
        isInterface:    Boolean,
        name:           String,
        descriptor:     MethodDescriptor,
        operands:       Operands
    ): MethodCallResult = {
        def fallback(): MethodCallResult = {
            super.invokestatic(pc, declaringClass, isInterface, name, descriptor, operands)
        }
        doInvokeNonVirtual(pc, declaringClass, isInterface, name, descriptor, operands, fallback _)
    }

}

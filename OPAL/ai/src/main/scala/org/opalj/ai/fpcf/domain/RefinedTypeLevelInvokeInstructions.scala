/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai
package fpcf
package domain

import scala.util.control.ControlThrowable

import org.opalj.log.OPALLogger
import org.opalj.log.Warn
import org.opalj.fpcf.PropertyKind
import org.opalj.br.Method
import org.opalj.br.MethodDescriptor
import org.opalj.br.ObjectType
import org.opalj.br.PC
import org.opalj.br.ReferenceType
import org.opalj.ai.domain.MethodCallsHandling
import org.opalj.ai.domain.TheCode
import org.opalj.ai.domain.TheProject
import org.opalj.ai.fpcf.properties.MethodReturnValue

/**
 *
 * @author Michael Eichberg
 */
trait RefinedTypeLevelInvokeInstructions
    extends MethodCallsDomainWithMethodLockup
    with PropertyStoreBased {
    domain: ValuesFactory with ReferenceValuesDomain with Configuration with TheProject with TheCode ⇒

    abstract override def usesProperties: Set[PropertyKind] = {
        super.usesProperties ++ Set(MethodReturnValue)
    }

    /**
     * Provides a hook for subclasses that need to track the information for which methods
     * refined return value information is actually used.
     *
     * @note Intended to be overridden by subclasses. Subclasses should simply call this
     *       method last to get the correct behavior.
     */
    protected[this] def doInvokeWithRefinedReturnValue(
        calledMethod: Method,
        result:       MethodCallResult
    ): MethodCallResult = {
        result
    }

    protected[this] def doInvoke(
        pc:       PC,
        method:   Method,
        operands: Operands,
        fallback: () ⇒ MethodCallResult
    ): MethodCallResult = {
        if (!method.returnType.isObjectType) {
            return fallback();
        }

        dependees.getOrQueryAndUpdate(method, MethodReturnValue.key) match {
            case UsedPropertiesBound(mrvProperty) ⇒
                mrvProperty.returnValue match {
                    case Some(mrvi) ⇒
                        val vi = domain.InitializedDomainValue(pc, mrvi)
                        val result = MethodCallResult(vi, getPotentialExceptions(pc))
                        doInvokeWithRefinedReturnValue(method, result)
                    case None ⇒
                        // the method always throws an exception... but we don't know which one
                        val potentialExceptions = getPotentialExceptions(pc)
                        ThrowsException(potentialExceptions)
                }
            case _ ⇒
                fallback()
        }
    }
}

/**
 *
 * @author Michael Eichberg
 */
trait MethodCallsDomainWithMethodLockup extends MethodCallsHandling {
    callingDomain: ValuesFactory with ReferenceValuesDomain with Configuration with TheProject with TheCode ⇒

    protected[this] def doInvoke(
        pc:       PC,
        method:   Method,
        operands: Operands,
        fallback: () ⇒ MethodCallResult
    ): MethodCallResult

    /**
     * Currently, if we have multiple targets, `fallback` is called and that result is
     * returned.
     */
    protected[this] def doVirtualInvoke(
        pc:            PC,
        declaringType: ObjectType,
        isInterface:   Boolean,
        name:          String,
        descriptor:    MethodDescriptor,
        operands:      Operands,
        fallback:      () ⇒ MethodCallResult
    ): MethodCallResult = {

        val DomainReferenceValueTag(receiver) = operands.last
        val receiverUTB = receiver.upperTypeBound
        if (!receiverUTB.isSingletonSet || !receiver.upperTypeBound.head.isObjectType)
            return fallback();

        val receiverType = receiverUTB.head.asObjectType
        // We can resolve (statically) all calls where the type information is precise
        // or where the called method is final.

        if (receiver.isPrecise) {
            classHierarchy.isInterface(receiverType) match {
                case Yes ⇒
                    doNonVirtualInvoke(
                        pc, receiverType, true, name, descriptor, operands, fallback
                    )
                case No ⇒
                    doNonVirtualInvoke(
                        pc, receiverType, false, name, descriptor, operands, fallback
                    )
                case Unknown ⇒
                    fallback()
            }
        } else {
            // IMPROVE Use project.instanceMethods
            project.classFile(receiverType).map { receiverClassFile ⇒
                val targetMethod =
                    if (receiverClassFile.isInterfaceDeclaration)
                        project.resolveInterfaceMethodReference(receiverType, name, descriptor)
                    else
                        project.resolveMethodReference(receiverType, name, descriptor)

                targetMethod match {
                    case Some(method) if method.isFinal ⇒ doInvoke(pc, method, operands, fallback)
                    case _                              ⇒ fallback()
                }
            }.getOrElse {
                fallback()
            }
        }
    }

    protected[this] def doNonVirtualInvoke(
        pc:            PC,
        declaringType: ObjectType,
        isInterface:   Boolean,
        name:          String,
        descriptor:    MethodDescriptor,
        operands:      Operands,
        fallback:      () ⇒ MethodCallResult
    ): MethodCallResult = {

        try {
            val resolvedMethod =
                project.classFile(declaringType) match {
                    case Some(classFile) ⇒
                        if (classFile.isInterfaceDeclaration)
                            project.resolveInterfaceMethodReference(declaringType, name, descriptor)
                        else
                            project.resolveMethodReference(declaringType, name, descriptor)
                    case _ ⇒
                        return fallback();
                }

            resolvedMethod match {
                case Some(method) ⇒
                    if (method.body.isDefined)
                        doInvoke(pc, method, operands, fallback)
                    else
                        fallback() // only happens if the project is incomplete/broken
                case _ ⇒
                    OPALLogger.logOnce(Warn(
                        "project configuration",
                        "method reference cannot be resolved: "+
                            declaringType.toJava+
                            "{ /*non virtual*/ "+descriptor.toJava(name)+"}"
                    ))
                    fallback()
            }
        } catch {
            case ct: ControlThrowable ⇒ throw ct
            case t: Throwable ⇒
                OPALLogger.error(
                    "internal, project configuration",
                    "resolving the method reference resulted in an exception: "+
                        project.classFile(declaringType).map(cf ⇒ if (cf.isInterfaceDeclaration) "interface " else "class ").getOrElse("") +
                        declaringType.toJava+"{ /*non virtual*/ "+descriptor.toJava(name)+"}",
                    t
                )
                fallback()
        }
    }

    // -----------------------------------------------------------------------------------
    //
    // Implementation of the invoke instructions
    //
    // -----------------------------------------------------------------------------------

    abstract override def invokevirtual(
        pc:            PC,
        declaringType: ReferenceType,
        name:          String,
        descriptor:    MethodDescriptor,
        operands:      Operands
    ): MethodCallResult = {

        if (declaringType.isArrayType)
            return super.invokevirtual(pc, declaringType, name, descriptor, operands);

        def fallback(): MethodCallResult = {
            super.invokevirtual(pc, declaringType, name, descriptor, operands)
        }

        doVirtualInvoke(
            pc, declaringType.asObjectType, false, name, descriptor, operands, fallback _
        )
    }

    abstract override def invokeinterface(
        pc:            PC,
        declaringType: ObjectType,
        name:          String,
        descriptor:    MethodDescriptor,
        operands:      Operands
    ): MethodCallResult = {

        def fallback(): MethodCallResult = {
            super.invokeinterface(pc, declaringType, name, descriptor, operands)
        }

        doVirtualInvoke(pc, declaringType, true, name, descriptor, operands, fallback _)
    }

    abstract override def invokespecial(
        pc:            PC,
        declaringType: ObjectType,
        isInterface:   Boolean,
        name:          String,
        descriptor:    MethodDescriptor,
        operands:      Operands
    ): MethodCallResult = {

        def fallback(): MethodCallResult = {
            super.invokespecial(
                pc,
                declaringType, isInterface, name, descriptor,
                operands
            )
        }

        doNonVirtualInvoke(
            pc, declaringType, isInterface, name, descriptor, operands, fallback _
        )
    }

    /**
     * Those `invokestatic` calls for which we have no concrete method (e.g.,
     * the respective class file was never loaded or the method is native) or
     * if have a recursive invocation are delegated to the super class.
     */
    abstract override def invokestatic(
        pc:            PC,
        declaringType: ObjectType,
        isInterface:   Boolean,
        name:          String,
        descriptor:    MethodDescriptor,
        operands:      Operands
    ): MethodCallResult = {

        def fallback(): MethodCallResult = {
            super.invokestatic(
                pc,
                declaringType, isInterface, name, descriptor,
                operands
            )
        }

        doNonVirtualInvoke(pc, declaringType, isInterface, name, descriptor, operands, fallback _)
    }

}


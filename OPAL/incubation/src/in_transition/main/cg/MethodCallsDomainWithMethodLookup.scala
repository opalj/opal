/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai
package cg

import scala.util.control.ControlThrowable

import org.opalj.log.Warn
import org.opalj.log.OPALLogger
import org.opalj.br.Method
import org.opalj.br.MethodDescriptor
import org.opalj.br.ObjectType
import org.opalj.br.ReferenceType
import org.opalj.ai.domain.MethodCallsHandling
import org.opalj.ai.domain.TheProject
import org.opalj.ai.domain.TheCode
import org.opalj.ai.analyses.cg.Callees

/**
 *
 * @author Michael Eichberg
 */
trait MethodCallsDomainWithMethodLockup extends MethodCallsHandling with Callees {
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

        val DomainReferenceValue(receiver) = operands.last
        val receiverUTB = receiver.upperTypeBound
        if (!receiverUTB.isSingletonSet || !receiver.upperTypeBound.head.isObjectType)
            return fallback();

        val receiverType = receiverUTB.head.asObjectType
        // We can resolve (statically) all calls where the type information is precise
        // or where the declaring class is final or where the called method is final.

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
            project.classFile(receiverType).map { receiverClassFile ⇒
                if (receiverClassFile.isFinal) {
                    val isInterface = receiverClassFile.isInterfaceDeclaration
                    doNonVirtualInvoke(
                        pc, receiverType, isInterface, name, descriptor, operands, fallback
                    )
                } else {
                    val targetMethod =
                        if (receiverClassFile.isInterfaceDeclaration)
                            project.resolveInterfaceMethodReference(receiverType, name, descriptor)
                        else
                            project.resolveMethodReference(receiverType, name, descriptor)

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
                            project.resolveInterfaceMethodReference(
                                declaringType, name, descriptor
                            )
                        else
                            project.resolveMethodReference(
                                declaringType, name, descriptor
                            )
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

        def fallback(): MethodCallResult = {
            super.invokevirtual(pc, declaringType, name, descriptor, operands)
        }

        if (declaringType.isArrayType)
            fallback()
        else
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

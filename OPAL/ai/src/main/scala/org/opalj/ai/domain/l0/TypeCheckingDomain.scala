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
package ai
package domain
package l0

import scala.reflect.ClassTag

import org.opalj.collection.immutable.UIDSet
import org.opalj.br.ArrayType
import org.opalj.br.ObjectType
import org.opalj.br.Method
import org.opalj.br.MethodDescriptor
import org.opalj.br.ClassHierarchy
import org.opalj.br.VerificationTypeInfo
import org.opalj.br.UninitializedVariableInfo
import org.opalj.br.UninitializedThisVariableInfo
import org.opalj.br.ObjectVariableInfo
import org.opalj.br.analyses.SomeProject

/**
 * Domain that can be used to compute the information required to compute the
 * [[org.opalj.br.StackMapTable]]; i.e., we precisely track the information regarding the
 * initialization status of references. (This is generally not necessary for the other domains
 * because we make the correct bytecode assumption over there and, therefore, never see an
 * invalid usage of an uninitialized object reference.)
 */
final class TypeCheckingDomain(
        val classHierarchy: ClassHierarchy,
        val method:         Method
) extends Domain
    with DefaultDomainValueBinding
    with DefaultTypeLevelIntegerValues
    with DefaultTypeLevelLongValues
    with TypeLevelLongValuesShiftOperators
    with TypeLevelPrimitiveValuesConversions
    with DefaultTypeLevelFloatValues
    with DefaultTypeLevelDoubleValues
    with TypeLevelFieldAccessInstructions
    with TypeLevelInvokeInstructions
    with ThrowAllPotentialExceptionsConfiguration
    with IgnoreSynchronization
    with DefaultTypeLevelHandlingOfMethodResults
    with DefaultTypeLevelReferenceValues
    with PostEvaluationMemoryManagement
    with DefaultExceptionsFactory
    with TheClassHierarchy
    with TheMethod {

    def this(project: SomeProject, method: Method) {
        this(project.classHierarchy, method)
    }

    type AReferenceValue = ReferenceValue
    type DomainReferenceValue = AReferenceValue

    final val DomainReferenceValue: ClassTag[DomainReferenceValue] = implicitly

    type DomainNullValue = NullValue
    type DomainObjectValue = ObjectValue
    type DomainArrayValue = ArrayValue

    val TheNullValue: DomainNullValue = new NullValue()

    // -----------------------------------------------------------------------------------
    //
    // REPRESENTATION OF REFERENCE VALUES
    //
    // -----------------------------------------------------------------------------------

    protected class InitializedObjectValue(
            theUpperTypeBound: ObjectType
    ) extends SObjectValue(theUpperTypeBound) with Value {
        this: DomainObjectValue ⇒

        final override def verificationTypeInfo: VerificationTypeInfo = {
            ObjectVariableInfo(theUpperTypeBound)
        }

        // WIDENING OPERATION
        override protected def doJoin(pc: Int, other: DomainValue): Update[DomainValue] = {
            other match {
                case _: UninitializedObjectValue ⇒ MetaInformationUpdateIllegalValue
                case that                        ⇒ super.doJoin(pc, that)
            }
        }
    }

    /**
     * @param vo The origin of the new instruction or -1 if this represents "uninitialized size".
     */
    protected case class UninitializedObjectValue(
            theType: ObjectType,
            vo:      ValueOrigin
    ) extends SObjectValue(theType) {
        this: DomainObjectValue ⇒

        override def isPrecise: Boolean = vo != -1 // we are talking about uninitialized this

        // joins of an uninitialized value with null results in an illegal value
        override def isNull: Answer = No

        final override def verificationTypeInfo: VerificationTypeInfo = {
            if (vo == -1)
                UninitializedThisVariableInfo
            else
                UninitializedVariableInfo(vo)
        }

        // WIDENING OPERATION
        override protected def doJoin(pc: Int, other: DomainValue): Update[DomainValue] = {
            other match {
                case UninitializedObjectValue(`theType`, `vo`) ⇒ NoUpdate
                // this value is not completely useable...
                case _                                         ⇒ MetaInformationUpdateIllegalValue
            }
        }

        override def abstractsOver(other: DomainValue): Boolean = {
            other match {
                case that: UninitializedObjectValue if (
                    (that.theType eq this.theType) && this.vo == that.vo
                ) ⇒
                    true
                case _ ⇒
                    false
            }
        }

        override def adapt(target: TargetDomain, origin: ValueOrigin): target.DomainValue = {
            target.NewObject(origin, theUpperTypeBound)
        }

        override def toString: String = {
            if (vo == -1)
                "UninitializedThis"
            else
                s"${theType.toJava}(uninitialized;origin=$vo)"
        }
    }

    override def invokespecial(
        pc:               Int,
        declaringClass:   ObjectType,
        isInterface:      Boolean,
        name:             String,
        methodDescriptor: MethodDescriptor,
        operands:         Operands
    ): MethodCallResult = {
        if (name == "<init>") {
            val receiver = operands.last
            // the value is now initialized and we have to update the stack/locals
            val UninitializedObjectValue(theType, _) = receiver
            val initializedObjectValue = new InitializedObjectValue(theType)
            updateAfterExecution(receiver, initializedObjectValue, TheIllegalValue)
        }
        super.invokespecial(pc, declaringClass, isInterface, name, methodDescriptor, operands)
    }

    // -----------------------------------------------------------------------------------
    //
    // FACTORY METHODS
    //
    // -----------------------------------------------------------------------------------

    override def NullValue(origin: ValueOrigin): DomainNullValue = TheNullValue

    override def NewObject(pc: Int, objectType: ObjectType): DomainObjectValue = {
        new UninitializedObjectValue(objectType, pc)
    }

    override def UninitializedThis(objectType: ObjectType): DomainObjectValue = {
        new UninitializedObjectValue(objectType, -1)
    }

    override def InitializedObjectValue(pc: Int, objectType: ObjectType): DomainObjectValue = {
        new InitializedObjectValue(objectType)
    }

    override def ObjectValue(origin: ValueOrigin, objectType: ObjectType): DomainObjectValue = {
        new InitializedObjectValue(objectType)
    }

    override def ObjectValue(
        origin:         ValueOrigin,
        upperTypeBound: UIDSet[ObjectType]
    ): DomainObjectValue = {
        if (upperTypeBound.isSingletonSet)
            ObjectValue(origin, upperTypeBound.head)
        else
            new MObjectValue(upperTypeBound)
    }

    override def ArrayValue(origin: ValueOrigin, arrayType: ArrayType): DomainArrayValue = {
        new ArrayValue(arrayType)
    }

}

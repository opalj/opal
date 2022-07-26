/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai
package domain
package l0

import scala.reflect.ClassTag

import org.opalj.collection.immutable.UIDSet
import org.opalj.br.ArrayType
import org.opalj.br.ClassHierarchy
import org.opalj.br.Method
import org.opalj.br.MethodDescriptor
import org.opalj.br.ObjectType
import org.opalj.br.UninitializedThisVariableInfo
import org.opalj.br.UninitializedVariableInfo
import org.opalj.br.VerificationTypeInfo
import org.opalj.br.analyses.SomeProject

/**
 * Concrete domain that can be used to compute the information required to compute the
 * [[org.opalj.br.StackMapTable]]; that is, we precisely track the information regarding the
 * initialization status of references. (This is generally not necessary for the other domains
 * because we make the correct bytecode assumption over there and, therefore, never see an
 * invalid usage of an uninitialized object reference.)
 */
final class TypeCheckingDomain(
        val classHierarchy: ClassHierarchy,
        val method:         Method
) extends Domain
    with DefaultSpecialDomainValuesBinding
    with DefaultTypeLevelIntegerValues
    with DefaultTypeLevelLongValues
    with TypeLevelLongValuesShiftOperators
    with TypeLevelPrimitiveValuesConversions
    with DefaultTypeLevelFloatValues
    with DefaultTypeLevelDoubleValues
    with TypeLevelFieldAccessInstructions
    with TypeLevelInvokeInstructions
    with TypeLevelDynamicLoads
    with ThrowAllPotentialExceptionsConfiguration
    with IgnoreSynchronization
    with DefaultTypeLevelHandlingOfMethodResults
    with DefaultTypeLevelReferenceValues
    with PostEvaluationMemoryManagement
    with DefaultExceptionsFactory
    with TheMethod {

    def this(project: SomeProject, method: Method) =
        this(project.classHierarchy, method)

    type AReferenceValue = ReferenceValueLike
    type DomainReferenceValue = AReferenceValue

    final val DomainReferenceValueTag: ClassTag[DomainReferenceValue] = implicitly

    type DomainNullValue = ANullValue
    type DomainObjectValue = AnObjectValue
    type DomainArrayValue = AnArrayValue

    val TheNullValue: DomainNullValue = new ANullValue()

    // -----------------------------------------------------------------------------------
    //
    // REPRESENTATION OF REFERENCE VALUES
    //
    // -----------------------------------------------------------------------------------

    protected case class InitializedObjectValue(
            override val theUpperTypeBound: ObjectType
    ) extends SObjectValueLike with Value {
        this: DomainObjectValue =>

        override def isNull: Answer = No

        // WIDENING OPERATION
        override protected def doJoin(pc: Int, other: DomainValue): Update[DomainValue] = {
            other match {
                case _: UninitializedObjectValue => MetaInformationUpdateIllegalValue
                case that                        => super.doJoin(pc, that)
            }
        }
    }

    /**
     * @param origin The origin of the `new` instruction or -1 in case of "uninitialized this".
     */
    protected case class UninitializedObjectValue(
            override val theUpperTypeBound: ObjectType,
            origin:                         ValueOrigin
    ) extends SObjectValueLike {
        this: DomainObjectValue =>

        override def isPrecise: Boolean = {
            origin != -1 /* "-1" means that we are talking about "uninitialized this" */ ||
                classHierarchy.isKnownToBeFinal(theUpperTypeBound)
        }

        // joins of an uninitialized value with null results in an illegal value
        override def isNull: Answer = No

        final override def verificationTypeInfo: VerificationTypeInfo = {
            if (origin == -1)
                UninitializedThisVariableInfo
            else
                UninitializedVariableInfo(origin)
        }

        // WIDENING OPERATION
        override protected def doJoin(pc: Int, other: DomainValue): Update[DomainValue] = {
            other match {
                case UninitializedObjectValue(`theUpperTypeBound`, `origin`) => NoUpdate
                // this value is not completely useable...
                case _ => MetaInformationUpdateIllegalValue
            }
        }

        override def abstractsOver(other: DomainValue): Boolean = {
            other match {
                case that: UninitializedObjectValue =>
                    (that.theUpperTypeBound eq this.theUpperTypeBound) && this.origin == that.origin
                case _ =>
                    false
            }
        }

        override def adapt(target: TargetDomain, origin: ValueOrigin): target.DomainValue = {
            target.NewObject(origin, theUpperTypeBound)
        }

        override def toString: String = {
            if (origin == -1)
                "UninitializedThis"
            else
                s"${theUpperTypeBound.toJava}(uninitialized;origin=$origin)"
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

    // --------------------------------------------------------------------------------------------
    //
    // FACTORY METHODS
    //
    // --------------------------------------------------------------------------------------------

    protected case class DefaultMObjectValue(
            upperTypeBound: UIDSet[ObjectType]
    ) extends MObjectValueLike {
        override def isNull: Answer = Unknown
    }

    protected case class DefaultArrayValue(
            theUpperTypeBound: ArrayType
    ) extends AnArrayValue {
        override def isNull: Answer = Unknown
    }

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
            DefaultMObjectValue(upperTypeBound)
    }

    override def ArrayValue(origin: ValueOrigin, arrayType: ArrayType): DomainArrayValue = {
        DefaultArrayValue(arrayType)
    }

}

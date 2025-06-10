/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai
package domain
package l0

import scala.reflect.ClassTag

import org.opalj.br.ArrayType
import org.opalj.br.ClassHierarchy
import org.opalj.br.Method
import org.opalj.br.MethodDescriptor
import org.opalj.br.ClassType
import org.opalj.br.ReferenceType
import org.opalj.br.UninitializedThisVariableInfo
import org.opalj.br.UninitializedVariableInfo
import org.opalj.br.VerificationTypeInfo
import org.opalj.br.analyses.SomeProject
import org.opalj.collection.immutable.UIDSet

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
        override val theUpperTypeBound: ClassType
    ) extends SObjectValueLike with Value {
        this: DomainObjectValue =>

        override def isNull: Answer = Unknown

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
        override val theUpperTypeBound: ClassType,
        origin:                         ValueOrigin
    ) extends SObjectValueLike {
        this: DomainObjectValue =>

        override def isPrecise: Boolean = {
            origin != -1 /* "-1" means that we are talking about "uninitialized this" */ ||
            classHierarchy.isKnownToBeFinal(theUpperTypeBound)
        }

        // joins of an uninitialized value with null results in an illegal value
        override def isNull: Answer = Unknown

        override final def verificationTypeInfo: VerificationTypeInfo = {
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
        declaringClass:   ClassType,
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

    protected case class DefaultSObjectValue(
        override val theUpperTypeBound: ClassType
    ) extends SObjectValueLike {
        override def isNull: Answer = Unknown
    }

    protected case class DefaultMObjectValue(
        upperTypeBound: UIDSet[ClassType]
    ) extends MObjectValueLike {
        override def isNull: Answer = Unknown
    }

    protected case class DefaultArrayValue(
        theUpperTypeBound: ArrayType
    ) extends AnArrayValue {
        override def isNull: Answer = Unknown
    }

    override def NullValue(origin: ValueOrigin): DomainNullValue = TheNullValue

    override def NewObject(pc: Int, classType: ClassType): DomainObjectValue = {
        UninitializedObjectValue(classType, pc)
    }

    override def UninitializedThis(classType: ClassType): DomainObjectValue = {
        UninitializedObjectValue(classType, -1)
    }

    override def InitializedObjectValue(pc: Int, classType: ClassType): DomainObjectValue = {
        InitializedObjectValue(classType)
    }

    override def ObjectValue(origin: ValueOrigin, classType: ClassType): DomainObjectValue = {
        InitializedObjectValue(classType)
    }

    override def ObjectValue(
        origin:         ValueOrigin,
        upperTypeBound: UIDSet[ClassType]
    ): DomainObjectValue = {
        if (upperTypeBound.isSingletonSet)
            ObjectValue(origin, upperTypeBound.head)
        else
            DefaultMObjectValue(upperTypeBound)
    }

    override def ArrayValue(origin: ValueOrigin, arrayType: ArrayType): DomainArrayValue = {
        DefaultArrayValue(arrayType)
    }

    override def isValueASubtypeOf(value: DomainValue, supertype: ReferenceType): Answer = {
        asReferenceValue(value) match {
            case _: NullValueLike => Unknown
            case otherRefValue    => otherRefValue.isValueASubtypeOf(supertype)(classHierarchy)
        }
    }

    override def refIsNull(pc: Int, value: DomainValue): Answer = {
        Unknown
    }

    override def arrayload(
        pc:       Int,
        index:    DomainValue,
        arrayref: DomainValue
    ): ArrayLoadResult = {
        // We might have - due to nonsensical casts on objects - an array load on an object that is not actually an
        // array. This does not run, but does compile (e.g. in varargs methods). We must allow it during frame table
        // generation, as the compiler also allows it.
        // see here: https://github.com/bcgit/bc-java/blob/0ea89a4388de4f18a2cd3a1801d5bdb2a954644d/util/src/main/java/org/bouncycastle/oer/OERDefinition.java#L638-L675
        if (arrayref.isArrayValue.isYes)
            asArrayAbstraction(arrayref).load(pc, index)
        else {
            var thrownExceptions: List[ExceptionValue] = Nil
            if (throwNullPointerExceptionOnArrayAccess)
                thrownExceptions = VMNullPointerException(pc) :: thrownExceptions
            if (throwArrayIndexOutOfBoundsException)
                thrownExceptions = VMArrayIndexOutOfBoundsException(pc) :: thrownExceptions
            ComputedValueOrException(ObjectValue(pc, ClassType.Object), thrownExceptions)
        }
    }

    override def arraystore(
        pc:       Int,
        value:    DomainValue,
        index:    DomainValue,
        arrayref: DomainValue
    ): ArrayStoreResult = {
        // We must support array operations on objects that are not actually arrays. See arrayload for explanation.
        if (arrayref.isArrayValue.isYes)
            asArrayAbstraction(arrayref).store(pc, value, index)
        else {
            var thrownExceptions: List[ExceptionValue] = Nil
            if (throwNullPointerExceptionOnArrayAccess)
                thrownExceptions = VMNullPointerException(pc) :: thrownExceptions
            if (throwArrayIndexOutOfBoundsException)
                thrownExceptions = VMArrayIndexOutOfBoundsException(pc) :: thrownExceptions
            ComputationWithSideEffectOrException(thrownExceptions)
        }

    }

    override def arraylength(
        pc:       Int,
        arrayref: DomainValue
    ): Computation[DomainValue, ExceptionValue] = {
        // We do not track length in this domain either way, and we do not want to error if "arrayref" is not actually
        // an array - there are instances of bytecode where people do nonsensical CHECKCASTs and then invoke ARRAYLENGTH
        // on those objects. Although this will never run successfully, Java allows us to compile this - so we should also
        // allow this when computing a stack map table using this domain.
        // See example: https://github.com/bcgit/bc-java/blob/0ea89a4388de4f18a2cd3a1801d5bdb2a954644d/util/src/main/java/org/bouncycastle/oer/OERDefinition.java#L638-L675
        ComputedValue(IntegerValue(pc))
    }

}

/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai
package domain
package l0

import scala.reflect.ClassTag

import org.opalj.br.ArrayType
import org.opalj.br.ClassHierarchy
import org.opalj.br.ClassType
import org.opalj.br.Method
import org.opalj.br.ReferenceType
import org.opalj.br.analyses.SomeProject
import org.opalj.collection.immutable.UIDSet
import org.opalj.value.IsSArrayValue

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

    // --------------------------------------------------------------------------------------------
    //
    // FACTORY METHODS
    //
    // --------------------------------------------------------------------------------------------

    protected case class DefaultSObjectValue(
        override val theUpperTypeBound: ClassType
    ) extends SObjectValueLike {
        override def isNull: Answer = Unknown

        override def doJoin(pc: ValueOrigin, other: Value): Update[Value] = {
            other match {
                case ArrayOrObjectValue => NoUpdate
                case _                  => super.doJoin(pc, other)
            }
        }
    }

    protected case class DefaultMObjectValue(
        upperTypeBound: UIDSet[ClassType]
    ) extends MObjectValueLike {
        override def isNull: Answer = Unknown

        override def doJoin(pc: ValueOrigin, other: Value): Update[Value] = {
            other match {
                case ArrayOrObjectValue => NoUpdate
                case _                  => super.doJoin(pc, other)
            }
        }
    }

    private object ArrayOrObjectValue extends DefaultSObjectValue(ClassType.Object) {
        override def doJoin(pc: ValueOrigin, other: Value): Update[Value] = {
            other match {
                case ArrayOrObjectValue | _: ANullValue                          => NoUpdate
                case _: SObjectValueLike | _: MObjectValueLike | _: AnArrayValue => StructuralUpdate(other)
            }
        }
    }

    protected case class DefaultArrayValue(
        theUpperTypeBound: ArrayType
    ) extends AnArrayValue {
        override def isNull: Answer = Unknown

        override def doJoin(pc: ValueOrigin, other: Value): Update[Value] = {
            other match {
                case ArrayOrObjectValue => NoUpdate
                case _                  => super.doJoin(pc, other)
            }
        }
    }

    override def NullValue(origin: ValueOrigin): DomainNullValue = TheNullValue

    override def NewObject(pc: Int, classType: ClassType): DomainObjectValue = {
        DefaultSObjectValue(classType)
    }

    override def UninitializedThis(classType: ClassType): DomainObjectValue = {
        DefaultSObjectValue(classType)
    }

    override def InitializedObjectValue(pc: Int, classType: ClassType): DomainObjectValue = {
        DefaultSObjectValue(classType)
    }

    override def ObjectValue(origin: ValueOrigin, classType: ClassType): DomainObjectValue = {
        DefaultSObjectValue(classType)
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

    override def arrayload(
        pc:       Int,
        index:    DomainValue,
        arrayref: DomainValue
    ): ArrayLoadResult = {
        // We might have - due to nonsensical casts on objects - an array load on an object that is not actually an
        // array. This does not run, but does compile (e.g. in varargs methods). We must allow it during frame table
        // generation, as the compiler also allows it.
        // see here: https://github.com/bcgit/bc-java/blob/0ea89a4388de4f18a2cd3a1801d5bdb2a954644d/util/src/main/java/org/bouncycastle/oer/OERDefinition.java#L638-L675

        arrayref match {
            case array: IsSArrayValue =>
                asArrayAbstraction(array).load(pc, index)
            case _ =>
                ComputedValueOrException(ArrayOrObjectValue, getArrayAccessRelatedExceptions(pc))
        }
    }

    override def arraystore(
        pc:       Int,
        value:    DomainValue,
        index:    DomainValue,
        arrayref: DomainValue
    ): ArrayStoreResult = {
        // We must support array operations on objects that are not actually arrays. See arrayload for explanation.

        arrayref match {
            case array: IsSArrayValue =>
                asArrayAbstraction(array).store(pc, value, index)
            case _ =>
                ComputationWithSideEffectOrException(getArrayAccessRelatedExceptions(pc))
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

    // It may now happen that loads are attempted on arrays that are null. We must make sure that this does not result
    // in an illegal value, but in a value that would be expected of this access. Since the NullValue does not carry a
    // type, we must infer it based on the array-load instruction used to access the array. Default will be an
    // ObjectValue, so we override all load operations for primitive-type arrays to return an actual value of the
    // primitive type. In practice, this means that
    //      laload null <index>
    // must result in a LongValue, as seen below.
    // Also, when AALoad is used to load an array from within another array (which happens to be null), we cannot detect
    // whether or not the resulting value is an Object or an Array. I did not find a way to represent isArray = maybe,
    // so for now we just accept non-array values when loading from arrays, and return the proper expected type.
    // https://github.com/openjdk/jdk/blob/e18277b470a162b9668297e8e286c812c4b0b604/src/java.desktop/share/classes/sun/print/PathGraphics.java#L681

    override def laload(pc: ValueOrigin, index: Value, arrayref: Value): ArrayLoadResult = {
        arrayref match {
            case _: IsSArrayValue => super.laload(pc, index, arrayref)
            case _                => ComputedValueOrException(LongValue(pc), getArrayAccessRelatedExceptions(pc))
        }
    }

    override def iaload(pc: ValueOrigin, index: Value, arrayref: Value): ArrayLoadResult = {
        arrayref match {
            case _: IsSArrayValue => super.iaload(pc, index, arrayref)
            case _                => ComputedValueOrException(IntegerValue(pc), getArrayAccessRelatedExceptions(pc))
        }
    }

    override def daload(pc: ValueOrigin, index: Value, arrayref: Value): ArrayLoadResult = {
        arrayref match {
            case _: IsSArrayValue => super.daload(pc, index, arrayref)
            case _                => ComputedValueOrException(DoubleValue(pc), getArrayAccessRelatedExceptions(pc))
        }
    }

    override def saload(pc: ValueOrigin, index: Value, arrayref: Value): ArrayLoadResult = {
        arrayref match {
            case _: IsSArrayValue => super.saload(pc, index, arrayref)
            case _                => ComputedValueOrException(ShortValue(pc), getArrayAccessRelatedExceptions(pc))
        }
    }

    override def faload(pc: ValueOrigin, index: Value, arrayref: Value): ArrayLoadResult = {
        arrayref match {
            case _: IsSArrayValue => super.faload(pc, index, arrayref)
            case _                => ComputedValueOrException(FloatValue(pc), getArrayAccessRelatedExceptions(pc))
        }
    }

    override def caload(pc: ValueOrigin, index: Value, arrayref: Value): ArrayLoadResult = {
        arrayref match {
            case _: IsSArrayValue => super.caload(pc, index, arrayref)
            case _                => ComputedValueOrException(CharValue(pc), getArrayAccessRelatedExceptions(pc))
        }
    }

    override def baload(pc: ValueOrigin, index: Value, arrayref: Value): ArrayLoadResult = {
        arrayref match {
            case _: IsSArrayValue => super.baload(pc, index, arrayref)
            case _                => ComputedValueOrException(ByteValue(pc), getArrayAccessRelatedExceptions(pc))
        }
    }

    private def getArrayAccessRelatedExceptions(pc: ValueOrigin): List[ExceptionValue] = {
        var thrownExceptions: List[ExceptionValue] = Nil
        if (throwNullPointerExceptionOnArrayAccess)
            thrownExceptions = VMNullPointerException(pc) :: thrownExceptions
        if (throwArrayIndexOutOfBoundsException)
            thrownExceptions = VMArrayIndexOutOfBoundsException(pc) :: thrownExceptions

        thrownExceptions
    }

}

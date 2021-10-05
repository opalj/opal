/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai

import org.opalj.value.IsNullValue
import org.opalj.value.IsPrimitiveValue
import org.opalj.value.IsReferenceValue
import org.opalj.value.IsReturnAddressValue
import org.opalj.value.ValueInformation
import org.opalj.br.BooleanType
import org.opalj.br.ByteType
import org.opalj.br.CharType
import org.opalj.br.ConstantFieldValue
import org.opalj.br.ConstantInteger
import org.opalj.br.ConstantLong
import org.opalj.br.ConstantFloat
import org.opalj.br.ConstantDouble
import org.opalj.br.ConstantString
import org.opalj.br.DoubleType
import org.opalj.br.FloatType
import org.opalj.br.IntegerType
import org.opalj.br.LongType
import org.opalj.br.ShortType

/**
 * A domain is the fundamental abstraction mechanism in OPAL that enables the customization
 * of the abstract interpretation framework towards the needs of a specific analysis.
 *
 * A domain encodes the semantics of computations (e.g., the addition of two values)
 * with respect to the domain's values (e.g., the representation of integer values).
 * Customizing a domain is the fundamental mechanism of adapting the AI framework
 * to one's needs.
 *
 * This trait defines the interface between the abstract interpretation framework
 * and some (user defined) domain. I.e., this interface defines all methods that
 * are needed by OPAL to perform an abstract interpretation.
 *
 * ==Control Flow==
 * OPAL controls the process of evaluating the code of a method, but requires a
 * domain to perform the actual computations of an instruction's result. E.g., to
 * calculate the result of adding two integer values, or to perform the comparison
 * of two object instances, or to get the result of converting a `long` value to an
 * `int` value, the framework always consults the domain.
 *
 * Handling of instructions that manipulate the stack (e.g. `dup`), that move values
 * between the stack and the locals (e.g., `Xload_Y`) or that determine the control
 * flow is, however, completely embedded into OPAL-AI.
 *
 * OPAL uses the following methods to inform a domain about the progress of the
 * abstract interpretation:
 *  - [[org.opalj.ai.CoreDomainFunctionality.afterEvaluation]]
 *  - [[org.opalj.ai.CoreDomainFunctionality.flow]]
 *  - [[org.opalj.ai.CoreDomainFunctionality.evaluationCompleted]]
 *  - [[org.opalj.ai.CoreDomainFunctionality.abstractInterpretationEnded]]
 * A domain that implements (`overrides`) one of these methods should always also delegate
 * the call to its superclass to make sure that every domain interested in these
 * events is informed.
 *
 * ==Implementing Abstract Domains==
 * While it is perfectly possible to implement a new domain by inheriting from this
 * trait, it is recommended to first study the already implemented domains and to
 * use them as a foundation.
 * To facilitate the usage of OPAL several classes/traits that implement parts of
 * this `Domain` trait are pre-defined and can be flexibly combined (mixed together)
 * when needed.
 *
 * When you extend this trait or implement parts of it you should keep as many methods/
 * fields private to facilitate mix-in composition of multiple traits.
 *
 * ==Thread Safety==
 * When every analyzed method is associated with a unique `Domain` instance and – given
 * that OPAL only uses one thread to analyze a given method at a time – no special care
 * has to be taken. However, if a domain needs to consult another domain which is, e.g,
 * associated with a project as a whole (e.g., to create a central store of values),
 * it is then the responsibility of the domain to make sure that coordination with
 * '''the world''' is thread safe.
 *
 * @note OPAL assumes that – at least conceptually – every method/code block is associated
 *      with its own instance of a domain object.
 *
 * @author Michael Eichberg
 * @author Dennis Siebert
 */
trait Domain
    extends CoreDomainFunctionality
    with IntegerValuesDomain
    with LongValuesDomain
    with FloatValuesDomain
    with DoubleValuesDomain
    with ReferenceValuesDomain
    with FieldAccessesDomain
    with MethodCallsDomain
    with MonitorInstructionsDomain
    with ReturnInstructionsDomain
    with DynamicLoadsDomain
    with PrimitiveValuesConversionsDomain
    with TypedValuesFactory
    with Configuration {

    // -----------------------------------------------------------------------------------
    //
    // FACTORY METHODS TO CREATE DOMAIN VALUES
    //
    // -----------------------------------------------------------------------------------

    /**
     * Creates the domain value that represents the constant field value.
     */
    final def ConstantFieldValue(pc: Int, cv: ConstantFieldValue[_]): DomainValue = {
        (cv.kindId: @scala.annotation.switch) match {
            case ConstantInteger.KindId => IntegerValue(pc, cv.toInt)
            case ConstantLong.KindId    => LongValue(pc, cv.toLong)
            case ConstantFloat.KindId   => FloatValue(pc, cv.toFloat)
            case ConstantDouble.KindId  => DoubleValue(pc, cv.toDouble)
            case ConstantString.KindId  => StringValue(pc, cv.toUTF8)
        }
    }

    // Here, we provide the base implementation!
    override def InitializedDomainValue(origin: ValueOrigin, vi: ValueInformation): DomainValue = {
        vi match {

            case pv @ IsPrimitiveValue(baseType) =>
                if (pv.constantValue.isDefined) {
                    baseType match {
                        case BooleanType => BooleanValue(origin, pv.asConstantBoolean)
                        case ByteType    => ByteValue(origin, pv.asConstantByte)
                        case ShortType   => ShortValue(origin, pv.asConstantShort)
                        case CharType    => CharValue(origin, pv.asConstantChar)
                        case IntegerType => IntegerValue(origin, pv.asConstantInteger)
                        case LongType    => LongValue(origin, pv.asConstantLong)
                        case FloatType   => FloatValue(origin, pv.asConstantFloat)
                        case DoubleType  => DoubleValue(origin, pv.asConstantDouble)
                    }
                } else {
                    TypedValue(origin, baseType)
                }

            case _: IsNullValue => NullValue(origin)

            case v: IsReferenceValue =>
                if (v.isNull.isNo && v.asReferenceType.isObjectType) {
                    val t = v.leastUpperType.get.asObjectType
                    if (v.isPrecise)
                        InitializedObjectValue(origin, t)
                    else
                        NonNullObjectValue(origin, t)
                } else {
                    ReferenceValue(origin, v.leastUpperType.get)
                }

            case _: IsReturnAddressValue =>
                throw new IllegalArgumentException(
                    s"$vi does not contain sufficient information to create a return address value"
                )

            case vi =>
                throw new IllegalArgumentException(s"cannot create domain value for $vi")
        }
    }
}

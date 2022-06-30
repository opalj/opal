/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai
package domain
package l1

import scala.reflect.ClassTag

import org.opalj.value.IsStringValue
import org.opalj.value.TheStringValue
import org.opalj.br.MethodDescriptor
import org.opalj.br.ObjectType
import org.opalj.br.VoidType

/**
 * Enables the tracing of concrete string values and can, e.g., be used to
 * resolve static "class.forName(...)" calls.
 *
 * @author Michael Eichberg
 */
trait StringValues
    extends ReferenceValues
    with DefaultJavaObjectToDomainValueConversion
    with MethodCallsDomain
    with PostEvaluationMemoryManagement {
    domain: CorrelationalDomainSupport with IntegerValuesDomain with TypedValuesFactory with Configuration =>

    type DomainStringValue <: StringValue with DomainObjectValue
    val DomainStringValueTag: ClassTag[DomainStringValue]

    protected trait StringValue extends SObjectValue with IsStringValue {
        this: DomainStringValue =>

        /**
         * The represented string. `value` will be `null` if and only if the [[StringValue]] is not
         * yet completely initialized!
         */
        def value: String

        override def constantValue: Option[String] = Some(value)

        override def doJoinWithNonNullValueWithSameOrigin(
            joinPC: Int,
            other:  DomainSingleOriginReferenceValue
        ): Update[DomainSingleOriginReferenceValue] = {

            other match {
                case DomainStringValueTag(that) =>
                    if (this.value == that.value) {
                        if (this.refId == that.refId)
                            NoUpdate
                        else
                            // Strings are immutable, hence, we can still keep the
                            // "reference" to the "value", but we still need to
                            // create a MetaInformationUpdate to make sure that potential
                            // future reference comparisons are reevaluated if necessary.
                            RefIdUpdate(that)
                    } else {
                        // We have to drop the concrete information...
                        // Given that the values are different we are no longer able to
                        // derive the concrete value.
                        val newRefId = nextRefId()
                        val newValue = ObjectValue(origin, No, true, ObjectType.String, newRefId)
                        StructuralUpdate(newValue)
                    }

                case _ =>
                    val result = super.doJoinWithNonNullValueWithSameOrigin(joinPC, other)
                    if (result.isStructuralUpdate) {
                        result
                    } else {
                        // This (string) value and the other value may have a corresponding
                        // abstract representation (w.r.t. the next abstraction level!)
                        // but we still need to drop the concrete information.
                        StructuralUpdate(result.value.update())
                    }
            }
        }

        override def abstractsOver(other: DomainValue): Boolean = {
            if (this eq other)
                return true;

            other match {
                case that: StringValue => that.value == this.value
                case _                 => false
            }
        }

        override def adapt(target: TargetDomain, vo: ValueOrigin): target.DomainValue = {
            // The following method is provided by `CoreDomain` and, hence,
            // all possible target domains are automatically supported.
            target.StringValue(vo, this.value)
        }

        override def toCanonicalForm: IsStringValue = TheStringValue(value)

        override def equals(other: Any): Boolean = {
            other match {
                case that: StringValue => that.value == this.value && super.equals(other)
                case _                 => false
            }
        }

        override protected def canEqual(other: SObjectValue): Boolean = {
            other.isInstanceOf[StringValue]
        }

        override def hashCode: Int = {
            super.hashCode * 41 + (if (value eq null) 0 else value.hashCode())
        }

        override def toString: String = {
            if (value eq null)
                s"""String(<initialization incomplete>)[@$origin;refId=$refId]"""
            else
                s"""String("$value")[@$origin;refId=$refId]"""
        }

    }

    object StringValue {
        def unapply(value: DomainValue): Option[String] = {
            value match {
                case s: StringValue => Some(s.value)
                case _              => None
            }
        }
    }

    abstract override def toJavaObject(pc: Int, value: DomainValue): Option[Object] = {
        value match {
            case StringValue(value) => Some(value)
            case _                  => super.toJavaObject(pc, value)
        }
    }

    abstract override def toDomainValue(pc: Int, value: Object): DomainReferenceValue = {
        value match {
            case s: String => StringValue(pc, s)
            case _         => super.toDomainValue(pc, value)
        }
    }

    abstract override def NewObject(
        origin:     ValueOrigin,
        objectType: ObjectType
    ): DomainObjectValue = {
        if (objectType eq ObjectType.String)
            StringValue(origin, null)
        else
            super.NewObject(origin, objectType)
    }

    abstract override def invokespecial(
        pc:               Int,
        declaringClass:   ObjectType,
        isInterface:      Boolean,
        name:             String,
        methodDescriptor: MethodDescriptor,
        operands:         Operands
    ): MethodCallResult = {

        // In general the compiler creates a sequence comparable to the following
        // (1) new String
        // (2) dup // duplicate the reference to directly use it after initialization
        // (3) invokespecial <init>(...)
        // (4) // do something with the string; e.g., store it in a local variable

        if ((declaringClass eq ObjectType.String) && name == "<init>") {

            val newStringKindValue = operands(methodDescriptor.parametersCount)
            if (newStringKindValue.isInstanceOf[StringValue]) {
                // we need to filter inter-constructor calls (i.e., we don't
                // want to analyze calls between the constructors of the class
                // java.lang.String)
                val newStringValue = newStringKindValue.asInstanceOf[StringValue]

                if (methodDescriptor == MethodDescriptor.NoArgsAndReturnVoid) {
                    updateAfterEvaluation(
                        newStringValue,
                        StringValue(newStringValue.origin, "", newStringValue.refId)
                    )
                    return ComputationWithSideEffectOnly;

                } else if (methodDescriptor == StringValues.ConstructorWithString) {
                    operands.head match {
                        // Let's test if we know the parameter ...
                        case StringValue(s) =>
                            updateAfterEvaluation(
                                newStringValue,
                                StringValue(newStringValue.origin, s, newStringValue.refId)
                            )
                            return ComputationWithSideEffectOnly

                        case _ => /* we can do nothing special */
                    }
                }
                // We don't know the precise value, but we still assume that the value
                // is correctly initialized.
                updateAfterEvaluation(newStringValue, newStringValue.update())
            }
        }
        super.invokespecial(pc, declaringClass, isInterface, name, methodDescriptor, operands)
    }

    final override def StringValue(origin: ValueOrigin, value: String): DomainObjectValue = {
        StringValue(origin, value, nextRefId())
    }

    def StringValue(origin: ValueOrigin, value: String, refId: RefId): DomainStringValue
}

object StringValues {

    val ConstructorWithString = MethodDescriptor(ObjectType.String, VoidType)

}

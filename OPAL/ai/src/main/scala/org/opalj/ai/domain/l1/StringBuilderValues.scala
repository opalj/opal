/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai
package domain
package l1

import java.lang.{StringBuilder => JStringBuilder}

import org.opalj.br.ObjectType

/**
 * Enables the tracing of `StringBuilders`.
 *
 * TODO ==Implementation Details==
 * ==Copy on Branch==
 * Given that StringBuilders are mutable, we have to create a copy whenever we
 * have a branch. This enables us to make the domain value
 * that represents the state of the StringBuilder independently mutable on each branch.
 * E.g.,
 * {{{
 * val sb : StringBuilder = ....
 * if (condition) sb.append("X") else sb.append("Y")
 * // here, the represented string either ends with "X" or with "Y", but not with "XY" or "YX"
 * }}}
 *
 * ==Ensure Termination==
 * To ensure termination in degenerated cases, such as:
 * {{{
 * val b : StringBuilder = ...
 * while((System.nanoTime % 33L) != 0){
 *     b.append((System.nanoTime % 33L).toString)
 * }
 * return b.toString
 * }}}
 * We count the number of joins per PC and if that value exceeds the configured threshold,
 * we completely abstract over the contents of the string builder.
 *
 * @author Michael Eichberg
 */
trait StringBuilderValues extends StringValues {
    domain: Domain with CorrelationalDomainSupport with Configuration with IntegerValuesDomain with TypedValuesFactory =>

    // TODO Move concrete class to DefaultBindingClass...
    protected class StringBuilderValue(
            val origin:      ValueOrigin,
            val builderType: ObjectType /*either StringBuilder oder StringBuffer*/ ,
            val builder:     JStringBuilder,
            val refId:       RefId
    ) extends SObjectValue {
        this: DomainStringValue =>

        assert(builder != null)
        assert((builderType eq ObjectType.StringBuffer) || (builderType eq ObjectType.StringBuilder))

        final override def isNull: No.type = No
        final override def isPrecise: Boolean = true
        final override def theUpperTypeBound: ObjectType = builderType

        override def doJoinWithNonNullValueWithSameOrigin(
            joinPC: Int,
            other:  DomainSingleOriginReferenceValue
        ): Update[DomainSingleOriginReferenceValue] = {

            other match {
                case that: StringBuilderValue =>
                    if (this.builder == that.builder && this.refId == that.refId) {
                        NoUpdate
                    } else {
                        // we have to drop the concrete information...
                        // we are no longer able to track a concrete instance
                        StructuralUpdate(this.update())
                    }
                case _ =>
                    val result = super.doJoinWithNonNullValueWithSameOrigin(joinPC, other)
                    if (result.isStructuralUpdate) {
                        result
                    } else {
                        // This value and the other value may have a corresponding
                        // abstract representation (w.r.t. the next abstraction level!)
                        // but we still need to drop the concrete information and
                        // have to update the timestamp.
                        StructuralUpdate(result.value.update())
                    }
            }
        }

        override def abstractsOver(other: DomainValue): Boolean = {
            if (this eq other)
                return true;

            other match {
                case that: StringBuilderValue =>
                    that.builder == this.builder && (this.builderType eq that.builderType)
                case _ =>
                    false
            }
        }

        override def adapt(target: TargetDomain, vo: ValueOrigin): target.DomainValue =
            target match {
                case that: StringBuilderValues =>
                    that.StringBuilderValue(
                        this.origin,
                        this.builderType,
                        this.builder
                    ).asInstanceOf[target.DomainValue]
                case _ =>
                    super.adapt(target, vo)
            }

        override def equals(other: Any): Boolean = {
            other match {
                case that: StringBuilderValue =>
                    that.builder == this.builder && (this.builderType eq that.builderType)
                case _ => false
            }
        }

        override protected def canEqual(other: SObjectValue): Boolean = {
            other.isInstanceOf[StringBuilderValue]
        }

        override def hashCode: Int = super.hashCode * 71 + value.hashCode()

        override def toString: String = {
            s"""${this.builderType.toJava}(origin=$origin;builder="$builder";refId=$refId)"""
        }

    }

    object StringBuilderValue {
        def unapply(value: StringBuilderValue): Option[String] = Some(value.builder.toString)
    }

    final def StringBuilderValue(
        origin:      ValueOrigin,
        builderType: ObjectType
    ): StringBuilderValue = {
        StringBuilderValue(origin, builderType, new JStringBuilder())
    }

    def StringBuilderValue(
        origin:      ValueOrigin,
        builderType: ObjectType,
        builder:     JStringBuilder
    ): StringBuilderValue
}

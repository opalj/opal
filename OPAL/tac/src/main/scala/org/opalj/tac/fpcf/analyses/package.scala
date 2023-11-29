/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf

import org.opalj.br.BaseType
import org.opalj.br.BooleanType
import org.opalj.br.ByteType
import org.opalj.br.CharType
import org.opalj.br.ClassHierarchy
import org.opalj.br.DoubleType
import org.opalj.br.FieldType
import org.opalj.br.FloatType
import org.opalj.br.IntegerType
import org.opalj.br.LongType
import org.opalj.br.NumericType
import org.opalj.br.ObjectType
import org.opalj.br.ObjectType.primitiveType
import org.opalj.br.ReferenceType
import org.opalj.br.ShortType
import org.opalj.value.IsNullValue
import org.opalj.value.IsPrimitiveValue
import org.opalj.value.IsReferenceValue
import org.opalj.value.ValueInformation

package object analyses {

    final def isTypeCompatible(
        formal: FieldType, actual: FieldType
    )(implicit ch: ClassHierarchy): Boolean = (formal, actual) match {
        // declared type and actual type are reference types and assignable
        case (ft: ReferenceType, at: ReferenceType) =>
            ch.isSubtypeOf(at, ft)

        // declared type and actual type are base types and either:
        case (ft: NumericType, at: BaseType) =>
            // - the same type
            (at eq ft) ||
                // - both numeric types and the declared type can represent the value
                (at.isNumericType && ft.asNumericType.isWiderThan(at.asNumericType))

        // declared type is base type, actual type might be a boxed value
        case (ft: BaseType, at: ReferenceType) =>
            ch.isSubtypeOf(at, ft.WrapperType) ||
                (ft match {
                    case BooleanType | ByteType | CharType => false
                    case nt: NumericType =>
                        Seq(ByteType, CharType, ShortType, IntegerType, LongType, FloatType, DoubleType).exists { tpe =>
                            nt.isWiderThan(tpe) && ch.isSubtypeOf(at, tpe.WrapperType)
                        }
                })

        // actual type is base type, declared type might be a boxed type
        case (ft: ObjectType, at: BaseType) =>
            primitiveType(ft) match {
                case Some(BooleanType)     => at.isBooleanType
                case Some(nt: NumericType) => at.isNumericType && nt.isWiderThan(at.asNumericType)
                case _                     => false
            }

        case _ =>
            false
    }

    final def isTypeCompatible(
        formal: FieldType, actual: ValueInformation, requireNonNullReferenceValue: Boolean = false
    )(implicit classHierarchy: ClassHierarchy): Boolean = (formal, actual) match {
        // the actual type is null and the declared type is a ref type
        case (_: ReferenceType, _: IsNullValue) =>
            // TODO here we would need the declared type information
            !requireNonNullReferenceValue
        // declared type and actual type are reference types and assignable
        case (pType: ReferenceType, v: IsReferenceValue) =>
            v.isValueASubtypeOf(pType).isNotNo

        // boolean values will be encoded as integer values in bytecode
        case (_: BooleanType, IsPrimitiveValue(v)) =>
            (v eq BooleanType) || (v eq IntegerType)

        // declared type and actual type are base types and either:
        case (pType: NumericType, IsPrimitiveValue(v)) =>
            // - the same type
            (v eq pType) ||
                // - both numeric types and the declared type can represent the value
                (v.isNumericType && pType.asNumericType.isWiderThan(v.asNumericType)) ||
                // - special cases introduced by the JVM or choice of the AI domain (e.g. everything is an integer value)
                ((pType eq ByteType) && (v eq IntegerType)) ||
                ((pType eq CharType) && (v eq ByteType)) ||
                ((pType eq CharType) && (v eq IntegerType)) ||
                ((pType eq ShortType) && (v eq ByteType)) ||
                ((pType eq ShortType) && (v eq IntegerType))

        // the actual type is null and the declared type is a base type
        case (_: BaseType, _: IsNullValue) =>
            false

        // declared type is base type, actual type might be a boxed value
        case (pType: BaseType, v: IsReferenceValue) =>
            v.asReferenceValue.isValueASubtypeOf(pType.WrapperType).isNotNo ||
                (pType match {
                    case BooleanType | ByteType | CharType => false
                    case nt: NumericType =>
                        Seq(ByteType, CharType, ShortType, IntegerType, LongType, FloatType, DoubleType).exists { tpe =>
                            nt.isWiderThan(tpe) && v.asReferenceValue.isValueASubtypeOf(tpe.WrapperType).isNotNo
                        }
                })

        // actual type is base type, declared type might be a boxed type
        case (pType: ObjectType, IsPrimitiveValue(v)) if !requireNonNullReferenceValue =>
            primitiveType(pType) match {
                case Some(BooleanType)     => v.isBooleanType
                case Some(nt: NumericType) => v.isNumericType && nt.isWiderThan(v.asNumericType)
                case _                     => false
            }

        case _ =>
            false
    }
}

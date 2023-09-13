/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses

import org.opalj.collection.immutable.IntTrieSet
import org.opalj.collection.immutable.EmptyIntTrieSet
import org.opalj.value.IsNullValue
import org.opalj.value.IsPrimitiveValue
import org.opalj.value.IsReferenceValue
import org.opalj.value.ValueInformation
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
import org.opalj.br.PCs
import org.opalj.br.ReferenceType
import org.opalj.br.ShortType
import org.opalj.ai.ValueOrigin
import org.opalj.ai.pcOfImmediateVMException
import org.opalj.ai.pcOfMethodExternalException
import org.opalj.ai.ValueOriginForImmediateVMException
import org.opalj.ai.ValueOriginForMethodExternalException
import org.opalj.ai.MethodExternalExceptionsOriginOffset
import org.opalj.ai.ImmediateVMExceptionsOriginOffset
import org.opalj.ai.isMethodExternalExceptionOrigin
import org.opalj.ai.isImmediateVMException

package object cg {

    type V = DUVar[ValueInformation]

    /**
     * A persistent representation (using pcs instead of TAC value origins) for a UVar.
     */
    final def persistentUVar(
        value: V
    )(
        implicit
        stmts: Array[Stmt[V]]
    ): Some[(ValueInformation, IntTrieSet)] = {
        Some((value.value, value.definedBy.map(pcOfDefSite _)))
    }

    final def pcOfDefSite(valueOrigin: ValueOrigin)(implicit stmts: Array[Stmt[V]]): Int = {
        if (valueOrigin >= 0)
            stmts(valueOrigin).pc
        else if (valueOrigin > ImmediateVMExceptionsOriginOffset)
            valueOrigin // <- it is a parameter!
        else if (valueOrigin > MethodExternalExceptionsOriginOffset)
            ValueOriginForImmediateVMException(stmts(pcOfImmediateVMException(valueOrigin)).pc)
        else
            ValueOriginForMethodExternalException(
                stmts(pcOfMethodExternalException(valueOrigin)).pc
            )
    }

    final def valueOriginsOfPCs(pcs: PCs, pcToIndex: Array[Int]): IntTrieSet = {
        pcs.foldLeft(EmptyIntTrieSet: IntTrieSet) { (origins, pc) =>
            if (ai.underlyingPC(pc) < 0)
                origins + pc // parameter
            else if (pc >= 0 && pcToIndex(pc) >= 0)
                origins + pcToIndex(pc) // local
            else if (isImmediateVMException(pc) && pcToIndex(pcOfImmediateVMException(pc)) >= 0)
                origins + ValueOriginForImmediateVMException(pcToIndex(pcOfImmediateVMException(pc)))
            else if (isMethodExternalExceptionOrigin(pc) && pcToIndex(pcOfMethodExternalException(pc)) >= 0)
                origins + ValueOriginForMethodExternalException(pcToIndex(pcOfMethodExternalException(pc)))
            else
                origins // as is
        }
    }

    final def uVarForDefSites(
        defSites:  (ValueInformation, IntTrieSet),
        pcToIndex: Array[Int]
    ): V = {
        UVar(defSites._1, valueOriginsOfPCs(defSites._2, pcToIndex))
    }

    final def isTypeCompatible(
        formal: FieldType, actual: ValueInformation, forCallReceiver: Boolean = false
    )(implicit classHierarchy: ClassHierarchy): Boolean = (formal, actual) match {
        // the actual type is null and the declared type is a ref type
        case (_: ReferenceType, _: IsNullValue) =>
            // TODO here we would need the declared type information
            !forCallReceiver
        // declared type and actual type are reference types and assignable
        case (pType: ReferenceType, v: IsReferenceValue) =>
            v.isValueASubtypeOf(pType).isNotNo

        case (_: BooleanType, IsPrimitiveValue(v)) =>
            v eq BooleanType

        // declared type and actual type are base types and the same type
        case (pType: NumericType, IsPrimitiveValue(v)) =>
            (v eq pType) || v.isNumericType && pType.asNumericType.isWiderThan(v.asNumericType)

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
        case (pType: ObjectType, IsPrimitiveValue(v)) if !forCallReceiver =>
            primitiveType(pType) match {
                case Some(BooleanType)     => v.isBooleanType
                case Some(nt: NumericType) => v.isNumericType && nt.isWiderThan(v.asNumericType)
                case _                     => false
            }

        case _ =>
            false
    }
}

/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ll
package llvm

import org.bytedeco.javacpp.PointerPointer
import org.bytedeco.llvm.LLVM.LLVMTypeRef
import org.bytedeco.llvm.global.LLVM.LLVMArrayTypeKind
import org.bytedeco.llvm.global.LLVM.LLVMBFloatTypeKind
import org.bytedeco.llvm.global.LLVM.LLVMCountParamTypes
import org.bytedeco.llvm.global.LLVM.LLVMCountStructElementTypes
import org.bytedeco.llvm.global.LLVM.LLVMDisposeMessage
import org.bytedeco.llvm.global.LLVM.LLVMDoubleTypeKind
import org.bytedeco.llvm.global.LLVM.LLVMFloatTypeKind
import org.bytedeco.llvm.global.LLVM.LLVMFP128TypeKind
import org.bytedeco.llvm.global.LLVM.LLVMFunctionTypeKind
import org.bytedeco.llvm.global.LLVM.LLVMGetArrayLength
import org.bytedeco.llvm.global.LLVM.LLVMGetElementType
import org.bytedeco.llvm.global.LLVM.LLVMGetParamTypes
import org.bytedeco.llvm.global.LLVM.LLVMGetReturnType
import org.bytedeco.llvm.global.LLVM.LLVMGetStructName
import org.bytedeco.llvm.global.LLVM.LLVMGetTypeKind
import org.bytedeco.llvm.global.LLVM.LLVMGetVectorSize
import org.bytedeco.llvm.global.LLVM.LLVMHalfTypeKind
import org.bytedeco.llvm.global.LLVM.LLVMIntegerTypeKind
import org.bytedeco.llvm.global.LLVM.LLVMIsFunctionVarArg
import org.bytedeco.llvm.global.LLVM.LLVMIsLiteralStruct
import org.bytedeco.llvm.global.LLVM.LLVMIsOpaqueStruct
import org.bytedeco.llvm.global.LLVM.LLVMIsPackedStruct
import org.bytedeco.llvm.global.LLVM.LLVMLabelTypeKind
import org.bytedeco.llvm.global.LLVM.LLVMMetadataTypeKind
import org.bytedeco.llvm.global.LLVM.LLVMPointerTypeKind
import org.bytedeco.llvm.global.LLVM.LLVMPPC_FP128TypeKind
import org.bytedeco.llvm.global.LLVM.LLVMPrintTypeToString
import org.bytedeco.llvm.global.LLVM.LLVMScalableVectorTypeKind
import org.bytedeco.llvm.global.LLVM.LLVMStructGetTypeAtIndex
import org.bytedeco.llvm.global.LLVM.LLVMStructTypeKind
import org.bytedeco.llvm.global.LLVM.LLVMTokenTypeKind
import org.bytedeco.llvm.global.LLVM.LLVMTypeIsSized
import org.bytedeco.llvm.global.LLVM.LLVMVectorTypeKind
import org.bytedeco.llvm.global.LLVM.LLVMVoidTypeKind
import org.bytedeco.llvm.global.LLVM.LLVMX86_FP80TypeKind
import org.bytedeco.llvm.global.LLVM.LLVMX86_MMXTypeKind

/**
 * This object returns the correct type for the given type ref on apply.
 *
 * @author Marc Clement
 */
object Type {
    def apply(ref: LLVMTypeRef): Type = {
        LLVMGetTypeKind(ref) match {
            case LLVMVoidTypeKind           => VoidType(ref)
            case LLVMHalfTypeKind           => HalfType(ref)
            case LLVMFloatTypeKind          => FloatType(ref)
            case LLVMDoubleTypeKind         => DoubleType(ref)
            case LLVMX86_FP80TypeKind       => X86_FP80Type(ref)
            case LLVMFP128TypeKind          => FP128Type(ref)
            case LLVMPPC_FP128TypeKind      => PPC_FP128Type(ref)
            case LLVMLabelTypeKind          => LabelType(ref)
            case LLVMIntegerTypeKind        => IntegerType(ref)
            case LLVMFunctionTypeKind       => FunctionType(ref)
            case LLVMStructTypeKind         => StructType(ref)
            case LLVMArrayTypeKind          => ArrayType(ref)
            case LLVMPointerTypeKind        => PointerType(ref)
            case LLVMVectorTypeKind         => VectorType(ref)
            case LLVMMetadataTypeKind       => MetadataType(ref)
            case LLVMX86_MMXTypeKind        => X86_MMXType(ref)
            case LLVMTokenTypeKind          => TokenType(ref)
            case LLVMScalableVectorTypeKind => ScalableVectorType(ref)
            case LLVMBFloatTypeKind         => FloatType(ref)
            case typeKind                   => throw new IllegalArgumentException("unknown type kind: " + typeKind)
        }
    }
}

sealed abstract class Type(ref: LLVMTypeRef) {
    def repr: String = {
        val bytePointer = LLVMPrintTypeToString(ref)
        val string = bytePointer.getString
        LLVMDisposeMessage(bytePointer)
        string
    }

    override def toString: String = s"Type(${repr})"

    def isSized: Boolean = intToBool(LLVMTypeIsSized(ref))
}

trait SequentialType {
    val ref: LLVMTypeRef

    def element: Type = Type(LLVMGetElementType(ref))
}

/** type with no size */
case class VoidType(ref: LLVMTypeRef) extends Type(ref)
/** 16 bit floating point type */
case class HalfType(ref: LLVMTypeRef) extends Type(ref)
/** 32 bit floating point type */
case class FloatType(ref: LLVMTypeRef) extends Type(ref)
/** 64 bit floating point type */
case class DoubleType(ref: LLVMTypeRef) extends Type(ref)
/** 80 bit floating point type (X87) */
case class X86_FP80Type(ref: LLVMTypeRef) extends Type(ref)
/** 128 bit floating point type (112-bit mantissa) */
case class FP128Type(ref: LLVMTypeRef) extends Type(ref)
/** 128 bit floating point type (two 64-bits) */
case class PPC_FP128Type(ref: LLVMTypeRef) extends Type(ref)
/** Labels */
case class LabelType(ref: LLVMTypeRef) extends Type(ref)
/** Arbitrary bit width integers */
case class IntegerType(ref: LLVMTypeRef) extends Type(ref)
/** Functions */
case class FunctionType(ref: LLVMTypeRef) extends Type(ref) {
    def returnType: Type = Type(LLVMGetReturnType(ref))
    def isVarArg: Boolean = intToBool(LLVMIsFunctionVarArg(ref))

    def paramCount: Int = LLVMCountParamTypes(ref)
    def params: Iterable[Type] = {
        val result = new PointerPointer[LLVMTypeRef](paramCount.toLong)
        LLVMGetParamTypes(ref, result)
        (0.toLong until paramCount.toLong).map(result.get(_)).map(p => Type(new LLVMTypeRef(p)))
    }
}
/** Structures */
case class StructType(ref: LLVMTypeRef) extends Type(ref) {
    def name: String = LLVMGetStructName(ref).getString
    private def elementCount: Int = LLVMCountStructElementTypes(ref)
    private def elementAtIndex(i: Int) = {
        assert(i < elementCount)
        Type(LLVMStructGetTypeAtIndex(ref, i))
    }
    def elements: Iterable[Type] = (0 until elementCount).map(elementAtIndex)
    def isPacked: Boolean = intToBool(LLVMIsPackedStruct(ref))
    def isOpaque: Boolean = intToBool(LLVMIsOpaqueStruct(ref))
    def isLiteral: Boolean = intToBool(LLVMIsLiteralStruct(ref))
}
/** Arrays */
case class ArrayType(ref: LLVMTypeRef) extends Type(ref) with SequentialType {
    def length: Int = LLVMGetArrayLength(ref)
}
/** Pointers */
case class PointerType(ref: LLVMTypeRef) extends Type(ref)
/** Fixed width SIMD vector type */
case class VectorType(ref: LLVMTypeRef) extends Type(ref) with SequentialType {
    def size: Int = LLVMGetVectorSize(ref)
}
/** Metadata */
case class MetadataType(ref: LLVMTypeRef) extends Type(ref)
/** X86 MMX */
case class X86_MMXType(ref: LLVMTypeRef) extends Type(ref)
/** Tokens */
case class TokenType(ref: LLVMTypeRef) extends Type(ref)
/** Scalable SIMD vector type */
case class ScalableVectorType(ref: LLVMTypeRef) extends Type(ref)
/** 16 bit brain floating point type */
case class BFloatType(ref: LLVMTypeRef) extends Type(ref)

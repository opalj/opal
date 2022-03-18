/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.ll.llvm

import org.bytedeco.llvm.LLVM.LLVMTypeRef
import org.bytedeco.llvm.global.LLVM._

object Type {
    def apply(ref: LLVMTypeRef): Type = {
        LLVMGetTypeKind(ref) match {
            case LLVMVoidTypeKind           ⇒ VoidType(ref)
            case LLVMHalfTypeKind           ⇒ HalfType(ref)
            case LLVMFloatTypeKind          ⇒ FloatType(ref)
            case LLVMDoubleTypeKind         ⇒ DoubleType(ref)
            case LLVMX86_FP80TypeKind       ⇒ X86_FP80Type(ref)
            case LLVMFP128TypeKind          ⇒ FP128Type(ref)
            case LLVMPPC_FP128TypeKind      ⇒ PPC_FP128Type(ref)
            case LLVMLabelTypeKind          ⇒ LabelType(ref)
            case LLVMIntegerTypeKind        ⇒ IntegerType(ref)
            case LLVMFunctionTypeKind       ⇒ FunctionType(ref)
            case LLVMStructTypeKind         ⇒ StructType(ref)
            case LLVMArrayTypeKind          ⇒ ArrayType(ref)
            case LLVMPointerTypeKind        ⇒ PointerType(ref)
            case LLVMVectorTypeKind         ⇒ VectorType(ref)
            case LLVMMetadataTypeKind       ⇒ MetadataType(ref)
            case LLVMX86_MMXTypeKind        ⇒ X86_MMXType(ref)
            case LLVMTokenTypeKind          ⇒ TokenType(ref)
            case LLVMScalableVectorTypeKind ⇒ ScalableVectorType(ref)
            case LLVMBFloatTypeKind         ⇒ FloatType(ref)
            case typeKind                   ⇒ throw new IllegalArgumentException("unknown type kind: "+typeKind)
        }
    }
}

sealed abstract class Type(ref: LLVMTypeRef) {
    def repr(): String = {
        val bytePointer = LLVMPrintTypeToString(ref)
        val string = bytePointer.getString
        LLVMDisposeMessage(bytePointer)
        string
    }

    override def toString: String = s"Type(${repr()})"
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
    def returnType(): Type = Type(LLVMGetReturnType(ref))
}
/** Structures */
case class StructType(ref: LLVMTypeRef) extends Type(ref)
/** Arrays */
case class ArrayType(ref: LLVMTypeRef) extends Type(ref)
/** Pointers */
case class PointerType(ref: LLVMTypeRef) extends Type(ref)
/** Fixed width SIMD vector type */
case class VectorType(ref: LLVMTypeRef) extends Type(ref)
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

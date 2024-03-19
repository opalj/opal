/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac

import org.opalj.br.ArrayType
import org.opalj.br.BaseType
import org.opalj.br.BootstrapMethod
import org.opalj.br.ComputationalType
import org.opalj.br.ComputationalTypeDouble
import org.opalj.br.ComputationalTypeFloat
import org.opalj.br.ComputationalTypeInt
import org.opalj.br.ComputationalTypeLong
import org.opalj.br.ComputationalTypeReference
import org.opalj.br.DoubleType
import org.opalj.br.Field
import org.opalj.br.FieldType
import org.opalj.br.FloatType
import org.opalj.br.IntegerType
import org.opalj.br.LongType
import org.opalj.br.Method
import org.opalj.br.MethodDescriptor
import org.opalj.br.MethodHandle
import org.opalj.br.ObjectType
import org.opalj.br.PC
import org.opalj.br.ReferenceType
import org.opalj.br.Type
import org.opalj.br.analyses.ProjectLike
import org.opalj.value.ValueInformation

/**
 * Represents an expression. In general, every expression should be a simple expression, where
 * the child expressions are just [[Var]]s or [[Const]]s.
 * However, when the code is going to be transformed to human readable code (e.g., Java oder
 * Scala), then it is possible to build up complex/nested expressions '''after''' all
 * transformations and static analyses have been performed.
 *
 * @tparam V
 */
trait Expr[+V <: Var[V]] extends ASTNode[V] {

    /**
     * The computational type of the underlying value.
     *
     * I.e., an approximation of the type of the underlying value. It is the best
     * type information directly available. The precision of the type information
     * depends on the number of pre-/post-processing steps that are done.
     */
    def cTpe: ComputationalType

    /**
     * The number of sub expression directly referenced by this expression.
     * A unary expression has one sub expression (the operand), a binary expression has
     * two and a method has as many as explicit and implicit (`this`) parameters exist.
     *
     * @note Transitive dependencies are not counted.
     */
    def subExprCount: Int

    /**
     * Returns the sub expression with the given index; if the index is wrong the
     * result is undefined!
     */
    def subExpr(index: Int): Expr[V]

    /**
     * Returns `true` if the given predicate evaluates to `true` for all direct subexpressions of
     * this expression; if the evaluation should perform a recursive decent then it needs to be
     * done by the predicate!
     */
    def forallSubExpressions[W >: V <: Var[W]](p: Expr[W] => Boolean): Boolean

    private[tac] def remapIndexes(
        pcToIndex:                    Array[Int],
        isIndexOfCaughtExceptionStmt: Int => Boolean
    ): Unit = {}

    override def toCanonicalForm(
        implicit ev: V <:< DUVar[ValueInformation]
    ): Expr[DUVar[ValueInformation]]

    // TYPE CAST (RELATED) EXPRESSIONS

    /** `true` if ''this'' expression is a [[Var]]. */
    def isValueExpression: Boolean
    def isVar: Boolean
    def asVar: V = throw new ClassCastException();
    def asInstanceOf: InstanceOf[V] = throw new ClassCastException();
    def isCompare: Boolean = false
    def asCompare: Compare[V] = throw new ClassCastException();
    def asParam: Param = throw new ClassCastException();
    def isMethodTypeConst: Boolean = false
    def asMethodTypeConst: MethodTypeConst = throw new ClassCastException();
    def isMethodHandleConst: Boolean = false
    def asMethodHandleConst: MethodHandleConst = throw new ClassCastException();
    def isConst: Boolean = false
    def isIntConst: Boolean = false
    def asIntConst: IntConst = throw new ClassCastException();
    def isLongConst: Boolean = false
    def asLongConst: LongConst = throw new ClassCastException();
    def isFloatConst: Boolean = false
    def asFloatConst: FloatConst = throw new ClassCastException();
    def isDoubleConst: Boolean = false
    def asDoubleConst: DoubleConst = throw new ClassCastException();
    def isStringConst: Boolean = false
    def asStringConst: StringConst = throw new ClassCastException();
    def isClassConst: Boolean = false
    def asClassConst: ClassConst = throw new ClassCastException();
    def isNullExpr: Boolean = false
    def asNullExpr: NullExpr = throw new ClassCastException();
    def isDynamicConst: Boolean = false
    def asDynamicConst: DynamicConst = throw new ClassCastException();
    def asBinaryExpr: BinaryExpr[V] = throw new ClassCastException();
    def asPrefixExpr: PrefixExpr[V] = throw new ClassCastException();
    def asPrimitiveTypeCastExpr: PrimitiveTypecastExpr[V] = throw new ClassCastException();
    def isNew: Boolean = false
    def asNew: New = throw new ClassCastException();
    def isNewArray: Boolean = false
    def asNewArray: NewArray[V] = throw new ClassCastException();
    def isArrayLoad: Boolean = false
    def asArrayLoad: ArrayLoad[V] = throw new ClassCastException();
    def asArrayLength: ArrayLength[V] = throw new ClassCastException();
    def isFieldRead: Boolean = false
    def asFieldRead: FieldRead[V] = throw new ClassCastException();
    def isGetField: Boolean = false
    def asGetField: GetField[V] = throw new ClassCastException();
    def isGetStatic: Boolean = false
    def asGetStatic: GetStatic = throw new ClassCastException();
    def asInvokedynamicFunctionCall: InvokedynamicFunctionCall[V] = throw new ClassCastException();
    def isFunctionCall: Boolean = false
    def asFunctionCall: FunctionCall[V] = throw new ClassCastException();
    def isStaticFunctionCall: Boolean = false
    def asStaticFunctionCall: StaticFunctionCall[V] = throw new ClassCastException();
    def asInstanceFunctionCall: InstanceFunctionCall[V] = throw new ClassCastException();
    def asNonVirtualFunctionCall: NonVirtualFunctionCall[V] = throw new ClassCastException();
    def isVirtualFunctionCall: Boolean = false;
    def asVirtualFunctionCall: VirtualFunctionCall[V] = throw new ClassCastException();
}

/**
 * An `instance of` expression as defined by the JVM specification.
 */
case class InstanceOf[+V <: Var[V]](pc: PC, value: Expr[V], cmpTpe: ReferenceType) extends Expr[V] {

    override final def asInstanceOf: this.type = this
    override final def astID: Int = InstanceOf.ASTID

    override final def cTpe: ComputationalType = ComputationalTypeInt

    override final def isSideEffectFree: Boolean = true
    override final def isValueExpression: Boolean = false
    override final def isVar: Boolean = false

    override final def subExprCount: Int = 1
    override final def subExpr(index: Int): Expr[V] = value

    override final def forallSubExpressions[W >: V <: Var[W]](p: Expr[W] => Boolean): Boolean = p(value)

    override private[tac] def remapIndexes(
        pcToIndex:                    Array[Int],
        isIndexOfCaughtExceptionStmt: Int => Boolean
    ): Unit = {
        value.remapIndexes(pcToIndex, isIndexOfCaughtExceptionStmt)
    }

    override def toCanonicalForm(
        implicit ev: V <:< DUVar[ValueInformation]
    ): Expr[DUVar[ValueInformation]] = {
        InstanceOf(pc, value.toCanonicalForm, cmpTpe)
    }

    override def toString: String = s"InstanceOf(pc=$pc,$value,${cmpTpe.toJava})"

}
object InstanceOf { final val ASTID = -2 }

/**
 * A comparison of two values.
 */
case class Compare[+V <: Var[V]](
    pc:        PC,
    left:      Expr[V],
    condition: RelationalOperator,
    right:     Expr[V]
) extends Expr[V] {
    override final def isCompare: Boolean = true
    override final def asCompare: this.type = this
    override final def astID: Int = Compare.ASTID

    override final def cTpe: ComputationalType = ComputationalTypeInt

    override final def isSideEffectFree: Boolean = true
    override final def isValueExpression: Boolean = false
    override final def isVar: Boolean = false

    override final def subExprCount: Int = 2
    override final def subExpr(index: Int): Expr[V] = if (index == 0) left else right

    override final def forallSubExpressions[W >: V <: Var[W]](p: Expr[W] => Boolean): Boolean = p(left) && p(right)

    override private[tac] def remapIndexes(
        pcToIndex:                    Array[Int],
        isIndexOfCaughtExceptionStmt: Int => Boolean
    ): Unit = {
        left.remapIndexes(pcToIndex, isIndexOfCaughtExceptionStmt)
        right.remapIndexes(pcToIndex, isIndexOfCaughtExceptionStmt)
    }

    override def toCanonicalForm(
        implicit ev: V <:< DUVar[ValueInformation]
    ): Expr[DUVar[ValueInformation]] = {
        Compare(pc, left.toCanonicalForm, condition, right.toCanonicalForm)
    }

    override def toString: String = s"Compare(pc=$pc,$left,$condition,$right)"
}
object Compare { final val ASTID = -4 }

trait ValueExpr[+V <: Var[V]] extends Expr[V] {

    override final def isValueExpression: Boolean = true

    override final def subExprCount: Int = 0
    override final def subExpr(index: Int): Expr[V] = throw new IndexOutOfBoundsException();

    override final def forallSubExpressions[W >: V <: Var[W]](p: Expr[W] => Boolean): Boolean = true
}

trait NoVariableExpr extends ValueExpr[Nothing] {

    override final def toCanonicalForm(
        implicit ev: Nothing <:< DUVar[ValueInformation]
    ): Expr[DUVar[ValueInformation]] = {
        this
    }
}

/**
 * Explicit initialization of a parameter. Parameter statements '''are only used by the naive
 * representation ([[TACNaive]])''' where it is necessary to perform an initial initialization
 * of the register values. In case of [[TACAI]], usage of parameters are implicitly encoded using
 * parameter origins (see [[DUVar]]).
 */
case class Param(cTpe: ComputationalType, name: String) extends NoVariableExpr {
    override final def asParam: this.type = this
    override final def astID: Int = Param.ASTID
    override final def isVar: Boolean = false
    override final def isSideEffectFree: Boolean = true
}

object Param { final val ASTID = -1 }

/**
 * A constant value expression.
 */
sealed abstract class Const extends NoVariableExpr {
    override final def isConst: Boolean = true
    override final def isVar: Boolean = false
    override final def isSideEffectFree: Boolean = true
    def tpe: Type
}

case class MethodTypeConst(pc: PC, value: MethodDescriptor) extends Const {
    override final def isMethodTypeConst: Boolean = true
    override final def asMethodTypeConst: this.type = this
    override final def astID: Int = MethodTypeConst.ASTID
    override final def tpe: Type = ObjectType.MethodType
    override final def cTpe: ComputationalType = ComputationalTypeReference
    override def toString: String = s"MethodTypeConst(pc=$pc,${value.toJava})"
}
object MethodTypeConst { final val ASTID = -10 }

case class MethodHandleConst(pc: PC, value: MethodHandle) extends Const {
    override final def isMethodHandleConst: Boolean = true
    override final def asMethodHandleConst: this.type = this
    override final def astID: Int = MethodHandleConst.ASTID
    override final def tpe: Type = ObjectType.MethodHandle
    override final def cTpe: ComputationalType = ComputationalTypeReference
    override def toString: String = s"MethodHandleConst(pc=$pc,${value.toJava})"
}
object MethodHandleConst { final val ASTID = -11 }

sealed abstract class SimpleValueConst extends Const

case class IntConst(pc: PC, value: Int) extends SimpleValueConst {
    override final def isIntConst: Boolean = true
    override final def asIntConst: this.type = this
    override final def astID: Int = IntConst.ASTID
    override final def tpe: Type = IntegerType
    override final def cTpe: ComputationalType = ComputationalTypeInt
    override def toString: String = s"IntConst(pc=$pc,$value)"
}
object IntConst { final val ASTID = -5 }

case class LongConst(pc: PC, value: Long) extends SimpleValueConst {
    override final def isLongConst: Boolean = true
    override final def asLongConst: this.type = this
    override final def astID: Int = LongConst.ASTID
    override final def tpe: Type = LongType
    override final def cTpe: ComputationalType = ComputationalTypeLong
    override def toString: String = s"LongConst(pc=$pc,$value)"
}
object LongConst { final val ASTID = -6 }

case class FloatConst(pc: PC, value: Float) extends SimpleValueConst {
    override final def isFloatConst: Boolean = true
    override final def asFloatConst: this.type = this
    override final def astID: Int = FloatConst.ASTID
    override final def tpe: Type = FloatType
    override final def cTpe: ComputationalType = ComputationalTypeFloat
    override def toString: String = s"FloatConst(pc=$pc,$value)"
}
object FloatConst { final val ASTID = -7 }

case class DoubleConst(pc: PC, value: Double) extends SimpleValueConst {
    override final def isDoubleConst: Boolean = true
    override final def asDoubleConst: this.type = this
    override final def astID: Int = DoubleConst.ASTID
    override final def tpe: Type = DoubleType
    override final def cTpe: ComputationalType = ComputationalTypeDouble
    override def toString: String = s"DoubleConst(pc=$pc,$value)"
}
object DoubleConst { final val ASTID = -8 }

case class StringConst(pc: PC, value: String) extends SimpleValueConst {
    override final def isStringConst: Boolean = true
    override final def asStringConst: this.type = this
    override final def astID: Int = StringConst.ASTID
    override final def tpe: Type = ObjectType.String
    override final def cTpe: ComputationalType = ComputationalTypeReference
    override def toString: String = s"StringConst(pc=$pc,$value)"
}
object StringConst { final val ASTID = -9 }

case class ClassConst(pc: PC, value: ReferenceType) extends SimpleValueConst {
    override final def isClassConst: Boolean = true
    override final def asClassConst: this.type = this
    override final def astID: Int = ClassConst.ASTID
    override final def tpe: Type = ObjectType.Class
    override final def cTpe: ComputationalType = ComputationalTypeReference
    override def toString: String = s"ClassConst(pc=$pc,${value.toJava})"
}
object ClassConst { final val ASTID = -12 }

case class DynamicConst(
    pc:              PC,
    bootstrapMethod: BootstrapMethod,
    name:            String,
    descriptor:      FieldType
) extends SimpleValueConst {
    override final def isDynamicConst: Boolean = true
    override final def asDynamicConst: this.type = this
    override final def astID: Int = DynamicConst.ASTID
    override final def tpe: Type = descriptor
    override final def cTpe: ComputationalType = descriptor.computationalType
    override def toString: String = s"DynamicConst(pc=$pc,$bootstrapMethod,$name,$descriptor)"
}
object DynamicConst { final val ASTID = -28 }

case class NullExpr(pc: PC) extends SimpleValueConst {
    override final def isNullExpr: Boolean = true
    override final def asNullExpr: this.type = this
    override final def astID: Int = NullExpr.ASTID
    override final def tpe: Type = ObjectType.Object // IMPROVE Should we introduce a fake type such as "java.null"?
    override final def cTpe: ComputationalType = ComputationalTypeReference
    override def toString: String = s"NullExpr(pc=$pc)"
}
object NullExpr { final val ASTID = -13 }

/**
 * @param cTpe  The computational type of the result of the binary expression if the expression
 *              succeeds.
 */
case class BinaryExpr[+V <: Var[V]](
    pc:    PC,
    cTpe:  ComputationalType,
    op:    BinaryArithmeticOperator,
    left:  Expr[V],
    right: Expr[V]
) extends Expr[V] {

    override final def isValueExpression: Boolean = false
    override final def isVar: Boolean = false
    override final def asBinaryExpr: this.type = this
    override final def astID: Int = BinaryExpr.ASTID

    override final def subExprCount: Int = 2
    override final def subExpr(index: Int): Expr[V] = if (index == 0) left else right

    override final def forallSubExpressions[W >: V <: Var[W]](p: Expr[W] => Boolean): Boolean = p(left) && p(right)

    override final def isSideEffectFree: Boolean = {
        // For now, we have to consider a potential "div by zero exception";
        // a better handling is only possible if we know that the value is not zero (0).
        (op != BinaryArithmeticOperators.Divide && op != BinaryArithmeticOperators.Modulo) ||
        (right.cTpe != ComputationalTypeInt && right.cTpe != ComputationalTypeLong) ||
        (right.isLongConst && right.asLongConst.value != 0) ||
        (right.isIntConst && right.asIntConst.value != 0)
    }

    override private[tac] def remapIndexes(
        pcToIndex:                    Array[Int],
        isIndexOfCaughtExceptionStmt: Int => Boolean
    ): Unit = {
        left.remapIndexes(pcToIndex, isIndexOfCaughtExceptionStmt)
        right.remapIndexes(pcToIndex, isIndexOfCaughtExceptionStmt)
    }

    override def toCanonicalForm(
        implicit ev: V <:< DUVar[ValueInformation]
    ): Expr[DUVar[ValueInformation]] = {
        BinaryExpr(pc, cTpe, op, left.toCanonicalForm, right.toCanonicalForm)
    }

    override def toString: String = s"BinaryExpr(pc=$pc,$cTpe,$left,$op,$right)"
}
object BinaryExpr { final val ASTID = -14 }

/**
 * @param cTpe The computational type of the result of the prefix expression.
 */
case class PrefixExpr[+V <: Var[V]](
    pc:      PC,
    cTpe:    ComputationalType,
    op:      UnaryArithmeticOperator,
    operand: Expr[V]
) extends Expr[V] {

    override final def isValueExpression: Boolean = false
    override final def isVar: Boolean = false
    override final def asPrefixExpr: this.type = this
    override final def astID: Int = PrefixExpr.ASTID

    override final def isSideEffectFree: Boolean = true

    override final def subExprCount: Int = 1
    override final def subExpr(index: Int): Expr[V] = operand

    override final def forallSubExpressions[W >: V <: Var[W]](p: Expr[W] => Boolean): Boolean = p(operand)

    override private[tac] def remapIndexes(
        pcToIndex:                    Array[Int],
        isIndexOfCaughtExceptionStmt: Int => Boolean
    ): Unit = {
        operand.remapIndexes(pcToIndex, isIndexOfCaughtExceptionStmt)
    }

    override def toCanonicalForm(
        implicit ev: V <:< DUVar[ValueInformation]
    ): Expr[DUVar[ValueInformation]] = {
        PrefixExpr(pc, cTpe, op, operand.toCanonicalForm)
    }
    override def toString: String = s"PrefixExpr(pc=$pc,$cTpe,$op,$operand)"
}
object PrefixExpr { final val ASTID = -15 }

case class PrimitiveTypecastExpr[+V <: Var[V]](
    pc:        PC,
    targetTpe: BaseType,
    operand:   Expr[V]
) extends Expr[V] {

    override final def isValueExpression: Boolean = false
    override final def isVar: Boolean = false
    override final def asPrimitiveTypeCastExpr: this.type = this
    override final def astID: Int = PrimitiveTypecastExpr.ASTID
    override final def cTpe: ComputationalType = targetTpe.computationalType

    override final def isSideEffectFree: Boolean = true

    override final def subExprCount: Int = 1
    override final def subExpr(index: Int): Expr[V] = operand

    override final def forallSubExpressions[W >: V <: Var[W]](p: Expr[W] => Boolean): Boolean = p(operand)

    override private[tac] def remapIndexes(
        pcToIndex:                    Array[Int],
        isIndexOfCaughtExceptionStmt: Int => Boolean
    ): Unit = {
        operand.remapIndexes(pcToIndex, isIndexOfCaughtExceptionStmt)
    }

    override def toCanonicalForm(
        implicit ev: V <:< DUVar[ValueInformation]
    ): Expr[DUVar[ValueInformation]] = {
        PrimitiveTypecastExpr(pc, targetTpe, operand.toCanonicalForm)
    }

    override def toString: String = s"PrimitiveTypecastExpr(pc=$pc,$targetTpe,$operand)"
}
object PrimitiveTypecastExpr { final val ASTID = -16 }

/**
 * Allocates memory for the (non-abstract) given object. Note, that the call of the separator
 * is done later and therefore the object is not considered to be properly initialized and –
 * therefore – no further operations other than the call of a constructor are allowed.
 */
case class New(pc: PC, tpe: ObjectType) extends Expr[Nothing] {

    override final def isValueExpression: Boolean = false
    override final def isVar: Boolean = false
    override final def isNew: Boolean = true
    override final def asNew: this.type = this
    override final def astID: Int = New.ASTID
    override final def cTpe: ComputationalType = ComputationalTypeReference

    override final def subExprCount: Int = 0
    override final def subExpr(index: Int): Nothing = throw new IndexOutOfBoundsException();

    override final def forallSubExpressions[W >: Nothing <: Var[W]](p: Expr[W] => Boolean): Boolean = true

    /**
     * Returns `false` because an `OutOfMemoryError` may be thrown.
     */
    override final def isSideEffectFree: Boolean = false

    override final def toCanonicalForm(
        implicit ev: Nothing <:< DUVar[ValueInformation]
    ): Expr[DUVar[ValueInformation]] = {
        this
    }

    override def toString: String = s"New(pc=$pc,${tpe.toJava})"
}
object New { final val ASTID = -17 }

trait ArrayExpr[+V <: Var[V]] extends Expr[V] {
    override final def isValueExpression: Boolean = false
    override final def isVar: Boolean = false
}

/**
 * @param counts Encodes the number of dimensions that are initialized and the size of the
 *               respective dimension.
 * @param tpe The type of the array. The number of dimensions is always `>= count.size`.
 */
case class NewArray[+V <: Var[V]](pc: PC, counts: Seq[Expr[V]], tpe: ArrayType) extends ArrayExpr[V] {

    override final def isNewArray: Boolean = true
    override final def asNewArray: this.type = this
    override final def astID: Int = NewArray.ASTID
    override final def cTpe: ComputationalType = ComputationalTypeReference

    override final def subExprCount: Int = counts.size
    override final def subExpr(index: Int): Expr[V] = counts(index)

    override final def forallSubExpressions[W >: V <: Var[W]](p: Expr[W] => Boolean): Boolean = {
        counts.forall(count => p(count))
    }

    /**
     * Returns `false` by default, because a `NewArray` instruction may throw
     * `NegativeIndexSizeException` (and also `OutOfMemoryError`.)
     */
    override final def isSideEffectFree: Boolean = false

    override private[tac] def remapIndexes(
        pcToIndex:                    Array[Int],
        isIndexOfCaughtExceptionStmt: Int => Boolean
    ): Unit = {
        counts.foreach { c => c.remapIndexes(pcToIndex, isIndexOfCaughtExceptionStmt) }
    }

    override def toCanonicalForm(
        implicit ev: V <:< DUVar[ValueInformation]
    ): Expr[DUVar[ValueInformation]] = {
        NewArray(pc, counts.map(_.toCanonicalForm), tpe)
    }

    override def toString: String = {
        s"NewArray(pc=$pc,${counts.mkString("[", ",", "]")},${tpe.toJava})"
    }
}
object NewArray { final val ASTID = -18 }

case class ArrayLoad[+V <: Var[V]](pc: PC, index: Expr[V], arrayRef: Expr[V]) extends ArrayExpr[V] {

    override final def isArrayLoad: Boolean = true
    override final def asArrayLoad: this.type = this
    override final def astID: Int = ArrayLoad.ASTID
    override final def cTpe: ComputationalType = ComputationalTypeReference

    override final def isSideEffectFree: Boolean = false

    override final def subExprCount: Int = 2
    override final def subExpr(index: Int): Expr[V] = if (index == 0) this.index else arrayRef

    override final def forallSubExpressions[W >: V <: Var[W]](p: Expr[W] => Boolean): Boolean = p(index) && p(arrayRef)

    override private[tac] def remapIndexes(
        pcToIndex:                    Array[Int],
        isIndexOfCaughtExceptionStmt: Int => Boolean
    ): Unit = {
        index.remapIndexes(pcToIndex, isIndexOfCaughtExceptionStmt)
        arrayRef.remapIndexes(pcToIndex, isIndexOfCaughtExceptionStmt)
    }

    override def toCanonicalForm(
        implicit ev: V <:< DUVar[ValueInformation]
    ): Expr[DUVar[ValueInformation]] = {
        ArrayLoad(pc, index.toCanonicalForm, arrayRef.toCanonicalForm)
    }

    override def toString: String = s"ArrayLoad(pc=$pc,$index,$arrayRef)"
}
object ArrayLoad { final val ASTID = -19 }

case class ArrayLength[+V <: Var[V]](pc: PC, arrayRef: Expr[V]) extends ArrayExpr[V] {

    override final def asArrayLength: this.type = this
    override final def astID: Int = ArrayLength.ASTID
    override final def cTpe: ComputationalType = ComputationalTypeInt

    override final def isSideEffectFree: Boolean = { assert(arrayRef.isVar); false /* potential NPE */ }

    override final def subExprCount: Int = 1
    override final def subExpr(index: Int): Expr[V] = arrayRef

    override final def forallSubExpressions[W >: V <: Var[W]](p: Expr[W] => Boolean): Boolean = p(arrayRef)

    override private[tac] def remapIndexes(
        pcToIndex:                    Array[Int],
        isIndexOfCaughtExceptionStmt: Int => Boolean
    ): Unit = {
        arrayRef.remapIndexes(pcToIndex, isIndexOfCaughtExceptionStmt)
    }

    override def toCanonicalForm(
        implicit ev: V <:< DUVar[ValueInformation]
    ): Expr[DUVar[ValueInformation]] = {
        ArrayLength(pc, arrayRef.toCanonicalForm)
    }

    override def toString: String = s"ArrayLength(pc=$pc,$arrayRef)"
}
object ArrayLength { final val ASTID = -20 }

abstract class FieldRead[+V <: Var[V]] extends Expr[V] {

    override final def cTpe: ComputationalType = declaredFieldType.computationalType
    override final def isValueExpression: Boolean = false
    override final def isVar: Boolean = false

    override final def isFieldRead: Boolean = true
    override final def asFieldRead: this.type = this

    def declaringClass: ObjectType
    def name: String
    def declaredFieldType: FieldType

    /**
     * Identifies the field if it can be found.
     */
    def resolveField(implicit p: ProjectLike): Option[Field] = {
        p.resolveFieldReference(declaringClass, name, declaredFieldType)
    }

}

case class GetField[+V <: Var[V]](
    pc:                PC,
    declaringClass:    ObjectType,
    name:              String,
    declaredFieldType: FieldType,
    objRef:            Expr[V]
) extends FieldRead[V] {

    override final def isGetField: Boolean = true
    override final def asGetField: this.type = this
    override final def astID: Int = GetField.ASTID

    override final def subExprCount: Int = 1
    override final def subExpr(index: Int): Expr[V] = objRef

    override final def forallSubExpressions[W >: V <: Var[W]](p: Expr[W] => Boolean): Boolean = p(objRef)

    override private[tac] def remapIndexes(
        pcToIndex:                    Array[Int],
        isIndexOfCaughtExceptionStmt: Int => Boolean
    ): Unit = {
        objRef.remapIndexes(pcToIndex, isIndexOfCaughtExceptionStmt)
    }

    final def isSideEffectFree: Boolean = {
        assert(objRef.isValueExpression)
        // IMPROVE if the access is non-null, it is side-effect free
        false
    }

    override def hashCode(): Int = {
        ((GetField.ASTID * 1171 +
            pc) * 31 +
            declaringClass.hashCode) * 31 +
            name.hashCode
    }

    override def toCanonicalForm(
        implicit ev: V <:< DUVar[ValueInformation]
    ): Expr[DUVar[ValueInformation]] = {
        GetField(pc, declaringClass, name, declaredFieldType, objRef.toCanonicalForm)
    }

    override def toString: String = {
        s"GetField(pc=$pc,${declaringClass.toJava},$name,${declaredFieldType.toJava},$objRef)"
    }
}
object GetField { final val ASTID = -21 }

case class GetStatic(
    pc:                PC,
    declaringClass:    ObjectType,
    name:              String,
    declaredFieldType: FieldType
) extends FieldRead[Nothing] {

    override final def isGetStatic: Boolean = true
    override final def asGetStatic: this.type = this
    override final def astID: Int = GetStatic.ASTID

    override final def isSideEffectFree: Boolean = true

    override final def subExprCount: Int = 0
    override final def subExpr(index: Int): Nothing = throw new IndexOutOfBoundsException();

    override final def forallSubExpressions[W >: Nothing <: Var[W]](p: Expr[W] => Boolean): Boolean = true

    override def toCanonicalForm(
        implicit ev: Nothing <:< DUVar[ValueInformation]
    ): Expr[DUVar[ValueInformation]] = {
        this
    }

    override def hashCode(): Int = {
        ((GetStatic.ASTID * 1171 +
            pc) * 31 +
            declaringClass.hashCode) * 31 +
            name.hashCode
    }

    override def toString: String = {
        s"GetStatic(pc=$pc,${declaringClass.toJava},$name,${declaredFieldType.toJava})"
    }
}
object GetStatic { final val ASTID = -22 }

/**
 * Representation of an `invokedynamic` instruction where the finally called method returns some
 * value.
 *
 * @tparam V The type of the [[Var]]s.
 */
case class InvokedynamicFunctionCall[+V <: Var[V]](
    pc:              PC,
    bootstrapMethod: BootstrapMethod,
    name:            String,
    descriptor:      MethodDescriptor,
    params:          Seq[Expr[V]]
) extends Expr[V] {

    override final def asInvokedynamicFunctionCall: this.type = this
    override final def isValueExpression: Boolean = false
    override final def isVar: Boolean = false
    override final def astID: Int = InvokedynamicFunctionCall.ASTID
    override final def cTpe: ComputationalType = descriptor.returnType.computationalType

    // IMPROVE [FUTURE] Use some analysis to determine if a method call is side effect free
    override final def isSideEffectFree: Boolean = false

    override final def subExprCount: Int = params.size
    override final def subExpr(index: Int): Expr[V] = params(index)

    override final def forallSubExpressions[W >: V <: Var[W]](p: Expr[W] => Boolean): Boolean = {
        params.forall(param => p(param))
    }

    override private[tac] def remapIndexes(
        pcToIndex:                    Array[Int],
        isIndexOfCaughtExceptionStmt: Int => Boolean
    ): Unit = {
        params.foreach { p => p.remapIndexes(pcToIndex, isIndexOfCaughtExceptionStmt) }
    }

    override def toCanonicalForm(
        implicit ev: V <:< DUVar[ValueInformation]
    ): Expr[DUVar[ValueInformation]] = {
        InvokedynamicFunctionCall(pc, bootstrapMethod, name, descriptor, params.map(_.toCanonicalForm))
    }

    override def hashCode(): Int = {
        (((InvokedynamicFunctionCall.ASTID * 1171 +
            pc) * 31 +
            bootstrapMethod.hashCode) * 31 +
            name.hashCode) * 31 +
            descriptor.hashCode
    }

    override def toString: String = {
        val sig = descriptor.toJava(name)
        val params = this.params.mkString("(", ",", ")")
        s"InvokedynamicFunctionCall(pc=$pc,$bootstrapMethod,$sig,$params)"
    }
}
object InvokedynamicFunctionCall { final val ASTID = -23 }

sealed abstract class FunctionCall[+V <: Var[V]] extends Expr[V] with Call[V] {

    override final def cTpe: ComputationalType = descriptor.returnType.computationalType
    override final def isValueExpression: Boolean = false
    override final def isVar: Boolean = false
    override final def isFunctionCall: Boolean = true
    override final def asFunctionCall: this.type = this
    override final def isStaticCall: Boolean = isStaticFunctionCall
}

sealed abstract class InstanceFunctionCall[+V <: Var[V]] extends FunctionCall[V] {

    override final def allParams: Seq[Expr[V]] = receiver +: params

    def receiver: Expr[V]
    override final def receiverOption: Some[Expr[V]] = Some(receiver)

    override final def subExprCount: Int = params.size + 1
    override final def subExpr(index: Int): Expr[V] = if (index == 0) receiver else params(index - 1)

    override final def forallSubExpressions[W >: V <: Var[W]](p: Expr[W] => Boolean): Boolean = {
        p(receiver) && params.forall(param => p(param))
    }
    override final def asInstanceFunctionCall: this.type = this
}

/**
 * An instance based method call which does not require virtual method lookup. In other
 * words the target method is either directly found in the specified class or a super
 * class thereof. (Basically corresponds to an invokespecial at bytecode level.)
 *
 * @param pc The pc of the underlying, original bytecode instruction. Primarily useful to
 *           do a lookup in the line-/local-variable tables.
 * @param declaringClass The declaring class of the target method.
 * @param isInterface `true` if the declaring class defines an interface type.
 *                   (Required since Java 8.)
 * @param name The name of the target method.
 * @param descriptor The descriptor.
 * @param receiver The receiver object.
 * @param params The parameters of the method call (including the implicit `this` reference.)
 * @tparam V The type of the Var used by this representation.
 */
case class NonVirtualFunctionCall[+V <: Var[V]](
    pc:             PC,
    declaringClass: ObjectType,
    isInterface:    Boolean,
    name:           String,
    descriptor:     MethodDescriptor,
    receiver:       Expr[V],
    params:         Seq[Expr[V]]
) extends InstanceFunctionCall[V] {

    override final def asNonVirtualFunctionCall: this.type = this
    override final def astID: Int = NonVirtualFunctionCall.ASTID
    override final def isSideEffectFree: Boolean = false

    /**
     * Identifies the potential call target if it can be found.
     *
     * @see [ProjectLike#specialCall] for further details.
     */
    def resolveCallTarget(callerClassType: ObjectType)(implicit p: ProjectLike): Result[Method] = {
        p.specialCall(callerClassType, declaringClass, isInterface, name, descriptor)
    }

    override final def resolveCallTargets(
        callingContext: ObjectType
    )(
        implicit
        p:  ProjectLike,
        ev: V <:< DUVar[ValueInformation]
    ): Set[Method] = {
        resolveCallTarget(callingContext)(p).toSet
    }

    override private[tac] def remapIndexes(
        pcToIndex:                    Array[Int],
        isIndexOfCaughtExceptionStmt: Int => Boolean
    ): Unit = {
        receiver.remapIndexes(pcToIndex, isIndexOfCaughtExceptionStmt)
        params foreach { p => p.remapIndexes(pcToIndex, isIndexOfCaughtExceptionStmt) }
    }

    override def toCanonicalForm(
        implicit ev: V <:< DUVar[ValueInformation]
    ): Expr[DUVar[ValueInformation]] = {
        NonVirtualFunctionCall(
            pc,
            declaringClass,
            isInterface,
            name,
            descriptor,
            receiver.toCanonicalForm,
            params.map(_.toCanonicalForm)
        )
    }

    override def hashCode(): Int = {
        (((NonVirtualFunctionCall.ASTID * 1171 +
            pc) * 31 +
            declaringClass.hashCode) * 31 +
            name.hashCode) * 31 +
            descriptor.hashCode
    }

    override def toString: String = {
        val sig = descriptor.toJava(name)
        val declClass = declaringClass.toJava
        val params = this.params.mkString("(", ",", ")")
        s"NonVirtualFunctionCall(pc=$pc,$declClass,isInterface=$isInterface,$sig,$receiver,$params)"
    }
}
object NonVirtualFunctionCall { final val ASTID = -24 }

case class VirtualFunctionCall[+V <: Var[V]](
    pc:             PC,
    declaringClass: ReferenceType,
    isInterface:    Boolean,
    name:           String,
    descriptor:     MethodDescriptor,
    receiver:       Expr[V],
    params:         Seq[Expr[V]]
) extends InstanceFunctionCall[V]
    with VirtualCall[V] {

    override final def isVirtualFunctionCall: Boolean = true
    override final def asVirtualFunctionCall: this.type = this
    override final def astID: Int = VirtualFunctionCall.ASTID
    override final def isSideEffectFree: Boolean = false

    override private[tac] def remapIndexes(
        pcToIndex:                    Array[Int],
        isIndexOfCaughtExceptionStmt: Int => Boolean
    ): Unit = {
        receiver.remapIndexes(pcToIndex, isIndexOfCaughtExceptionStmt)
        params.foreach { p => p.remapIndexes(pcToIndex, isIndexOfCaughtExceptionStmt) }
    }

    override def toCanonicalForm(
        implicit ev: V <:< DUVar[ValueInformation]
    ): Expr[DUVar[ValueInformation]] = {
        VirtualFunctionCall(
            pc,
            declaringClass,
            isInterface,
            name,
            descriptor,
            receiver.toCanonicalForm,
            params.map(_.toCanonicalForm)
        )
    }

    override def hashCode(): Int = {
        (((VirtualFunctionCall.ASTID * 1171 +
            pc) * 31 +
            declaringClass.hashCode) * 31 +
            name.hashCode) * 31 +
            descriptor.hashCode
    }

    override def toString: String = {
        val sig = descriptor.toJava(name)
        val declClass = declaringClass.toJava
        val params = this.params.mkString("(", ",", ")")
        s"VirtualFunctionCall(pc=$pc,$declClass,isInterface=$isInterface,$sig,$receiver,$params)"
    }
}
object VirtualFunctionCall { final val ASTID = -25 }

case class StaticFunctionCall[+V <: Var[V]](
    pc:             PC,
    declaringClass: ObjectType,
    isInterface:    Boolean,
    name:           String,
    descriptor:     MethodDescriptor,
    params:         Seq[Expr[V]]
) extends FunctionCall[V] {

    override final def allParams: Seq[Expr[V]] = params

    override final def isStaticFunctionCall: Boolean = true
    override final def asStaticFunctionCall: this.type = this
    override final def astID: Int = StaticFunctionCall.ASTID

    override final def isSideEffectFree: Boolean = false

    override final def receiverOption: Option[Expr[V]] = None

    override final def subExprCount: Int = params.size
    override final def subExpr(index: Int): Expr[V] = params(index)

    override final def forallSubExpressions[W >: V <: Var[W]](p: Expr[W] => Boolean): Boolean = {
        params.forall(param => p(param))
    }

    /**
     * Identifies the potential call target if it can be found.
     *
     * @see [ProjectLike#staticCall] for further details.
     */
    def resolveCallTarget(callingContext: ObjectType)(implicit p: ProjectLike): Result[Method] = {
        p.staticCall(callingContext, declaringClass, isInterface, name, descriptor)
    }

    override final def resolveCallTargets(
        callingContext: ObjectType
    )(
        implicit
        p:  ProjectLike,
        ev: V <:< DUVar[ValueInformation]
    ): Set[Method] = {
        resolveCallTarget(callingContext).toSet
    }

    override private[tac] def remapIndexes(
        pcToIndex:                    Array[Int],
        isIndexOfCaughtExceptionStmt: Int => Boolean
    ): Unit = {
        params.foreach { p => p.remapIndexes(pcToIndex, isIndexOfCaughtExceptionStmt) }
    }

    override def toCanonicalForm(
        implicit ev: V <:< DUVar[ValueInformation]
    ): Expr[DUVar[ValueInformation]] = {
        StaticFunctionCall(
            pc,
            declaringClass,
            isInterface,
            name,
            descriptor,
            params.map(_.toCanonicalForm)
        )
    }

    override def hashCode(): Int = {
        (((StaticFunctionCall.ASTID * 1171 +
            pc) * 31 +
            declaringClass.hashCode) * 31 +
            name.hashCode) * 31 +
            descriptor.hashCode
    }

    override def toString: String = {
        val sig = descriptor.toJava(name)
        val declClass = declaringClass.toJava
        val params = this.params.mkString("(", ",", ")")
        s"StaticFunctionCall(pc=$pc,$declClass,isInterface=$isInterface,$sig,$params)"
    }
}
object StaticFunctionCall { final val ASTID = -26 }

/**
 * Represents a variable. Depending on the concrete usage, it is possible to distinguish between
 * a use and/or definition site. Typically, `V` is directly bound by the direct subtypes of Var.
 *
 * @example
 * {{{
 *     trait MyVar extends Var[MyVar]
 * }}}
 *
 * @tparam V Specifies the type of `Var` used by the three address representation. `V` is also
 *           the self type.
 */
trait Var[+V <: Var[V]] extends ValueExpr[V] { this: V =>

    override final def isVar: Boolean = true
    override final def asVar: V = this
    override final def astID: Int = Var.ASTID

    /**
     * A ''human readable'' name of the local variable.
     */
    def name: String
}

object Var {

    final val ASTID = -27

    def unapply[V <: Var[V]](variable: Var[V]): Some[String] = Some(variable.name)

}

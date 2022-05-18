/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac

import org.opalj.br._
import org.opalj.br.analyses.ProjectLike
import org.opalj.collection.immutable.IntTrieSet
import org.opalj.collection.immutable.IntIntPair
import org.opalj.value.ValueInformation

import scala.collection.immutable.ArraySeq

/**
 * Super trait of all three-address code/quadruple statements.
 *
 * @author Michael Eichberg
 * @author Roberts Kolosovs
 */
sealed abstract class Stmt[+V <: Var[V]] extends ASTNode[V] {

    /**
     * The program counter of the original '''underyling bytecode instruction'''.
     *
     * This `pc` is independent of the (implicit) `index` of the statement
     * in the generated statements array! This pc is, e.g., useful for
     * getting line number information.
     */
    def pc: UShort

    def forallSubExpressions[W >: V <: Var[W]](p: Expr[W] => Boolean): Boolean

    /**
     * Called by the framework to enable each statement/expression to re-map the target
     * `pc` of a(n unconditional) jump instruction to the index of the respective quadruple
     * statement in the statements array.
     *
     * ==Example==
     * The bytecode instruction:  `5: goto 10` (where 5 is the original `pc` and `10` is
     * the branchoffset) is re-mapped to a `goto pcToIndex(5+10)` quadruples statement.
     */
    private[tac] def remapIndexes(
        pcToIndex:                    Array[Int],
        isIndexOfCaughtExceptionStmt: Int => Boolean
    ): Unit

    override def toCanonicalForm(
        implicit
        ev: V <:< DUVar[ValueInformation]
    ): Stmt[DUVar[ValueInformation]]

    // TYPE CONVERSION METHODS

    def asIf: If[V] = throw new ClassCastException();
    def asGoto: Goto = throw new ClassCastException();
    def asRet: Ret = throw new ClassCastException();
    def asJSR: JSR = throw new ClassCastException();
    def asSwitch: Switch[V] = throw new ClassCastException();
    def asAssignmentLike: AssignmentLikeStmt[V] = throw new ClassCastException();
    def asAssignment: Assignment[V] = throw new ClassCastException();
    def asReturnValue: ReturnValue[V] = throw new ClassCastException();
    def asReturn: Return = throw new ClassCastException();
    def asNop: Nop = throw new ClassCastException();
    def asSynchronizationStmt: SynchronizationStmt[V] = throw new ClassCastException();
    def asMonitorEnter: MonitorEnter[V] = throw new ClassCastException();
    def asMonitorExit: MonitorExit[V] = throw new ClassCastException();
    def asArrayStore: ArrayStore[V] = throw new ClassCastException();
    def asThrow: Throw[V] = throw new ClassCastException();
    def asFieldWriteAccessStmt: FieldWriteAccessStmt[V] = throw new ClassCastException();
    def asPutStatic: PutStatic[V] = throw new ClassCastException();
    def asPutField: PutField[V] = throw new ClassCastException();
    /*inner type*/ def asMethodCall: MethodCall[V] = throw new ClassCastException();
    /*inner type*/ def asInstanceMethodCall: InstanceMethodCall[V] = throw new ClassCastException();
    def asNonVirtualMethodCall: NonVirtualMethodCall[V] = throw new ClassCastException();
    def asVirtualMethodCall: VirtualMethodCall[V] = throw new ClassCastException();
    def asStaticMethodCall: StaticMethodCall[V] = throw new ClassCastException();
    def asInvokedynamicMethodCall: InvokedynamicMethodCall[V] = throw new ClassCastException();
    def asExprStmt: ExprStmt[V] = throw new ClassCastException();
    def asCaughtException: CaughtException[V] = throw new ClassCastException();
    def asCheckcast: Checkcast[V] = throw new ClassCastException();

    def isAssignment: Boolean = false
    def isExprStmt: Boolean = false
    def isNonVirtualMethodCall: Boolean = false
    def isVirtualMethodCall: Boolean = false
    def isStaticMethodCall: Boolean = false
    def isCaughtException: Boolean = false

}

/**
 * Representation of a conditional jump, where we jump to the given target if the condition
 * evaluates to true.
 *
 * @param target Index in the statements array.
 * @param left The expression left to the relational operator. In general, this can be expected to
 *        be a Var or Const.
 *        However, it is an expression to facilitate advanced use cases such as
 *        generating source code.
 * @param right The expression right to the relational operator. In general, this can be expected to
 *        be a Var or Const.
 *        However, it is an expression to facilitate advanced use cases such as
 *        generating source code.
 */
case class If[+V <: Var[V]](
        pc:                      PC,
        left:                    Expr[V],
        condition:               RelationalOperator,
        right:                   Expr[V],
        private[tac] var target: Int
) extends Stmt[V] {

    final override def asIf: this.type = this
    final override def astID: Int = If.ASTID
    final def leftExpr: Expr[V] = left
    final def rightExpr: Expr[V] = right
    final override def forallSubExpressions[W >: V <: Var[W]](p: Expr[W] => Boolean): Boolean = {
        p(left) && p(right)
    }

    /**
     * The target statement that is executed if the condition evaluates to `true`.
     */
    def targetStmt: Int = target

    private[tac] override def remapIndexes(
        pcToIndex:                    Array[Int],
        isIndexOfCaughtExceptionStmt: Int => Boolean
    ): Unit = {
        left.remapIndexes(pcToIndex, isIndexOfCaughtExceptionStmt)
        right.remapIndexes(pcToIndex, isIndexOfCaughtExceptionStmt)
        target = pcToIndex(target)
    }

    final override def toCanonicalForm(
        implicit
        ev: V <:< DUVar[ValueInformation]
    ): Stmt[DUVar[ValueInformation]] = {
        If(pc, left.toCanonicalForm, condition, right.toCanonicalForm, target)
    }

    final override def isSideEffectFree: Boolean = {
        assert(left.isValueExpression && right.isValueExpression)
        true
    }

    override def toString: String = s"If(pc=$pc,$left,$condition,$right,target=$target)"

}
object If {
    final val ASTID = 0
}

trait VariableFreeStmt extends Stmt[Nothing] {

    final override def toCanonicalForm(
        implicit
        ev: Nothing <:< DUVar[ValueInformation]
    ): Stmt[DUVar[ValueInformation]] = {
        this
    }

}

/**
 * @param target First the `pc` (absolute) of the target instruction in the
 *          original bytecode array; then the index of the respective quadruples
 *          instruction.
 */
case class Goto(pc: PC, private var target: Int) extends VariableFreeStmt {

    final override def asGoto: this.type = this
    final override def astID: Int = Goto.ASTID
    final override def forallSubExpressions[W >: Nothing <: Var[W]](p: Expr[W] => Boolean): Boolean = true

    private[tac] override def remapIndexes(
        pcToIndex:                    Array[Int],
        isIndexOfCaughtExceptionStmt: Int => Boolean
    ): Unit = {
        target = pcToIndex(target)
    }

    final override def isSideEffectFree: Boolean = true

    /**
     * @note Calling this method is only supported after the quadruples representation
     *         is created and the re-mapping of `pc`s to instruction indexes has happened!
     */
    def targetStmt: Int = target

    override def toString: String = s"Goto(pc=$pc,target=$target)"

}
object Goto {
    final val ASTID = 1
}

/**
 * Return from subroutine; only to be used in combination with JSR instructions (Java 6 and earlier).
 *
 * @param returnAddresses The set of return addresses. Based on the return addresses it is
 *                        immediately possible to determine the original JSR instruction that led
 *                        to the execution of the subroutine. It is the JSR instruction directly
 *                        preceding the instruction to which this RET instruction jumps to.
 *                        '''This information is only relevant in case of flow-sensitive
 *                        analyses.'''
 */
case class Ret(pc: PC, private var returnAddresses: PCs) extends VariableFreeStmt {

    final override def asRet: this.type = this
    final override def astID: Int = Ret.ASTID
    final override def forallSubExpressions[W >: Nothing <: Var[W]](p: Expr[W] => Boolean): Boolean = true

    private[tac] override def remapIndexes(
        pcToIndex:                    Array[Int],
        isIndexOfCaughtExceptionStmt: Int => Boolean
    ): Unit = {
        returnAddresses = returnAddresses map { pcToIndex }
    }

    final override def isSideEffectFree: Boolean = true

    override def toString: String = {
        s"Ret(pc=$pc,returnAddresses=${returnAddresses.mkString("(", ",", ")")})"
    }

}
object Ret {
    final val ASTID = 2
}

/**
 * JSR/RET instructions in the bytecode are mapped to corresponding statements where the
 * Ret instruction explicitly encodes the control flow by explicitly listing all target
 * instructions. The target instructions implicitly encode the JSR instruction which
 * called the subroutine.
 *
 * @param target At creation time the `pc` (absolute) of the target instruction in the
 *          original bytecode array; then the index of the respective quadruples
 *          instruction.
 */
case class JSR(pc: PC, private[tac] var target: Int) extends VariableFreeStmt {

    final override def asJSR: this.type = this
    final override def astID: Int = JSR.ASTID
    final override def forallSubExpressions[W >: Nothing <: Var[W]](p: Expr[W] => Boolean): Boolean = true

    private[tac] override def remapIndexes(
        pcToIndex:                    Array[Int],
        isIndexOfCaughtExceptionStmt: Int => Boolean
    ): Unit = {
        target = pcToIndex(target)
    }

    final override def isSideEffectFree: Boolean = true

    /**
     * The first statement of the called subroutine.
     *
     * @note Calling this method is only supported after the quadruples representation
     *         is created and the re-mapping of `pc`s to instruction indexes has happened!
     */
    def targetStmt: Int = target

    override def toString: String = s"JSR(pc=$pc,target=$target)"

}
object JSR {
    final val ASTID = 3
}

/**
 * Represents a table or lookup switch statement.
 *
 * @note The conversion from switch bytecode instructions may end up in simple goto statement.
 *
 * @param defaultTarget the target if none of the values matches.
 * @param npairs may be empty in that case we have a glorified goto – or if all other cases
 *        were determined to be dead.
 */
case class Switch[+V <: Var[V]](
        pc:                        PC,
        private var defaultTarget: Int,
        index:                     Expr[V],
        private var npairs:        ArraySeq[IntIntPair /*(Case Value, Jump Target)*/ ]
) extends Stmt[V] {

    final override def asSwitch: this.type = this
    final override def astID: Int = Switch.ASTID
    final override def forallSubExpressions[W >: V <: Var[W]](p: Expr[W] => Boolean): Boolean = {
        p(index)
    }

    private[tac] override def remapIndexes(
        pcToIndex:                    Array[Int],
        isIndexOfCaughtExceptionStmt: Int => Boolean
    ): Unit = {
        npairs = npairs.map { x =>
            val newIndex = pcToIndex(x._2)
            // assert(newIndex >= 0)
            x.copy(_2 = newIndex)
        }
        defaultTarget = pcToIndex(defaultTarget)
        index.remapIndexes(pcToIndex, isIndexOfCaughtExceptionStmt)
    }

    final override def toCanonicalForm(
        implicit
        ev: V <:< DUVar[ValueInformation]
    ): Stmt[DUVar[ValueInformation]] = {
        Switch(pc, defaultTarget, index.toCanonicalForm, npairs)
    }

    final override def isSideEffectFree: Boolean = {
        assert(index.isValueExpression)
        true
    }

    // Calling this method is only supported after the three-address code representation
    // is created and the remapping of pcs to instruction indexes has happened!
    def caseStmts: ArraySeq[Int] = npairs.map(x => x._2)

    // Calling this method is only supported after the quadruples representation
    // is created and the remapping of pcs to instruction indexes has happened!
    def defaultStmt: Int = defaultTarget

    override def toString: String = {
        val npairs = this.npairs.mkString("(", ",", ")")
        s"Switch(pc=$pc,defaultTarget=$defaultTarget,index=$index,npairs=$npairs"
    }
}
object Switch {
    final val ASTID = 4
}

sealed abstract class AssignmentLikeStmt[+V <: Var[V]] extends Stmt[V] {
    def expr: Expr[V]
    final override def asAssignmentLike: AssignmentLikeStmt[V] = this
}

object AssignmentLikeStmt {

    def unapply[V <: Var[V]](stmt: Stmt[V]): Option[(PC, Expr[V])] = {
        stmt match {
            case s: AssignmentLikeStmt[V] => Some((s.pc, s.expr))
            case _                        => None
        }
    }

}

case class Assignment[+V <: Var[V]](
        pc:        PC,
        targetVar: V,
        expr:      Expr[V]
) extends AssignmentLikeStmt[V] {

    final override def asAssignment: this.type = this
    final override def isAssignment: Boolean = true

    final override def astID: Int = Assignment.ASTID
    final override def forallSubExpressions[W >: V <: Var[W]](p: Expr[W] => Boolean): Boolean = {
        p(expr)
    }

    private[tac] override def remapIndexes(
        pcToIndex:                    Array[Int],
        isIndexOfCaughtExceptionStmt: Int => Boolean
    ): Unit = {
        targetVar.remapIndexes(pcToIndex, isIndexOfCaughtExceptionStmt)
        expr.remapIndexes(pcToIndex, isIndexOfCaughtExceptionStmt)
    }

    final override def toCanonicalForm(
        implicit
        ev: V <:< DUVar[ValueInformation]
    ): Stmt[DUVar[ValueInformation]] = {
        Assignment(pc, ev(targetVar).toCanonicalForm, expr.toCanonicalForm)
    }

    final override def isSideEffectFree: Boolean = expr.isSideEffectFree

    override def hashCode(): Opcode = (Assignment.ASTID * 1171 + pc) * 31 + expr.hashCode

    override def toString: String = s"Assignment(pc=$pc,$targetVar,$expr)"

}
object Assignment {
    final val ASTID = 5
}

case class ReturnValue[+V <: Var[V]](pc: Int, expr: Expr[V]) extends Stmt[V] {

    final override def asReturnValue: this.type = this
    final override def astID: Int = ReturnValue.ASTID
    final override def forallSubExpressions[W >: V <: Var[W]](p: Expr[W] => Boolean): Boolean = {
        p(expr)
    }

    private[tac] override def remapIndexes(
        pcToIndex:                    Array[Int],
        isIndexOfCaughtExceptionStmt: Int => Boolean
    ): Unit = {
        expr.remapIndexes(pcToIndex, isIndexOfCaughtExceptionStmt)
    }

    final override def toCanonicalForm(
        implicit
        ev: V <:< DUVar[ValueInformation]
    ): Stmt[DUVar[ValueInformation]] = {
        ReturnValue(pc, expr.toCanonicalForm)
    }

    final override def isSideEffectFree: Boolean = {
        // IMPROVE Check if the method does call synchronization statements; if so we may get an exception when we return from the method; otherwise the method is side-effect free
        false
    }

    override def toString: String = s"ReturnValue(pc=$pc,$expr)"
}
object ReturnValue {
    final val ASTID = 6
}

sealed abstract class SimpleStmt extends VariableFreeStmt {

    /**
     * Nothing to do.
     */
    private[tac] override def remapIndexes(
        pcToIndex:                    Array[Int],
        isIndexOfCaughtExceptionStmt: Int => Boolean
    ): Unit = {}

}

case class Return(pc: Int) extends SimpleStmt {

    final override def asReturn: this.type = this
    final override def astID: Int = Return.ASTID
    final override def forallSubExpressions[W >: Nothing <: Var[W]](p: Expr[W] => Boolean): Boolean = true

    final override def isSideEffectFree: Boolean = {
        // IMPROVE Check if the method does call synchronization statements; if so we may get an exception when we return from the method; otherwise the method is side-effect free
        false
    }

    override def toString: String = s"Return(pc=$pc)"
}
object Return {
    final val ASTID = 7
}

/**
 * Models a no-operation. In general, a NOP (no operation) can be pruned; however, in the TACAI
 * representation, if a NOP is the last statement of a basic block where the previous basic block
 * has multiple-successors then it may be the case that the NOP cannot be pruned, because it it
 * is required to keep the path alive. For example, given the following code:
 * {{{
 * public int m(int i, int j, boolean z){
 *   int r = 0;
 *   if (z) {
 *     r = i;
 *   } else {
 *     r = j;
 *   }
 *   return r;
 * }
 * }}}
 *
 * In this case, the returned value is either i or j and therefore the UVar would directly encode
 * that information and therefore the assignments would be ignored. (The information cannot be
 * recovered! If needed a complete SSA representation would be required; TACAI only provides only
 * an SSA-like representation!)
 */
case class Nop(pc: Int) extends SimpleStmt {

    final override def asNop: this.type = this
    final override def astID: Int = Nop.ASTID
    final override def forallSubExpressions[W >: Nothing <: Var[W]](p: Expr[W] => Boolean): Boolean = true

    final override def isSideEffectFree: Boolean = true

    override def toString: String = s"Nop(pc=$pc)"
}
object Nop {
    final val ASTID = 8
}

sealed abstract class SynchronizationStmt[+V <: Var[V]] extends Stmt[V] {

    final override def asSynchronizationStmt: this.type = this

    def objRef: Expr[V]

    private[tac] override def remapIndexes(
        pcToIndex:                    Array[Int],
        isIndexOfCaughtExceptionStmt: Int => Boolean
    ): Unit = {
        objRef.remapIndexes(pcToIndex, isIndexOfCaughtExceptionStmt)
    }

}

case class MonitorEnter[+V <: Var[V]](pc: PC, objRef: Expr[V]) extends SynchronizationStmt[V] {

    final override def asMonitorEnter: this.type = this
    final override def astID: Int = MonitorEnter.ASTID
    final override def forallSubExpressions[W >: V <: Var[W]](p: Expr[W] => Boolean): Boolean = {
        p(objRef)
    }

    final override def isSideEffectFree: Boolean = {
        // IMPROVE Is the lock as such ever used (do we potentially have concurrency)?
        false
    }

    final override def toCanonicalForm(
        implicit
        ev: V <:< DUVar[ValueInformation]
    ): Stmt[DUVar[ValueInformation]] = {
        MonitorEnter(pc, objRef.toCanonicalForm)
    }

    override def toString: String = s"MonitorEnter(pc=$pc,$objRef)"
}
object MonitorEnter {
    final val ASTID = 9
}

case class MonitorExit[+V <: Var[V]](pc: PC, objRef: Expr[V]) extends SynchronizationStmt[V] {

    final override def asMonitorExit: this.type = this
    final override def astID: Int = MonitorExit.ASTID
    final override def forallSubExpressions[W >: V <: Var[W]](p: Expr[W] => Boolean): Boolean = {
        p(objRef)
    }

    final override def isSideEffectFree: Boolean = {
        // IMPROVE Is the lock as such ever used (do we potentially have concurrency)?
        false
    }

    final override def toCanonicalForm(
        implicit
        ev: V <:< DUVar[ValueInformation]
    ): Stmt[DUVar[ValueInformation]] = {
        MonitorExit(pc, objRef.toCanonicalForm)
    }

    override def toString: String = s"MonitorExit(pc=$pc,$objRef)"

}
object MonitorExit {
    final val ASTID = 10
}

case class ArrayStore[+V <: Var[V]](
        pc:       PC,
        arrayRef: Expr[V],
        index:    Expr[V],
        value:    Expr[V]
) extends Stmt[V] {

    final override def asArrayStore: this.type = this
    final override def astID: Int = ArrayStore.ASTID
    final override def forallSubExpressions[W >: V <: Var[W]](p: Expr[W] => Boolean): Boolean = {
        p(arrayRef) && p(index) && p(value)
    }

    final override def isSideEffectFree: Boolean = {
        // IMPROVE Is it a redundant write?
        false
    }

    private[tac] override def remapIndexes(
        pcToIndex:                    Array[Int],
        isIndexOfCaughtExceptionStmt: Int => Boolean
    ): Unit = {
        arrayRef.remapIndexes(pcToIndex, isIndexOfCaughtExceptionStmt)
        index.remapIndexes(pcToIndex, isIndexOfCaughtExceptionStmt)
        value.remapIndexes(pcToIndex, isIndexOfCaughtExceptionStmt)
    }

    final override def toCanonicalForm(
        implicit
        ev: V <:< DUVar[ValueInformation]
    ): Stmt[DUVar[ValueInformation]] = {
        ArrayStore(pc, arrayRef.toCanonicalForm, index.toCanonicalForm, value.toCanonicalForm)
    }

    override def toString: String = s"ArrayStore(pc=$pc,$arrayRef,$index,$value)"
}
object ArrayStore {
    final val ASTID = 11
}

case class Throw[+V <: Var[V]](pc: PC, exception: Expr[V]) extends Stmt[V] {

    final override def asThrow: this.type = this
    final override def astID: Int = Throw.ASTID
    final override def forallSubExpressions[W >: V <: Var[W]](p: Expr[W] => Boolean): Boolean = {
        p(exception)
    }

    private[tac] override def remapIndexes(
        pcToIndex:                    Array[Int],
        isIndexOfCaughtExceptionStmt: Int => Boolean
    ): Unit = {
        exception.remapIndexes(pcToIndex, isIndexOfCaughtExceptionStmt)
    }

    final override def toCanonicalForm(
        implicit
        ev: V <:< DUVar[ValueInformation]
    ): Stmt[DUVar[ValueInformation]] = {
        Throw(pc, exception.toCanonicalForm)
    }

    final override def isSideEffectFree: Boolean = false

    override def hashCode(): Opcode = (Throw.ASTID * 1171 + pc) * 31 + exception.hashCode

    override def toString: String = s"Throw(pc=$pc,$exception)"
}
object Throw {
    final val ASTID = 12
}

sealed abstract class FieldWriteAccessStmt[+V <: Var[V]] extends Stmt[V] {
    def declaringClass: ObjectType
    def name: String
    def declaredFieldType: FieldType
    def value: Expr[V]

    final override def asFieldWriteAccessStmt: this.type = this

    /**
     * Identifies the field if it can be found.
     */
    def resolveField(implicit p: ProjectLike): Option[Field] = {
        p.resolveFieldReference(declaringClass, name, declaredFieldType)
    }
}

case class PutStatic[+V <: Var[V]](
        pc:                PC,
        declaringClass:    ObjectType,
        name:              String,
        declaredFieldType: FieldType,
        value:             Expr[V]
) extends FieldWriteAccessStmt[V] {

    final override def asPutStatic: this.type = this
    final override def astID: Int = PutStatic.ASTID
    final override def forallSubExpressions[W >: V <: Var[W]](p: Expr[W] => Boolean): Boolean = {
        p(value)
    }

    private[tac] override def remapIndexes(
        pcToIndex:                    Array[Int],
        isIndexOfCaughtExceptionStmt: Int => Boolean
    ): Unit = {
        value.remapIndexes(pcToIndex, isIndexOfCaughtExceptionStmt)
    }

    final override def toCanonicalForm(
        implicit
        ev: V <:< DUVar[ValueInformation]
    ): Stmt[DUVar[ValueInformation]] = {
        PutStatic(pc, declaringClass, name, declaredFieldType, value.toCanonicalForm)
    }

    final override def isSideEffectFree: Boolean = {
        // IMPROVE Is it a redundant write?
        false
    }

    override def hashCode(): Opcode = {
        ((PutStatic.ASTID * 1171 + pc) * 31 + declaringClass.hashCode) * 31 + name.hashCode
    }

    override def toString: String = {
        s"PutStatic(pc=$pc,${declaringClass.toJava},$name,${declaredFieldType.toJava},$value)"
    }
}
object PutStatic {
    final val ASTID = 13
}

case class PutField[+V <: Var[V]](
        pc:                Int,
        declaringClass:    ObjectType,
        name:              String,
        declaredFieldType: FieldType,
        objRef:            Expr[V],
        value:             Expr[V]
) extends FieldWriteAccessStmt[V] {

    final override def asPutField: this.type = this
    final override def astID: Int = PutField.ASTID
    final override def forallSubExpressions[W >: V <: Var[W]](p: Expr[W] => Boolean): Boolean = {
        p(objRef) && p(value)
    }

    private[tac] override def remapIndexes(
        pcToIndex:                    Array[Int],
        isIndexOfCaughtExceptionStmt: Int => Boolean
    ): Unit = {
        objRef.remapIndexes(pcToIndex, isIndexOfCaughtExceptionStmt)
        value.remapIndexes(pcToIndex, isIndexOfCaughtExceptionStmt)
    }

    final override def toCanonicalForm(
        implicit
        ev: V <:< DUVar[ValueInformation]
    ): Stmt[DUVar[ValueInformation]] = {
        val newObjRef = objRef.toCanonicalForm
        val newValue = value.toCanonicalForm
        PutField(pc, declaringClass, name, declaredFieldType, newObjRef, newValue)
    }

    final override def isSideEffectFree: Boolean = {
        // IMPROVE Is it a redundant write?
        false
    }

    override def hashCode(): Opcode = {
        ((PutField.ASTID * 1171 + pc) * 31 + declaringClass.hashCode) * 31 + name.hashCode
    }

    override def toString: String = {
        s"PutField(pc=$pc,${declaringClass.toJava},$name,${declaredFieldType.toJava},$objRef,$value)"
    }
}
object PutField {
    final val ASTID = 14
}

sealed abstract class MethodCall[+V <: Var[V]] extends Stmt[V] with Call[V] {

    final override def isSideEffectFree: Boolean = false // IMPROVE Check if a call has no side-effect

    final override def asMethodCall: this.type = this

}

sealed abstract class InstanceMethodCall[+V <: Var[V]] extends MethodCall[V] {

    final override def allParams: Seq[Expr[V]] = receiver +: params

    def receiver: Expr[V]
    final override def receiverOption: Some[Expr[V]] = Some(receiver)
    final override def asInstanceMethodCall: this.type = this
    final override def forallSubExpressions[W >: V <: Var[W]](p: Expr[W] => Boolean): Boolean = {
        p(receiver) && params.forall(param => p(param))
    }

    private[tac] override def remapIndexes(
        pcToIndex:                    Array[Int],
        isIndexOfCaughtExceptionStmt: Int => Boolean
    ): Unit = {
        receiver.remapIndexes(pcToIndex, isIndexOfCaughtExceptionStmt)
        params foreach { p => p.remapIndexes(pcToIndex, isIndexOfCaughtExceptionStmt) }
    }

}

object InstanceMethodCall {

    def unapply[V <: Var[V]](
        call: InstanceMethodCall[V]
    ): Some[(Int, ReferenceType, Boolean, String, MethodDescriptor, Expr[V], Seq[Expr[V]])] = {
        import call._
        Some((pc, declaringClass, isInterface, name, descriptor, receiver, params))
    }
}

/**
 * Call of an instance method for which no virtual method call resolution has to happen.
 * I.e., it is either a super-call, a private instance method call or a constructor call.
 */
case class NonVirtualMethodCall[+V <: Var[V]](
        pc:             Int,
        declaringClass: ObjectType,
        isInterface:    Boolean,
        name:           String,
        descriptor:     MethodDescriptor,
        receiver:       Expr[V],
        params:         Seq[Expr[V]]
) extends InstanceMethodCall[V] {

    final override def asNonVirtualMethodCall: this.type = this
    final override def isNonVirtualMethodCall: Boolean = true
    final override def astID: Int = NonVirtualMethodCall.ASTID

    /**
     * Identifies the potential call target if it can be found.
     *
     * @see [ProjectLike#specialCall] for further details.
     */
    def resolveCallTarget(callerClassType: ObjectType)(implicit p: ProjectLike): Result[Method] = {
        p.specialCall(callerClassType, declaringClass, isInterface, name, descriptor)
    }

    // convenience method to enable Call to define a single method to handle all kinds of calls
    def resolveCallTargets(
        callingContext: ObjectType
    )(
        implicit
        p:  ProjectLike,
        ev: V <:< DUVar[ValueInformation]
    ): Set[Method] = {
        resolveCallTarget(callingContext)(p).toSet
    }

    final override def toCanonicalForm(
        implicit
        ev: V <:< DUVar[ValueInformation]
    ): Stmt[DUVar[ValueInformation]] = {
        NonVirtualMethodCall(
            pc,
            declaringClass,
            isInterface,
            name,
            descriptor,
            receiver.toCanonicalForm,
            params.map(_.toCanonicalForm)
        )
    }

    override def toString: String = {
        val sig = descriptor.toJava(name)
        val declClass = declaringClass.toJava
        val params = this.params.mkString("(", ",", ")")
        s"NonVirtualMethodCall(pc=$pc,$declClass,isInterface=$isInterface,$sig,$receiver,$params)"
    }
}
object NonVirtualMethodCall {
    final val ASTID = 15
}

case class VirtualMethodCall[+V <: Var[V]](
        pc:             Int,
        declaringClass: ReferenceType,
        isInterface:    Boolean,
        name:           String,
        descriptor:     MethodDescriptor,
        receiver:       Expr[V],
        params:         Seq[Expr[V]]
) extends InstanceMethodCall[V]
    with VirtualCall[V] {

    final override def asVirtualMethodCall: this.type = this
    final override def isVirtualMethodCall: Boolean = true
    final override def astID: Int = VirtualMethodCall.ASTID

    final override def toCanonicalForm(
        implicit
        ev: V <:< DUVar[ValueInformation]
    ): Stmt[DUVar[ValueInformation]] = {
        VirtualMethodCall(
            pc,
            declaringClass,
            isInterface,
            name,
            descriptor,
            receiver.toCanonicalForm,
            params.map(_.toCanonicalForm)
        )
    }

    override def toString: String = {
        val sig = descriptor.toJava(name)
        val declClass = declaringClass.toJava
        val params = this.params.mkString("(", ",", ")")
        s"VirtualMethodCall(pc=$pc,$declClass,isInterface=$isInterface,$sig,$receiver,$params)"
    }
}
object VirtualMethodCall {
    final val ASTID = 16
}

case class StaticMethodCall[+V <: Var[V]](
        pc:             Int,
        declaringClass: ObjectType,
        isInterface:    Boolean,
        name:           String,
        descriptor:     MethodDescriptor,
        params:         Seq[Expr[V]]
) extends MethodCall[V] {

    final override def allParams: Seq[Expr[V]] = params

    final override def asStaticMethodCall: this.type = this
    final override def isStaticMethodCall: Boolean = true
    final override def astID: Int = StaticMethodCall.ASTID
    final override def receiverOption: Option[Expr[V]] = None
    final override def forallSubExpressions[W >: V <: Var[W]](p: Expr[W] => Boolean): Boolean = {
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

    // convenience method to enable Call to define a single method to handle all kinds of calls
    def resolveCallTargets(
        callingContext: ObjectType
    )(
        implicit
        p:  ProjectLike,
        ev: V <:< DUVar[ValueInformation]
    ): Set[Method] = {
        resolveCallTarget(callingContext).toSet
    }

    private[tac] override def remapIndexes(
        pcToIndex:                    Array[Int],
        isIndexOfCaughtExceptionStmt: Int => Boolean
    ): Unit = {
        params.foreach { p => p.remapIndexes(pcToIndex, isIndexOfCaughtExceptionStmt) }
    }

    final override def toCanonicalForm(
        implicit
        ev: V <:< DUVar[ValueInformation]
    ): Stmt[DUVar[ValueInformation]] = {
        StaticMethodCall(
            pc,
            declaringClass,
            isInterface,
            name,
            descriptor,
            params.map(_.toCanonicalForm)
        )
    }

    override def toString: String = {
        val sig = descriptor.toJava(name)
        val declClass = declaringClass.toJava
        val params = this.params.mkString("(", ",", ")")
        s"StaticMethodCall(pc=$pc,$declClass,isInterface=$isInterface,$sig,$params)"
    }
}
object StaticMethodCall {
    final val ASTID = 17
}

/**
 * Representation of an `invokedynamic` instruction where the return type of the finally called
 * method is `void`.
 *
 * @tparam V The type of the [[Var]]s.
 */
case class InvokedynamicMethodCall[+V <: Var[V]](
        pc:              PC,
        bootstrapMethod: BootstrapMethod,
        name:            String,
        descriptor:      MethodDescriptor,
        params:          Seq[Expr[V]]
) extends Stmt[V] {

    final override def astID: Int = InvokedynamicMethodCall.ASTID
    final override def asInvokedynamicMethodCall: this.type = this
    // IMPROVE [FUTURE] Use some analysis to determine if a method call is side effect free
    final override def isSideEffectFree: Boolean = false
    final override def forallSubExpressions[W >: V <: Var[W]](p: Expr[W] => Boolean): Boolean = {
        params.forall(param => p(param))
    }

    private[tac] override def remapIndexes(
        pcToIndex:                    Array[Int],
        isIndexOfCaughtExceptionStmt: Int => Boolean
    ): Unit = {
        params.foreach { p => p.remapIndexes(pcToIndex, isIndexOfCaughtExceptionStmt) }
    }

    override def hashCode(): Int = {
        (((InvokedynamicMethodCall.ASTID * 1171 +
            pc) * 31 +
            bootstrapMethod.hashCode) * 31 +
            name.hashCode) * 31 +
            descriptor.hashCode
    }

    final override def toCanonicalForm(
        implicit
        ev: V <:< DUVar[ValueInformation]
    ): Stmt[DUVar[ValueInformation]] = {
        InvokedynamicMethodCall(
            pc,
            bootstrapMethod,
            name,
            descriptor,
            params.map(_.toCanonicalForm)
        )
    }

    override def toString: String = {
        val sig = descriptor.toJava(name)
        val params = this.params.mkString("(", ",", ")")
        s"InvokedynamicMethodCall(pc=$pc,$bootstrapMethod,$sig,$params)"
    }
}
object InvokedynamicMethodCall { final val ASTID = 18 }

/** An expression where the value is not further used. */
case class ExprStmt[+V <: Var[V]](pc: Int, expr: Expr[V]) extends AssignmentLikeStmt[V] {

    final override def asExprStmt: this.type = this
    final override def isExprStmt: Boolean = true
    final override def astID: Int = ExprStmt.ASTID
    final override def forallSubExpressions[W >: V <: Var[W]](p: Expr[W] => Boolean): Boolean = {
        p(expr)
    }

    private[tac] override def remapIndexes(
        pcToIndex:                    Array[Int],
        isIndexOfCaughtExceptionStmt: Int => Boolean
    ): Unit = {
        expr.remapIndexes(pcToIndex, isIndexOfCaughtExceptionStmt)
    }

    final override def isSideEffectFree: Boolean = {
        assert(
            !expr.isSideEffectFree,
            "useless ExprStmt - the referenced expression is side-effect free"
        )
        false
    }

    override def hashCode(): Opcode = (ExprStmt.ASTID * 1171 + pc) * 31 + expr.hashCode

    final override def toCanonicalForm(
        implicit
        ev: V <:< DUVar[ValueInformation]
    ): Stmt[DUVar[ValueInformation]] = {
        ExprStmt(pc, expr.toCanonicalForm)
    }

    override def toString: String = s"ExprStmt(pc=$pc,$expr)"

}
object ExprStmt {
    final val ASTID = 19
}

/**
 * Matches a statement which – in flat form – directly calls a virtual function; basically
 * abstracts over [[ExprStmt]]s and [[Assignment]]s.
 */
object VirtualFunctionCallStatement {

    def unapply[V <: Var[V]](stmt: Stmt[V]): Option[VirtualFunctionCall[V]] = {
        stmt match {
            case ExprStmt(_, vfc: VirtualFunctionCall[V])      => Some(vfc)
            case Assignment(_, _, vfc: VirtualFunctionCall[V]) => Some(vfc)
            case _                                             => None
        }
    }
}

/**
 * Matches a statement which – in flat form – directly calls a non-virtual function; basically
 * abstracts over [[ExprStmt]]s and [[Assignment]]s.
 */
object NonVirtualFunctionCallStatement {

    def unapply[V <: Var[V]](stmt: Stmt[V]): Option[NonVirtualFunctionCall[V]] = {
        stmt match {
            case ExprStmt(_, vfc: NonVirtualFunctionCall[V])      => Some(vfc)
            case Assignment(_, _, vfc: NonVirtualFunctionCall[V]) => Some(vfc)
            case _                                                => None
        }
    }
}

/**
 * Matches a statement which – in flat form – directly calls a static function; basically
 * abstracts over [[ExprStmt]]s and [[Assignment]]s.
 */
object StaticFunctionCallStatement {

    def unapply[V <: Var[V]](stmt: Stmt[V]): Option[StaticFunctionCall[V]] = {
        stmt match {
            case ExprStmt(_, vfc: StaticFunctionCall[V])      => Some(vfc)
            case Assignment(_, _, vfc: StaticFunctionCall[V]) => Some(vfc)
            case _                                            => None
        }
    }
}

/**
 * A caught exception is essential to ensure that the "throw" is never optimized away, even if
 * the exception object as such is not used.
 *
 * @note `CaughtException` expression are only created by [[TACAI]]!
 */
case class CaughtException[+V <: Var[V]](
        pc:                        PC,
        exceptionType:             Option[ObjectType],
        private var throwingStmts: IntTrieSet
) extends Stmt[V] {

    final override def isCaughtException: Boolean = true
    final override def asCaughtException: CaughtException[V] = this
    final override def astID: Int = CaughtException.ASTID
    final override def forallSubExpressions[W >: V <: Var[W]](p: Expr[W] => Boolean): Boolean = true

    final override def isSideEffectFree: Boolean = false

    private[tac] override def remapIndexes(
        pcToIndex:                    Array[Int],
        isIndexOfCaughtExceptionStmt: Int => Boolean
    ): Unit = {
        throwingStmts = throwingStmts map { pc => ai.remapPC(pcToIndex)(pc) }
    }

    final override def toCanonicalForm(
        implicit
        ev: V <:< DUVar[ValueInformation]
    ): Stmt[DUVar[ValueInformation]] = {
        this.asInstanceOf[Stmt[DUVar[ValueInformation]]]
    }

    /**
     * The origin(s) of the caught exception(s). An origin identifies the instruction
     * that ex- or implicitly created the exception:
     *  - If the exception is created locally (`new XXXException`) and also caught within the
     *    same method, then the origin identifies a normal variable definition site.
     *  - If the exception is a parameter the parameter's origin (-1,... -n) is returned.
     *  - If the exception was raised due to a sideeffect of evaluating an expression, then the
     *    origin is smaller or equal to [[org.opalj.ai.ImmediateVMExceptionsOriginOffset]] and can be
     *    tranformed to the index of the responsible instruction using
     *    [[org.opalj.ai#pcOfImmediateVMException]].
     */
    def origins: IntTrieSet = throwingStmts

    /**
     * Textual description of the sources of the caught exceptions. If the exception was
     * thrown by the JVM due to the evaluation of an expression (e.g., NullPointerException,
     * DivisionByZero,..) then the string will be `exception@<INDEX>` where index identifies
     * the failing expression. In case an exception is caught that was thrown using `ATHROW`
     * the local variable/parameter which stores the local variable is returned.
     */
    final def exceptionLocations: Iterator[String] = {
        throwingStmts.iterator.map { defSite =>
            if (defSite < 0) {
                if (ai.isImmediateVMException(defSite))
                    "exception[VM]@"+ai.pcOfImmediateVMException(defSite)
                else if (ai.isMethodExternalExceptionOrigin(defSite))
                    "exception@"+ai.pcOfMethodExternalException(defSite)
                else
                    "param"+(-defSite - 1).toHexString
            } else {
                "lv"+defSite.toHexString
            }
        }
    }

    override def toString: String = {
        val exceptionType = this.exceptionType.map(_.toJava).getOrElse("<ANY>")
        val exceptionLocations = this.exceptionLocations.mkString("{", ",", "}")
        s"CaughtException(pc=$pc,$exceptionType,caused by=$exceptionLocations)"
    }
}

object CaughtException {

    final val ASTID = 20

}

/**
 * A `checkcast` as defined by the JVM specification.
 */
case class Checkcast[+V <: Var[V]](pc: PC, value: Expr[V], cmpTpe: ReferenceType) extends Stmt[V] {

    final override def asCheckcast: this.type = this
    final override def astID: Int = Checkcast.ASTID
    final override def forallSubExpressions[W >: V <: Var[W]](p: Expr[W] => Boolean): Boolean = {
        p(value)
    }

    private[tac] override def remapIndexes(
        pcToIndex:                    Array[Int],
        isIndexOfCaughtExceptionStmt: Int => Boolean
    ): Unit = {
        value.remapIndexes(pcToIndex, isIndexOfCaughtExceptionStmt)
    }

    final override def toCanonicalForm(
        implicit
        ev: V <:< DUVar[ValueInformation]
    ): Stmt[DUVar[ValueInformation]] = {
        Checkcast(pc, value.toCanonicalForm, cmpTpe)
    }

    final override def isSideEffectFree: Boolean = {
        // IMPROVE identify (from the JVM verifiers point-of-view) truly useless checkcasts
        // A useless checkcast is one where the static intra-procedural type information which
        // is available in the bytecode is sufficient to determine that the type is a subtype
        // of the tested type (i.e., only those check casts are truly usefull that would not
        // lead to a failing validation of the bytecode by the JVM!)
        false
    }

    override def toString: String = s"Checkcast(pc=$pc,$value,${cmpTpe.toJava})"

}
object Checkcast {
    final val ASTID = 21
}

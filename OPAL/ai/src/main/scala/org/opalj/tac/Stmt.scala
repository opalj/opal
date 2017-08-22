/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2017
 * Software Technology Group
 * Department of Computer Science
 * Technische Universität Darmstadt
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  - Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package org.opalj
package tac

import org.opalj.br._

/**
 * Super trait of all quadruple statements.
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

    /**
     * Called by the framework to enable each statement/expression to re-map the target
     * `pc` of a(n unconditional) jump instruction to the index of the respective quadruple
     * statement in the statements array.
     *
     * ==Example==
     * The bytecode instruction:  `5: goto 10` (where 5 is the original `pc` and `10` is
     * the branchoffset) is re-mapped to a `goto pcToIndex(5+10)` quadruples statement.
     */
    private[tac] def remapIndexes(pcToIndex: Array[Int]): Unit

    // TYPE CONVERSION METHODS

    def asIf : If[V] = throw new ClassCastException();
    def asGoto : Goto = throw new ClassCastException();
    def asRet : Ret= throw new ClassCastException();
    def asJSR : JSR = throw new ClassCastException();
    def asSwitch: Switch[V] = throw new ClassCastException();
    def asAssignment : Assignment[V]= throw new ClassCastException();
    def asReturnValue : Return[V]= throw new ClassCastException();
    def asReturn : Return= throw new ClassCastException();
    def asNop : Nop= throw new ClassCastException();
    def asMonitorEnter:MonitorEnter[V]= throw new ClassCastException();
    def asMonitorExit:MonitorExit[V]= throw new ClassCastException();
    def asArrayStore:ArrayStore[V]= throw new ClassCastException();
    def asThrow : Throw[V]= throw new ClassCastException();
    def asPutStatic : PutStatic[V]= throw new ClassCastException();
    def asPutField : PutField[V]= throw new ClassCastException();
    def asNonVirtualMethodCall : NonVirtualMethodCall[V]= throw new ClassCastException();
    def asVirtualMethodCall : VirtualMethodCall[V]= throw new ClassCastException();
    def asStaticMethodCall : StaticMethodCall[V]= throw new ClassCastException();
    def asExprStmt : ExprStmt[V]= throw new ClassCastException();
    def asCheckcast : Checkcast[V]= throw new ClassCastException();

}

/**
 * @param target Index in the statements array.
 * @param left The expression left to the relational operator. In general, this can be expected to
 *             be a Var. However, it is not expression to facilitate advanced use cases such as
 *             generating source code.
 * @param right The expression right to the relational operator. In general, this can be expected to
 *             be a Var. However, it is not expression to facilitate advanced use cases such as
 *             generating source code.
 *
 */
case class If[+V <: Var[V]](
        pc:                      PC,
        left:                    Expr[V],
        condition:               RelationalOperator,
        right:                   Expr[V],
        private[tac] var target: Int
) extends Stmt[V] {

final override def asIf : this.type = this

    final def astID = If.ASTID

    def leftExpr: Expr[V] = left

    def rightExpr: Expr[V] = right

    /**
     * The target statement that is executed if the condition evaluates to `true`.
     */
    def targetStmt: Int = target

    private[tac] def remapIndexes(pcToIndex: Array[Int]): Unit = {
        left.remapIndexes(pcToIndex)
        right.remapIndexes(pcToIndex)
        target = pcToIndex(target)
    }

    override def toString: String = s"If(pc=$pc,$left,$condition,$right,target=$target)"

}
object If {
    final val ASTID = 0
}

/**
 * @param target First the `pc` (absolute) of the target instruction in the
 *          original bytecode array; then the index of the respective quadruples
 *          instruction.
 */
case class Goto(pc: PC, private var target: Int) extends Stmt[Nothing] {

final override def asGoto : this.type = this

    final def astID = Goto.ASTID

    private[tac] def remapIndexes(pcToIndex: Array[Int]): Unit = target = pcToIndex(target)

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
 * Return from subroutine; only to be used in combination with JSR instructions.
 *
 * @param returnAddresses The set of return addresses. Based on the return addresses it is
 *                        immediately possible to determine the original JSR instruction that led
 *                        to the execution of the subroutine. It is the JSR instruction directly
 *                        preceding the instruction to which this RET instruction jumps to.
 *                        '''This information is only relevant in case of flow-sensitive
 *                        analyses.'''
 */
case class Ret(pc: PC, private var returnAddresses: PCs) extends Stmt[Nothing] {

final override def asRet : this.type = this

    final def astID = Ret.ASTID

    private[tac] def remapIndexes(pcToIndex: Array[Int]): Unit = {
        returnAddresses = returnAddresses map { pcToIndex }
    }

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
case class JSR(pc: PC, private[tac] var target: Int) extends Stmt[Nothing] {

final override def asJSR : this.type = this

    final def astID = JSR.ASTID

    private[tac] def remapIndexes(pcToIndex: Array[Int]): Unit = {
        target = pcToIndex(target)
    }

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

case class Switch[+V <: Var[V]](
        pc:                        PC,
        private var defaultTarget: PC,
        index:                     Expr[V],
        private var npairs:        IndexedSeq[(Int, PC)]
) extends Stmt[V] {

    final override def asSwitch : this.type = this

    final def astID = Switch.ASTID

    private[tac] def remapIndexes(pcToIndex: Array[Int]): Unit = {
        npairs = npairs.map { x ⇒ (x._1, pcToIndex(x._2)) }
        defaultTarget = pcToIndex(defaultTarget)
    }

    // Calling this method is only supported after the quadruples representation
    // is created and the remapping of pcs to instruction indexes has happened!
    def caseStmts: IndexedSeq[Int] = npairs.map(x ⇒ x._2)

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

case class Assignment[+V <: Var[V]](pc: PC, targetVar: V, expr: Expr[V]) extends Stmt[V] {

final override def asAssignment : this.type = this

    final def astID = Assignment.ASTID

    private[tac] def remapIndexes(pcToIndex: Array[Int]): Unit = {
        targetVar.remapIndexes(pcToIndex)
        expr.remapIndexes(pcToIndex)
    }

    override def toString: String = s"Assignment(pc=$pc,$targetVar,$expr)"

}
object Assignment {
    final val ASTID = 5
}

case class ReturnValue[+V <: Var[V]](pc: PC, expr: Expr[V]) extends Stmt[V] {

    final override def asReturnValue : this.type = this

    final def astID = ReturnValue.ASTID

    private[tac] def remapIndexes(pcToIndex: Array[Int]): Unit = expr.remapIndexes(pcToIndex)

    override def toString: String = s"ReturnValue(pc=$pc,$expr)"
}
object ReturnValue {
    final val ASTID = 6
}

sealed abstract class SimpleStmt extends Stmt[Nothing] {

    /**
     * Nothing to do.
     */
    final private[tac] def remapIndexes(pcToIndex: Array[Int]): Unit = {}

}

case class Return(pc: PC) extends SimpleStmt {

    final override def asReturn : this.type = this

    final def astID = Return.ASTID

    override def toString: String = s"Return(pc=$pc)"
}
object Return {
    final val ASTID = 7
}

case class Nop(pc: PC) extends SimpleStmt {

    final override def asNop : this.type = this

    final def astID = Nop.ASTID

    override def toString: String = s"Nop(pc=$pc)"
}
object Nop {
    final val ASTID = 8
}

sealed abstract class SynchronizationStmt[+V <: Var[V]] extends Stmt[V] {

    def objRef: Expr[V]

    final private[tac] def remapIndexes(pcToIndex: Array[Int]): Unit = {
        objRef.remapIndexes(pcToIndex)
    }
}

case class MonitorEnter[+V <: Var[V]](pc: PC, objRef: Expr[V]) extends SynchronizationStmt[V] {

    final override def asMonitorEnter : this.type = this

    final def astID = MonitorEnter.ASTID

    override def toString: String = s"MonitorEnter(pc=$pc,$objRef)"
}
object MonitorEnter {
    final val ASTID = 9
}

case class MonitorExit[+V <: Var[V]](pc: PC, objRef: Expr[V]) extends SynchronizationStmt[V] {

    final override def asMonitorExit : this.type = this

    final def astID = MonitorExit.ASTID

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

    final override def asArrayStore : this.type = this

    final def astID = ArrayStore.ASTID

    private[tac] def remapIndexes(pcToIndex: Array[Int]): Unit = {
        arrayRef.remapIndexes(pcToIndex)
        index.remapIndexes(pcToIndex)
        value.remapIndexes(pcToIndex)
    }

    override def toString: String = s"ArrayStore(pc=$pc,$arrayRef,$index,$value)"
}
object ArrayStore {
    final val ASTID = 11
}

case class Throw[+V <: Var[V]](pc: PC, exception: Expr[V]) extends Stmt[V] {

final override def asThrow : this.type = this

    final def astID = Throw.ASTID

    private[tac] def remapIndexes(pcToIndex: Array[Int]): Unit = exception.remapIndexes(pcToIndex)

    override def toString: String = s"Throw(pc=$pc,$exception)"
}
object Throw {
    final val ASTID = 12
}

sealed abstract class FieldWriteAccessStmt[+V <: Var[V]] extends Stmt[V] {
    def declaringClass: ObjectType
    def name: String
    def value: Expr[V]
}

case class PutStatic[+V <: Var[V]](
        pc:                PC,
        declaringClass:    ObjectType,
        name:              String,
        declaredFieldType: FieldType,
        value:             Expr[V]
) extends FieldWriteAccessStmt[V] {

final override def asPutStatic : this.type = this

    final def astID = PutStatic.ASTID

    private[tac] def remapIndexes(pcToIndex: Array[Int]): Unit = value.remapIndexes(pcToIndex)

    override def toString: String = {
        s"PutStatic(pc=$pc,${declaringClass.toJava},name,${declaredFieldType.toJava},$value)"
    }
}
object PutStatic {
    final val ASTID = 13
}

case class PutField[+V <: Var[V]](
        pc:                PC,
        declaringClass:    ObjectType,
        name:              String,
        declaredFieldType: FieldType,
        objRef:            Expr[V],
        value:             Expr[V]
) extends FieldWriteAccessStmt[V] {

final override def asPutField : this.type = this

    final def astID = PutField.ASTID

    private[tac] def remapIndexes(pcToIndex: Array[Int]): Unit = {
        objRef.remapIndexes(pcToIndex)
        value.remapIndexes(pcToIndex)
    }

    override def toString: String = {
        s"PutField(pc=$pc,${declaringClass.toJava},name,${declaredFieldType.toJava},$objRef,$value)"
    }
}
object PutField {
    final val ASTID = 14
}

sealed abstract class MethodCall[+V <: Var[V]] extends Stmt[V] with Call[V]

sealed abstract class InstanceMethodCall[+V <: Var[V]] extends MethodCall[V] {

    def receiver: Expr[V]

    private[tac] def remapIndexes(pcToIndex: Array[Int]): Unit = {
        receiver.remapIndexes(pcToIndex)
        params.foreach { p ⇒ p.remapIndexes(pcToIndex) }
    }

}

object InstanceMethodCall {

    def unapply[V <: Var[V]](
        call: InstanceMethodCall[V]
    ): Some[(PC, ReferenceType, Boolean, String, MethodDescriptor, Expr[V], Seq[Expr[V]])] = {
        import call._
        Some((pc, declaringClass, isInterface, name, descriptor, receiver, params))
    }
}

/**
 * Call of an instance method for which no virtual method call resolution has to happen.
 * I.e., it is either a super-call, a private instance method call or a constructor call.
 */
case class NonVirtualMethodCall[+V <: Var[V]](
        pc:             PC,
        declaringClass: ReferenceType,
        isInterface:    Boolean,
        name:           String,
        descriptor:     MethodDescriptor,
        receiver:       Expr[V],
        params:         Seq[Expr[V]]
) extends InstanceMethodCall[V] {

    final override def asNonVirtualMethodCall : this.type = this

    final def astID = NonVirtualMethodCall.ASTID

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
        pc:             PC,
        declaringClass: ReferenceType,
        isInterface:    Boolean,
        name:           String,
        descriptor:     MethodDescriptor,
        receiver:       Expr[V],
        params:         Seq[Expr[V]]
) extends InstanceMethodCall[V] {

final override def asVirtualMethodCall : this.type = this

    final def astID = VirtualMethodCall.ASTID

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
        pc:             PC,
        declaringClass: ReferenceType,
        isInterface:    Boolean,
        name:           String,
        descriptor:     MethodDescriptor,
        params:         Seq[Expr[V]]
) extends MethodCall[V] {

final override def asStaticMethodCall : this.type = this

    final def astID = StaticMethodCall.ASTID

    private[tac] def remapIndexes(pcToIndex: Array[Int]): Unit = {
        params.foreach { p ⇒ p.remapIndexes(pcToIndex) }
    }

    override def toString: String = {
        val sig = descriptor.toJava(name)
        val declClass = declaringClass.toJava
        val params = this.params.mkString("(", ",", ")")
        s"NonVirtualMethodCall(pc=$pc,$declClass,isInterface=$isInterface,$sig,$params)"
    }
}
object StaticMethodCall {
    final val ASTID = 17
}

/** An expression where the value is not further used. */
case class ExprStmt[+V <: Var[V]](pc: PC, expr: Expr[V]) extends Stmt[V] {

final override def asExprStmt : this.type = this

    final def astID = ExprStmt.ASTID

    private[tac] def remapIndexes(pcToIndex: Array[Int]): Unit = expr.remapIndexes(pcToIndex)

    override def toString: String = s"ExprStmt(pc=$pc,$expr)"

}
object ExprStmt {
    final val ASTID = 18
}

/**
 * A `checkcast` as defined by the JVM specification.
 */
case class Checkcast[+V <: Var[V]](pc: PC, value: Expr[V], cmpTpe: ReferenceType) extends Stmt[V] {

final override def asCheckcast : this.type = this

    final def astID: Int = Checkcast.ASTID

    private[tac] def remapIndexes(pcToIndex: Array[Int]): Unit = value.remapIndexes(pcToIndex)

    override def toString: String = s"Checkcast(pc=$pc,$value,${cmpTpe.toJava})"

}
object Checkcast {
    final val ASTID = 20
}

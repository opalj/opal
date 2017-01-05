/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2016
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
sealed trait Stmt extends ASTNode {

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
}

/**
 * @param targetStmt Index in the statements array.
 */
case class If(
        pc:                      PC,
        left:                    Expr,
        condition:               RelationalOperator,
        right:                   Expr,
        private[tac] var target: Int
) extends Stmt {

    final def astID = If.ASTID

    private[tac] def remapIndexes(pcToIndex: Array[Int]): Unit = target = pcToIndex(target)

    /**
     * The target statement that is executed if the condition evaluates to `true`.
     *
     * @note Calling this method is only supported after the quadruples representation
     *         is created and the re-mapping of `pc`s to instruction indexes has happened!
     */
    def targetStmt: Int = target
}
object If {
    final val ASTID = 0
}

/**
 * @param target First the `pc` (absolute) of the target instruction in the
 *          original bytecode array; then the index of the respective quadruples
 *          instruction.
 */
case class Goto(pc: PC, private var target: Int) extends Stmt {

    final def astID = Goto.ASTID

    private[tac] def remapIndexes(pcToIndex: Array[Int]): Unit = {
        target = pcToIndex(target)
    }

    /**
     * @note Calling this method is only supported after the quadruples representation
     *         is created and the re-mapping of `pc`s to instruction indexes has happened!
     *
     */
    def targetStmt: Int = target

}
object Goto {
    final val ASTID = 1
}

case class Ret(pc: PC, private var returnAddressVar: Var) extends Stmt {

    final def astID = Ret.ASTID

    private[tac] def remapIndexes(pcToIndex: Array[Int]): Unit = {}

}
object Ret {
    final val ASTID = 2
}

/**
 * A JSR Instruction is mapped to two instructions:
 *  1. the jsr instruction which performs a jump
 *  1. an assigment instruction at the jump target that initializes the local variable that
 *      is used to store the return address.
 *
 * @param target At creation time the `pc` (absolute) of the target instruction in the
 *          original bytecode array; then the index of the respective quadruples
 *          instruction.
 */
case class JumpToSubroutine(pc: PC, private[tac] var target: Int) extends Stmt {

    final def astID = JumpToSubroutine.ASTID

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

}
object JumpToSubroutine {
    final val ASTID = 3
}

case class Switch(
        pc:                        PC,
        private var defaultTarget: PC,
        index:                     Var,
        private var npairs:        IndexedSeq[(Int, PC)]
) extends Stmt {

    final def astID = Switch.ASTID

    private[tac] def remapIndexes(pcToIndex: Array[Int]): Unit = {
        npairs = npairs.map { x ⇒ (x._1, pcToIndex(x._2)) }
        defaultTarget = pcToIndex(defaultTarget)
    }

    // Calling this method is only supported after the quadruples representation
    // is created and the remapping of pcs to instruction indexes has happened!
    def targetStmt: IndexedSeq[Int] = npairs.map(x ⇒ x._2)

    // Calling this method is only supported after the quadruples representation
    // is created and the remapping of pcs to instruction indexes has happened!
    def defaultStmt: Int = defaultTarget
}
object Switch {
    final val ASTID = 4
}

case class Assignment(pc: PC, targetVar: Var, expr: Expr) extends Stmt {
    final def astID = Assignment.ASTID
    private[tac] def remapIndexes(pcToIndex: Array[Int]): Unit = expr.remapIndexes(pcToIndex)
}
object Assignment {
    final val ASTID = 5
}

case class ReturnValue(pc: PC, expr: Expr) extends Stmt {
    final def astID = ReturnValue.ASTID
    private[tac] def remapIndexes(pcToIndex: Array[Int]): Unit = expr.remapIndexes(pcToIndex)
}
object ReturnValue {
    final val ASTID = 6
}

sealed trait SimpleStmt extends Stmt {

    /**
     * Nothing to do.
     */
    final private[tac] def remapIndexes(pcToIndex: Array[Int]): Unit = {}
}

case class Return(pc: PC) extends SimpleStmt {
    final def astID = Return.ASTID
}
object Return {
    final val ASTID = 7
}

case class Nop(pc: PC) extends SimpleStmt {
    final def astID = Nop.ASTID
}
object Nop {
    final val ASTID = 8
}

sealed trait SynchronizationStmt extends Stmt {
    def objRef: Expr
    private[tac] def remapIndexes(pcToIndex: Array[Int]): Unit = objRef.remapIndexes(pcToIndex)
}

case class MonitorEnter(pc: PC, objRef: Expr) extends SynchronizationStmt {
    final def astID = MonitorEnter.ASTID
}
object MonitorEnter {
    final val ASTID = 9
}

case class MonitorExit(pc: PC, objRef: Expr) extends SynchronizationStmt {
    final def astID = MonitorExit.ASTID
}
object MonitorExit {
    final val ASTID = 10
}

case class ArrayStore(
        pc:       PC,
        arrayRef: Expr,
        index:    Expr,
        value:    Expr
) extends Stmt {
    final def astID = ArrayStore.ASTID
    private[tac] def remapIndexes(pcToIndex: Array[Int]): Unit = {
        arrayRef.remapIndexes(pcToIndex)
        index.remapIndexes(pcToIndex)
        value.remapIndexes(pcToIndex)
    }
}
object ArrayStore {
    final val ASTID = 11
}

case class Throw(pc: PC, exception: Expr) extends Stmt {
    final def astID = Throw.ASTID
    private[tac] def remapIndexes(pcToIndex: Array[Int]): Unit = {
        exception.remapIndexes(pcToIndex)
    }
}
object Throw {
    final val ASTID = 12
}

sealed trait FieldAccessStmt extends Stmt

case class PutStatic(
        pc:             PC,
        declaringClass: ObjectType, name: String,
        value: Expr
) extends FieldAccessStmt {
    final def astID = PutStatic.ASTID
    private[tac] def remapIndexes(pcToIndex: Array[Int]): Unit = {
        value.remapIndexes(pcToIndex)
    }

}
object PutStatic {
    final val ASTID = 13
}

case class PutField(
        pc:             PC,
        declaringClass: ObjectType, name: String,
        objRef: Expr,
        value:  Expr
) extends FieldAccessStmt {
    final def astID = PutField.ASTID
    private[tac] def remapIndexes(pcToIndex: Array[Int]): Unit = {
        objRef.remapIndexes(pcToIndex)
        value.remapIndexes(pcToIndex)
    }

}
object PutField {
    final val ASTID = 14
}

sealed trait MethodCall extends Call with Stmt

sealed trait InstanceMethodCall extends MethodCall {
    def receiver: Expr
    private[tac] def remapIndexes(pcToIndex: Array[Int]): Unit = {
        receiver.remapIndexes(pcToIndex)
        params.foreach { p ⇒ p.remapIndexes(pcToIndex) }
    }
}

object InstanceMethodCall {

    def unapply(
        call: InstanceMethodCall
    ): Some[(PC, ReferenceType, String, MethodDescriptor, Expr, Seq[Expr])] = {
        import call._
        Some((pc, declaringClass, name, descriptor, receiver, params))
    }
}

case class NonVirtualMethodCall(
        pc:             PC,
        declaringClass: ReferenceType,
        name:           String,
        descriptor:     MethodDescriptor,
        receiver:       Expr,
        params:         Seq[Expr]
) extends InstanceMethodCall {
    final def astID = NonVirtualMethodCall.ASTID
}
object NonVirtualMethodCall {
    final val ASTID = 15
}

case class VirtualMethodCall(
        pc:             PC,
        declaringClass: ReferenceType,
        name:           String,
        descriptor:     MethodDescriptor,
        receiver:       Expr,
        params:         Seq[Expr]
) extends InstanceMethodCall {
    final def astID = VirtualMethodCall.ASTID
}
object VirtualMethodCall {
    final val ASTID = 16
}

case class StaticMethodCall(
        pc:             PC,
        declaringClass: ReferenceType,
        name:           String,
        descriptor:     MethodDescriptor,
        params:         Seq[Expr]
) extends MethodCall {
    final def astID = StaticMethodCall.ASTID
    private[tac] def remapIndexes(pcToIndex: Array[Int]): Unit = {
        params.foreach { p ⇒ p.remapIndexes(pcToIndex) }
    }
}
object StaticMethodCall {
    final val ASTID = 17
}

/**
 * The underlying expression will always throw an exception.
 */
case class FailingExpression(
        pc:   PC,
        expr: Expr
) extends Stmt {
    final def astID = FailingExpression.ASTID
    private[tac] def remapIndexes(pcToIndex: Array[Int]): Unit = expr.remapIndexes(pcToIndex)
}
object FailingExpression {
    final val ASTID = 18
}

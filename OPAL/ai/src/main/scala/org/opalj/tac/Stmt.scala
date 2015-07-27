/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2014
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
 * @author Michael Eichberg
 * @author Roberts Kolosovs
 */
sealed trait Stmt {

    /**
     * The program counter of the original underyling bytecode instruction.
     *
     * The pc is independent of the index of the statement in the statements array!
     */
    def pc: UShort

    /**
     * Remaps the indexes
     */
    private[tac] def remapIndexes(pcToIndex: Array[Int]): Unit
}

/**
 * @param targetStmt Index in the statements array.
 */
case class If(
        pc: PC,
        left: Expr,
        condition: RelationalOperator,
        right: Expr,
        private[tac] var target: Int) extends Stmt {

    private[tac] def remapIndexes(pcToIndex: Array[Int]): Unit = {
        target = pcToIndex(target)
    }

    // Calling this method is only supported after the quadruples representation
    // is created and the remapping of pcs to instruction indexes has happened!
    def targetStmt: Int = target
}

case class Goto(pc: PC, private[tac] var target: Int) extends Stmt {

    private[tac] def remapIndexes(pcToIndex: Array[Int]): Unit = {
        target = pcToIndex(target)
    }

    // Calling this method is only supported after the quadruples representation
    // is created and the remapping of pcs to instruction indexes has happened!
    def targetStmt: Int = target

}

case class JumpToSubroutine(pc: PC, private[tac] var target: Int) extends Stmt {

    private[tac] def remapIndexes(pcToIndex: Array[Int]): Unit = {
        target = pcToIndex(target)
    }

    // Calling this method is only supported after the quadruples representation
    // is created and the remapping of pcs to instruction indexes has happened!
    def targetStmt: Int = target

}

case class Switch(pc: PC, private[tac] var defaultTarget: Int, index: Var, private[tac] var npairs: IndexedSeq[(Int, Int)]) extends Stmt {
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

sealed trait SimpleStmt extends Stmt {

    /**
     * Nothing to do.
     */
    final private[tac] def remapIndexes(pcToIndex: Array[Int]): Unit = {}
}

case class Assignment(pc: PC, target: Var, source: Expr) extends SimpleStmt

case class ReturnValue(pc: PC, expr: Expr) extends SimpleStmt

case class Return(pc: PC) extends SimpleStmt

case class Nop(pc: PC) extends SimpleStmt

case class EmptyStmt(pc: PC) extends SimpleStmt

case class MonitorEnter(pc: PC, objRef: Expr) extends SimpleStmt

case class MonitorExit(pc: PC, objRef: Expr) extends SimpleStmt

case class ArrayStore(pc: PC, arrayRef: Expr, index: Expr, operandVar: Expr) extends SimpleStmt

case class Throw(pc: PC, exception: Expr) extends SimpleStmt

case class PutStatic(
    pc: PC,
    declaringClass: ObjectType, name: String,
    value: Expr) extends SimpleStmt

case class PutField(
    pc: PC,
    declaringClass: ObjectType, name: String,
    objRef: Expr, value: Expr) extends SimpleStmt

/**
 * Call of a method.
 */
case class MethodCall(
    pc: PC,
    declaringClass: ReferenceType,
    name: String,
    descriptor: MethodDescriptor,
    receiver: Option[Expr],
    params: List[Expr],
    target: Option[Var]) extends SimpleStmt


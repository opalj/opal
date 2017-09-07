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

/**
 * Defines nodes used by statements and expressions.
 *
 * @author Michael Eichberg
 */
trait ASTNode[+V <: Var[V]] {

    /**
     * Each type of node is assigned a different `id` to make it easily possible
     * to do a switch over all nodes.
     */
    def astID: Int

    /**
     * `true` if the statement/expression is ''GUARANTEED'' to have no externally observable
     * effect if it is not executed.
     * Sideeffect free instructions can be removed if the result of the evaluation of the
     * expression/statement is not used. For those instructions, which may result in an exception, it
     * has to be guaranteed that the exception is '''NEVER''' thrown. For example, a div instruction
     * is sideeffect free if it is (statically) known that the divisor is always not equal to zero;
     * otherwise, even if the result value is not used, the expression is not (potentially) side
     * effect free. An array load is only side effect free if the array reference is non-null and
     * if the index is valid.
     *
     * @note '''Deeply nested expressions are not supported'''; i.e. an expression's sub-expressions
     *       have to be [[Var]] or [[Const]] expressions. Generally, a statements expressions have to
     *       to simple expressions too - except of the [[Assignment]] statement; in the latter case
     *       the right-expression can have references to simple expressions. Hence, in case of
     *       [[Assignment]] statements the side-effect freenes is determined by the referenced
     *       expression; in all other cases the side-effect freeness is determined directly by
     *       the statement/expression.
     *
     * @return `true` if the expression is ''GUARENTEED'' to have no side effect other than
     *        wasting some CPU cycles if it is not executed.
     */
    def isSideEffectFree: Boolean
}


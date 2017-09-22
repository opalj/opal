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
package graphs

import org.opalj.collection.immutable.IntArraySet

/**
 * Represents the control-dependence information.
 *
 * An instruction/statement is control dependent on a predicate (here: `if`, `switch` or any
 * instruction that may throw an exception) if the value of the predicate
 * controls the execution of the instruction.
 *
 * Note that the classical definition:
 *
 *     Let G be a control flow graph; Let X and Y be nodes in G; Y is control dependent on X iff
 *     there exists a directed path P from X to Y with any Z in P \ X is not post-dominated by Y.
 *
 * Is not well suited for methods with potentially infinite loops, exceptions and multiple exit
 * points. (See [[PostDominatorTree$.apply]] for further information.)
 *
 * @note    In the context of static analysis an instruction (e.g., invoke, idiv,...) that
 *          may throw an exception that results in a different control-flow, is also a `predicate`
 *          additionally to all ifs and switches.
 * @note    If the underlying method/CFG contains infinite loops then it is expected that the
 *          dominance frontiers are already `corrected` if the used post dominator tree was
 *          augmented in the first place!
 *
 * @author Michael Eichberg
 */
trait ControlDependencies {

    /**
     * @return  The nodes/basic blocks on which the given node/basic block is '''directly'''
     *          control dependent on. That is, the set of nodes which directly control whether x is
     *          executed or not.
     *          '''Directly''' means that there is at least one path between a node Y in
     *          `Control(X)/*the returned set*/` and X, whose selection is controlled by Y and
     *          which contains no nodes that may prevent the execution of X.
     */
    def xIsDirectlyControlDependentOn(x: Int): IntArraySet

    /**
     * Calls the function `f` with those nodes on which the given node `x` is control
     * dependent on.
     */
    def xIsControlDependentOn(x: Int)(f: Int ⇒ Unit): Unit

}

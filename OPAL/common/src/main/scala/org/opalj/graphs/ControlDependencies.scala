/* BSD 2-Clause License - see OPAL/LICENSE for details. */
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
    def xIsControlDependentOn(x: Int)(f: Int => Unit): Unit

}

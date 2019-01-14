/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac

/**
 * Facilitates matching against values of computational type category 1.
 *
 * @example
 * {{{
 * case v @ CTC1() => ...
 * }}}
 */
private[tac] object CTC1 {
    def unapply(value: Var[_]): Boolean = value.cTpe.categoryId == 1
}

/**
 * Facilitates matching against values of computational type category 2.
 *
 * @example
 * {{{
 * case v @ CTC2() => ...
 * }}}
 */
private[tac] object CTC2 {
    def unapply(value: Var[_]): Boolean = value.cTpe.categoryId == 2
}

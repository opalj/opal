/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses

import org.opalj.br.DefinedMethod
import org.opalj.br.Method

/**
 * @author Maximilian Rüsch
 */
package object string {

    type TAC = TACode[TACMethodParameter, V]

    /**
     * The type of entities the string analysis process.
     *
     * @note The analysis require further context information, see [[SContext]].
     */
    type SEntity = PV

    /**
     * String analysis process a local variable within a particular context, i.e. the method in which it is used.
     */
    type SContext = (SEntity, Method)

    /**
     * The entity used for requesting string constancy information for specific def sites of an entity. The def site
     * should be given as a [[org.opalj.br.PC]].
     */
    case class DUSiteEntity(pc: Int, dm: DefinedMethod, tac: TAC, entity: SEntity)
}

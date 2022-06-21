/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai

import org.opalj.br.PC

/**
 * Mixin this trait if a domain needs access to the operands ([[Domain#OperandsArray]])
 * and/or locals ([[Domain#LocalsArray]]).
 *
 * ==Usage==
 * It is sufficient to mixin this trait in a [[Domain]] that needs to get access to the
 * memory structures. The abstract interpreter will then perform the initialization.
 *
 * This information is set immediately before the abstract interpretation is
 * started/continued.
 *
 * @author Michael Eichberg
 */
trait TheMemoryLayout { domain: ValuesDomain =>

    private[this] var theOperandsArray: OperandsArray = null

    private[this] var theLocalsArray: LocalsArray = null

    private[this] var theMemoryLayoutBeforeSubroutineCall: List[(PC, OperandsArray, LocalsArray)] = null

    private[this] var theSubroutinesOperandsArray: OperandsArray = null
    private[this] var theSubroutinesLocalsArray: LocalsArray = null

    private[ai] def setMemoryLayout(
        theOperandsArray:                    this.OperandsArray,
        theLocalsArray:                      this.LocalsArray,
        theMemoryLayoutBeforeSubroutineCall: List[(PC, this.OperandsArray, this.LocalsArray)],
        theSubroutinesOperandsArray:         this.OperandsArray,
        theSubroutinesLocalsArray:           this.LocalsArray
    ): Unit = {
        this.theOperandsArray = theOperandsArray
        this.theLocalsArray = theLocalsArray
        this.theMemoryLayoutBeforeSubroutineCall = theMemoryLayoutBeforeSubroutineCall
        this.theSubroutinesOperandsArray = theSubroutinesOperandsArray
        this.theSubroutinesLocalsArray = theSubroutinesLocalsArray
    }

    def operandsArray: OperandsArray = theOperandsArray

    def localsArray: LocalsArray = theLocalsArray

    def memoryLayoutBeforeSubroutineCall: List[(PC, this.OperandsArray, this.LocalsArray)] = {
        theMemoryLayoutBeforeSubroutineCall
    }

    def subroutinesOperandsArray: OperandsArray = theSubroutinesOperandsArray

    def subroutinesLocalsArray: LocalsArray = theSubroutinesLocalsArray

}

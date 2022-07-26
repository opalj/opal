/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai

import org.opalj.br.instructions.Instruction
import org.opalj.br.LiveVariables
import org.opalj.collection.immutable.IntTrieSet

/**
 * Mixin this trait if the domain needs information about the structure of the code.
 *
 * ==Usage==
 * It is sufficient to mixin this trait in a [[Domain]] that needs to get access to
 * the code array. The abstract interpreter will then perform the initialization.
 *
 * This information is set immediately before the abstract interpretation is started/continued.
 *
 * @author Michael Eichberg
 */
trait TheCodeStructure { domain: ValuesDomain =>

    private[this] var theInstructions: Array[Instruction] = null

    private[this] var theCFJoins: IntTrieSet = null

    def instructions: Array[Instruction] = theInstructions

    /**
     * @see    [[org.opalj.br.Code.cfPCs]],
     *         [[org.opalj.br.Code.cfJoins]],
     *         [[org.opalj.br.Code.predecessorPCs]]
     */
    def cfJoins: IntTrieSet = theCFJoins

    /**
     * Sets the code structure.
     *
     * This method is called by the AI framework immediately before the interpretation (continues).
     */
    private[ai] def setCodeStructure(
        theInstructions:  Array[Instruction],
        theCFJoins:       IntTrieSet,
        theLiveVariables: LiveVariables
    ): Unit = {
        this.theInstructions = theInstructions
        this.theCFJoins = theCFJoins
    }

}

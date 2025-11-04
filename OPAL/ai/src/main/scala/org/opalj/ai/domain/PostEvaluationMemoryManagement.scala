/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai
package domain

import scala.compiletime.uninitialized

import org.opalj.br.instructions.Instruction
import org.opalj.util.elidedAssert

/**
 * Provides the possibility to further update the memory layout (registers and operands)
 * after the execution of an instruction, but before any potential join is performed.
 *
 * Using this domain is only safe if the (partial-)domains that use this functionality
 * never interfere with each other.
 *
 * @note    If this domain is mixed in then the domain cannot be used to simultaneously analyze
 *          multiple different methods at the same time.
 *
 * @author Michael Eichberg
 */
trait PostEvaluationMemoryManagement extends CoreDomainFunctionality {

    private var oldValue: DomainValue = uninitialized
    private var newValueAfterEvaluation: DomainValue = uninitialized
    private var newValueAfterException: DomainValue = uninitialized

    protected def updateAfterExecution(
        oldValue:                DomainValue,
        newValueAfterEvaluation: DomainValue,
        newValueAfterException:  DomainValue
    ): Unit = {
        elidedAssert(this.oldValue eq null, "another update is already registered")

        elidedAssert(oldValue ne null)
        elidedAssert((newValueAfterEvaluation ne null) || (newValueAfterException ne null))
        elidedAssert(oldValue ne newValueAfterEvaluation, "useless self update")
        elidedAssert(oldValue ne newValueAfterException, "useless self update")

        this.oldValue = oldValue
        this.newValueAfterEvaluation = newValueAfterEvaluation
        this.newValueAfterException = newValueAfterException
    }

    protected def updateAfterEvaluation(oldValue: DomainValue, newValue: DomainValue): Unit = {
        updateAfterExecution(oldValue, newValue, null)
    }

    protected def updateAfterException(oldValue: DomainValue, newValue: DomainValue): Unit = {
        updateAfterExecution(oldValue, null, newValue)
    }

    abstract override def afterEvaluation(
        pc:                       Int,
        instruction:              Instruction,
        oldOperands:              Operands,
        oldLocals:                Locals,
        targetPC:                 Int,
        isExceptionalControlFlow: Boolean,
        forceJoin:                Boolean,
        newOperands:              Operands,
        newLocals:                Locals
    ): (Operands, Locals) = {
        val oldValue = this.oldValue
        if (oldValue ne null) {
            val (operands1, locals1) =
                if (isExceptionalControlFlow)
                    updateMemoryLayout(oldValue, newValueAfterException, newOperands, newLocals)
                else
                    updateMemoryLayout(oldValue, newValueAfterEvaluation, newOperands, newLocals)
            this.oldValue = null
            this.newValueAfterEvaluation = null
            this.newValueAfterException = null

            super.afterEvaluation(
                pc,
                instruction,
                oldOperands,
                oldLocals,
                targetPC,
                isExceptionalControlFlow,
                forceJoin,
                operands1,
                locals1
            )
        } else {
            super.afterEvaluation(
                pc,
                instruction,
                oldOperands,
                oldLocals,
                targetPC,
                isExceptionalControlFlow,
                forceJoin,
                newOperands,
                newLocals
            )
        }
    }

}

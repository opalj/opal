/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ba

/**
 * Pseudo instructions which generate the [[org.opalj.br.ExceptionHandler]] of the
 * [[org.opalj.br.Code]] attribute. An ExceptionHandler is composed of the three pseudo instructions
 * [[TRY]], [[TRYEND]] and [[CATCH]] with the same identifier symbol. The exceptionHandler includes
 * all instructions between [[TRY]] and [[TRYEND]]. The `handlerPC` is the pc of the instruction
 * following the [[CATCH]]. If the label contains numbers at the end, the ExceptionHandlers are
 * sorted ascending by that number in the ExceptionHandlerTable. Otherwise, the ExceptionHandlers
 * are lexically sorted by their label.
 *
 * @author Malte Limmeroth
 */
trait ExceptionHandlerElement extends PseudoInstruction {
    final override def isExceptionHandlerElement: Boolean = true
    def id: Symbol
}

/**
 * Pseudo instruction marking the start of a [[org.opalj.br.ExceptionHandler]].
 *
 * @see [[ExceptionHandlerElement]]
 */
case class TRY(id: Symbol) extends ExceptionHandlerElement {
    final override def isTry: Boolean = true
    final override def isCatch: Boolean = false
    final override def asTry: TRY = this
}

/**
 * Pseudo instruction marking the end of a [[org.opalj.br.ExceptionHandler]].
 *
 * @see [[ExceptionHandlerElement]]
 */
case class TRYEND(id: Symbol) extends ExceptionHandlerElement {
    final override def isTry: Boolean = false
    final override def isCatch: Boolean = false
}

/**
 * Pseudo instruction marking the handler of a [[org.opalj.br.ExceptionHandler]].
 *
 * @see [[ExceptionHandlerElement]]
 */
case class CATCH private (
        id:          Symbol,
        handlerType: Option[br.ObjectType]
) extends ExceptionHandlerElement {
    final override def isTry: Boolean = false
    final override def isCatch: Boolean = true
}

/**
 * Factory methods to create an [[CATCH]] pseudo instruction.
 *
 * @author Malte Limmeroth
 */
object CATCH {

    /**
     * Creates a [[CATCH]] pseudo instruction marking the handler of a
     * [[org.opalj.br.ExceptionHandler]] catching the given `handlerTpye`.
     * @see [[ExceptionHandlerElement]]
     * @param handlerType the fqn of the caught exception class
     */
    def apply(id: Symbol, handlerType: String): CATCH = {
        new CATCH(id, Some(br.ObjectType(handlerType)))
    }

    /**
     * Creates a [[CATCH]] pseudo instruction marking the handler of a
     * [[org.opalj.br.ExceptionHandler]] catching any exception.
     * @see [[ExceptionHandlerElement]]
     */
    def apply(id: Symbol): CATCH = new CATCH(id, None)

    def unapply(arg: CATCH): Option[(Symbol, Option[br.ObjectType])] = {
        Some((arg.id, arg.handlerType))
    }
}

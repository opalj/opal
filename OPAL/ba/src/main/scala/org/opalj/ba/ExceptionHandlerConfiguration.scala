/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ba

import scala.collection.immutable.ArraySeq
import scala.collection.mutable

private[ba] class ExceptionHandlerConfiguration(
        var position:  Int                   = 0,
        var startPC:   Int                   = -1, // -1 is illegal and used to identify missing labels
        var endPC:     Int                   = -1, // -1 is illegal and used to identify missing labels
        var handlerPC: Int                   = -1, // -1 is illegal and used to identify missing labels
        var catchType: Option[br.ObjectType] = None
)

/**
 * Incrementally builds the [[org.opalj.br.ExceptionHandlers]] from the added pseudo instructions
 * ([[ExceptionHandlerElement]]) representing an [[org.opalj.br.ExceptionHandler]].
 *
 * @author Malte Limmeroth
 *         @author Michael Eichberg
 */
class ExceptionHandlerTableBuilder {

    private[this] val map: mutable.Map[Symbol, ExceptionHandlerConfiguration] = mutable.Map.empty

    private[this] def getExceptionHandlerBuilder(id: Symbol): ExceptionHandlerConfiguration = {
        map.getOrElseUpdate(id, new ExceptionHandlerConfiguration())
    }

    def add(element: ExceptionHandlerElement, pc: br.PC): this.type = {
        val handler = getExceptionHandlerBuilder(element.id)
        element match {
            case TRY(_)    => handler.startPC = pc
            case TRYEND(_) => handler.endPC = pc
            case CATCH(_, position, catchType) =>
                handler.handlerPC = pc
                handler.position = position
                handler.catchType = catchType
        }
        this
    }

    /**
     * Generates the final [[org.opalj.br.ExceptionHandlers]] from the added pseudo instructions.
     * Fails if any handler is incomplete. That is, only one or two of the three pseudo instructions
     * for a single id was added.
     */
    def result(): br.ExceptionHandlers = {
        ArraySeq.from(map)
            .sortWith((left, right) => left._2.position < right._2.position).
            map[br.ExceptionHandler] { e =>
                val (id, ehBuilder) = e
                val errorMsg = s"invalid exception handler ($id): %s"
                require(ehBuilder.startPC >= 0, errorMsg.format(s"startPC = ${ehBuilder.startPC}"))
                require(ehBuilder.endPC >= 0, errorMsg.format(s"endPC = ${ehBuilder.endPC}"))
                require(ehBuilder.handlerPC >= 0, errorMsg.format(s"handlerPC = ${ehBuilder.handlerPC}"))
                require(ehBuilder.startPC < ehBuilder.endPC, errorMsg.format("empty sequence"))
                br.ExceptionHandler(
                    ehBuilder.startPC, ehBuilder.endPC, ehBuilder.handlerPC,
                    ehBuilder.catchType
                )
            }
    }
}

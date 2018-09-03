/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ba

import org.opalj.collection.immutable.RefArray

import scala.collection.mutable

private[ba] class ExceptionHandlerBuilder(
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
 */
class ExceptionHandlerGenerator {

    private[this] val map: mutable.Map[Symbol, ExceptionHandlerBuilder] = mutable.Map.empty

    private[this] def getExceptionHandlerBuilder(id: Symbol): ExceptionHandlerBuilder = {
        map.getOrElseUpdate(id, new ExceptionHandlerBuilder())
    }

    def add(element: ExceptionHandlerElement, pc: br.PC): Unit = {
        val handler = getExceptionHandlerBuilder(element.id)
        element match {
            case TRY(_)    ⇒ handler.startPC = pc
            case TRYEND(_) ⇒ handler.endPC = pc
            case CATCH(_, catchType) ⇒
                handler.handlerPC = pc
                handler.catchType = catchType
        }
    }

    private def sortByLastNumber(left: Symbol, right: Symbol) = {
        val pattern = "^(.*?)([0-9]*)$".r
        val pattern(lName, lId) = left.name
        val pattern(rName, rId) = right.name
        if (lId.isEmpty && rId.isEmpty) {
            lName < rName
        } else if (lId.isEmpty) {
            false
        } else if (rId.isEmpty) {
            true
        } else {
            Integer.parseInt(lId) < Integer.parseInt(rId)
        }
    }

    /**
     * Generates the final [[org.opalj.br.ExceptionHandlers]] from the added pseudo instructions.
     * Fails if any handler is incomplete. That is, only one or two of the three pseudo instructions
     * for a single id was added.
     */
    def result(): br.ExceptionHandlers = {
        // TODO Clarify why the exception handlers are sorted... this may prevent some legal specifications, doesn't it?
        RefArray.from(map).
            _UNSAFE_sortedWith((left, right) ⇒ sortByLastNumber(left._1, right._1)).
            map[br.ExceptionHandler] { e ⇒
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
        /*
        map.toIndexedSeq.sortWith((left, right) ⇒ sortByLastNumber(left._1, right._1)).map { e ⇒
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
        */
    }
}

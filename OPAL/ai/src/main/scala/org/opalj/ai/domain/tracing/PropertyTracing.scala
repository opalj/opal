/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai
package domain
package tracing

import org.opalj.collection.immutable.IntTrieSet
import org.opalj.br._

/**
 * Enables the tracing of some user-defined property while a method is analyzed.
 * A possible property could be, e.g., whether a certain check is performed on
 * all intraprocedural control flows.
 *
 * After the abstract interpretation of a method, the property is associated with
 * all ''executed instructions'' and can be queried. For example to get the information
 * whether the check was performed on all paths to all exit points.
 *
 * @author Michael Eichberg
 */
trait PropertyTracing extends CoreDomainFunctionality with CustomInitialization { domain: Domain =>

    trait Property { def join(otherProperty: DomainProperty): Update[DomainProperty] }

    type DomainProperty <: Property

    def initialPropertyValue(): DomainProperty

    /**
     * The type of the property. E.g., `Boolean` or some other type.
     */
    implicit val DomainPropertyTag: reflect.ClassTag[DomainProperty]

    /**
     * The array which stores the value the property has when the respective.
     * instruction is executed.
     */
    private var propertiesArray: Array[DomainProperty] = _

    abstract override def initProperties(code: Code, cfJoins: IntTrieSet, locals: Locals): Unit = {

        super.initProperties(code, cfJoins, locals)

        this.propertiesArray = new Array(code.instructions.length)
        this.propertiesArray(0) = initialPropertyValue()
    }

    def getProperty(pc: PC): DomainProperty = propertiesArray(pc)

    def setProperty(pc: PC, property: DomainProperty): Unit = propertiesArray(pc) = property

    /**
     * Returns a string representation of the property associated with the given
     * instruction. This string representation is used by OPAL's tools to enable
     * a meaningful representation of the property.
     *
     * (Run `de...ai.util.InterpretMethod` with a domain that traces properties.)
     */
    abstract override def properties(
        pc:               Int,
        propertyToString: AnyRef => String
    ): Option[String] = {

        val thisProperty = Option(propertiesArray(pc)).map(_.toString())

        super.properties(pc, propertyToString) match {
            case superProperty @ Some(description) =>
                thisProperty map (_+"; "+description) orElse superProperty
            case None =>
                thisProperty
        }

    }

    abstract override def flow(
        currentPC:                        PC,
        currentOperands:                  Operands,
        currentLocals:                    Locals,
        successorPC:                      PC,
        isSuccessorScheduled:             Answer,
        isExceptionalControlFlow:         Boolean,
        abruptSubroutineTerminationCount: Int,
        wasJoinPerformed:                 Boolean,
        worklist:                         List[PC],
        operandsArray:                    OperandsArray,
        localsArray:                      LocalsArray,
        tracer:                           Option[AITracer]
    ): List[PC] = {

        val forceScheduling: Boolean = {
            if (wasJoinPerformed) {
                propertiesArray(successorPC) join propertiesArray(currentPC) match {
                    case NoUpdate =>
                        false
                    case StructuralUpdate(property) =>
                        propertiesArray(successorPC) = property
                        true
                    case MetaInformationUpdate(property) =>
                        propertiesArray(successorPC) = property
                        false
                }
            } else {
                propertiesArray(successorPC) = propertiesArray(currentPC)
                // actually, it doesn't matter as we will continue the analysis anyway
                // but if the value is false we can omit the test where the value is
                // scheduled
                false
            }
        }

        var newIsSuccessorScheduled = isSuccessorScheduled
        val newWorklist =
            if (forceScheduling && isSuccessorScheduled.isNoOrUnknown) {
                newIsSuccessorScheduled = Yes
                val newWorklist =
                    schedule(successorPC, abruptSubroutineTerminationCount, worklist)
                if ((newWorklist ne worklist) && tracer.isDefined) {
                    // the instruction was not yet scheduled for another evaluation
                    tracer.get.flow(domain)(currentPC, successorPC, isExceptionalControlFlow)
                }
                newWorklist
            } else {
                worklist
            }
        super.flow(
            currentPC, currentOperands, currentLocals,
            successorPC, newIsSuccessorScheduled,
            isExceptionalControlFlow, abruptSubroutineTerminationCount,
            wasJoinPerformed,
            newWorklist,
            operandsArray, localsArray,
            tracer
        )
    }
}

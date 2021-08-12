/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai
package domain
package tracing

import org.opalj.br.instructions.ReturnInstruction

/**
 * Enables the tracing of a single boolean property where the precise semantics
 * is determined by the user.
 *
 * @author Michael Eichberg
 */
trait SimpleBooleanPropertyTracing
    extends PropertyTracing
    with RecordReturnFromMethodInstructions { domain: Domain with TheCode =>

    /**
     * A name associated with the property. Used for debugging purposes only.
     */
    def propertyName: String

    class BooleanProperty private[SimpleBooleanPropertyTracing] (val state: Boolean)
        extends Property {

        def join(otherProperty: DomainProperty): Update[DomainProperty] =
            this.state & otherProperty.state match {
                case `state`  => NoUpdate
                case newState => StructuralUpdate(new BooleanProperty(newState))
            }

        override def toString: String = domain.propertyName+"("+state+")"
    }

    def updateProperty(pc: Int, newState: Boolean): Unit = {
        setProperty(pc, new BooleanProperty(newState))
    }

    final type DomainProperty = BooleanProperty

    final val DomainPropertyTag: reflect.ClassTag[DomainProperty] = implicitly

    def initialPropertyValue(): DomainProperty = new BooleanProperty(false)

    def hasPropertyOnExit: Boolean = {
        allReturnFromMethodInstructions forall { pc => getProperty(pc).state }
    }

    def hasPropertyOnNormalReturn: Boolean = {
        allReturnFromMethodInstructions forall { pc =>
            !code.instructions(pc).isInstanceOf[ReturnInstruction] || getProperty(pc).state
        }
    }
}

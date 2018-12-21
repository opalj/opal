/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package properties

import org.opalj.br.Field
import org.opalj.br.instructions.GETFIELD
import org.opalj.br.instructions.INVOKEVIRTUAL
import org.opalj.br.instructions.INVOKEDYNAMIC
import org.opalj.br.analyses.SomeProject
import org.opalj.br.instructions.INVOKESTATIC
import org.opalj.br.instructions.INVOKEINTERFACE
import org.opalj.br.instructions.INVOKESPECIAL

sealed trait FieldPrematurelyReadPropertyMetaInformation extends PropertyMetaInformation {

    type Self = FieldPrematurelyRead

}

/**
 * Identifies fields that are read before they are initialized. This may e.g. happen through virtual
 * calls in a super class constructor.
 *
 * @author Dominik Helm
 */
sealed trait FieldPrematurelyRead extends Property with FieldPrematurelyReadPropertyMetaInformation {

    final def key: PropertyKey[FieldPrematurelyRead] = FieldPrematurelyRead.key

}

object FieldPrematurelyRead extends FieldPrematurelyReadPropertyMetaInformation {

    final val PropertyKeyName = "opalj.FieldPrematurelyRead"

    final val key: PropertyKey[FieldPrematurelyRead] = {
        PropertyKey.create(
            PropertyKeyName,
            (ps: PropertyStore, _, f: Field) ⇒ {
                val p = ps.context(classOf[SomeProject])
                if (isPrematurelyReadFallback(p, f)) PrematurelyReadField
                else NotPrematurelyReadField
            }
        )
    }

    def isPrematurelyReadFallback(p: SomeProject, field: Field): Boolean = {
        val classType = field.classFile.thisType
        val classes = p.classHierarchy.allSubclassTypes(classType, reflexive = true)
        var prematurelyRead = false
        var superclassType = field.classFile.superclassType
        while (!prematurelyRead && superclassType.isDefined) {
            val classFile = p.classFile(superclassType.get)
            prematurelyRead = classFile.forall(_.methods exists { m ⇒
                m.isConstructor && m.body.get.instructions.exists { inst ⇒
                    inst != null && (inst.opcode match {
                        case INVOKEDYNAMIC.opcode | INVOKEINTERFACE.opcode | INVOKESTATIC.opcode |
                            INVOKEVIRTUAL.opcode ⇒
                            true
                        case _ ⇒
                            false
                    })
                }
            })
            superclassType = classFile.flatMap(_.superclassType)
        }
        if (!prematurelyRead) {
            prematurelyRead = classes exists { classType ⇒
                val classFile = p.classFile(classType)
                classFile.forall(_.methods exists { m ⇒
                    m.isConstructor && m.body.get.instructions.exists { inst ⇒
                        inst != null && (inst.opcode match {
                            case GETFIELD.opcode ⇒
                                val GETFIELD(declClass, name, fieldType) = inst
                                declClass == classType && name == field.name
                            case INVOKEDYNAMIC.opcode | INVOKEINTERFACE.opcode |
                                INVOKESTATIC.opcode | INVOKEVIRTUAL.opcode ⇒
                                true
                            case INVOKESPECIAL.opcode ⇒
                                inst.asInvocationInstruction.name != "<init>"
                            case _ ⇒
                                false
                        })
                    }
                })
            }
        }
        prematurelyRead
    }
}

case object NotPrematurelyReadField extends FieldPrematurelyRead

case object PrematurelyReadField extends FieldPrematurelyRead

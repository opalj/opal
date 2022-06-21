/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * Fetch ﬁeld from object.
 *
 * ==NOTE==
 * Getting an Array's length is translated to the special `arraylength` instruction.
 * E.g., in the following case:
 * {{{
 * Object[] os = ...; os.length
 * }}}
 * `os.length` is determined using the special `arraylength` instruction.
 *
 * @see [[org.opalj.br.instructions.FieldAccess]] for additional
 *      pattern matching support.
 *
 * @author Michael Eichberg
 */
case class GETFIELD(
        declaringClass: ObjectType,
        name:           String,
        fieldType:      FieldType
) extends FieldReadAccess {

    final def opcode: Opcode = GETFIELD.opcode

    final def mnemonic: String = "getfield"

    final def jvmExceptions: List[ObjectType] = FieldAccess.jvmExceptions

    final def mayThrowExceptions: Boolean = true

    final def numberOfPoppedOperands(ctg: Int => ComputationalTypeCategory): Int = 1

    final def stackSlotsChange: Int = -1 + fieldType.computationalType.operandSize

    final def nextInstructions(
        currentPC:             PC,
        regularSuccessorsOnly: Boolean
    )(
        implicit
        code:           Code,
        classHierarchy: ClassHierarchy = ClassHierarchy.PreInitializedClassHierarchy
    ): List[PC] = {
        if (regularSuccessorsOnly)
            List(indexOfNextInstruction(currentPC))
        else
            Instruction.nextInstructionOrExceptionHandler(
                this, currentPC, ObjectType.NullPointerException
            )
    }

    override def toString = "get "+declaringClass.toJava+"."+name+" : "+fieldType.toJava

}

/**
 * General information and factory methods.
 *
 * @author Malte Limmeroth
 */
object GETFIELD extends InstructionMetaInformation {

    final val opcode = 180

    /**
     * Factory method to create [[GETFIELD]] instructions.
     *
     * @param   declaringClassName The binary name of the field's declaring class, e.g.
     *          `java/lang/Object`.
     * @param   name The field's name.
     * @param   fieldTypeName The field's type; see [[org.opalj.br.FieldType$]] for the concrete
     *          syntax.
     */
    def apply(declaringClassName: String, name: String, fieldTypeName: String): GETFIELD = {
        GETFIELD(ObjectType(declaringClassName), name, FieldType(fieldTypeName))
    }

}

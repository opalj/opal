/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * Set field in object.
 *
 * @see [[org.opalj.br.instructions.FieldAccess]] for additional
 *      pattern matching support.
 *
 * @author Michael Eichberg
 */
case class PUTFIELD(
        declaringClass: ObjectType,
        name:           String,
        fieldType:      FieldType
) extends FieldWriteAccess {

    final def opcode: Opcode = PUTFIELD.opcode

    final def mnemonic: String = "putfield"

    final def jvmExceptions: List[ObjectType] = FieldAccess.jvmExceptions

    final def mayThrowExceptions: Boolean = true

    final def numberOfPoppedOperands(ctg: Int => ComputationalTypeCategory): Int = 2

    final def stackSlotsChange: Int = -fieldType.computationalType.operandSize - 1

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

    override def toString = "put "+declaringClass.toJava+"."+name+" : "+fieldType.toJava

}

/**
 * General information and factory methods.
 *
 * @author Malte Limmeroth
 */
object PUTFIELD extends InstructionMetaInformation {

    final val opcode = 181

    /**
     * Factory method to create [[PUTFIELD]] instructions.
     *
     * @param   declaringClassName The binary name of the field's declaring class, e.g.
     *          `java/lang/Object`.
     * @param   name The field's name.
     * @param   fieldTypeName The field's type; see [[org.opalj.br.FieldType$]] for the concrete
     *          syntax.
     */
    def apply(declaringClassName: String, name: String, fieldTypeName: String): PUTFIELD = {
        PUTFIELD(ObjectType(declaringClassName), name, FieldType(fieldTypeName))
    }

}

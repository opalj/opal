/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * Set static field in class.
 *
 * @see [[org.opalj.br.instructions.FieldAccess]] for additional
 *      pattern matching support.
 *
 * @author Michael Eichberg
 */
case class PUTSTATIC(
        declaringClass: ObjectType,
        name:           String,
        fieldType:      FieldType
) extends FieldWriteAccess {

    final def opcode: Opcode = PUTSTATIC.opcode

    final def mnemonic: String = "putstatic"

    final def jvmExceptions: List[ObjectType] = Nil

    final def mayThrowExceptions: Boolean = false

    final def numberOfPoppedOperands(ctg: Int => ComputationalTypeCategory): Int = 1

    final def stackSlotsChange: Int = -fieldType.computationalType.operandSize

    final def nextInstructions(
        currentPC:             PC,
        regularSuccessorsOnly: Boolean
    )(
        implicit
        code:           Code,
        classHierarchy: ClassHierarchy = ClassHierarchy.PreInitializedClassHierarchy
    ): List[PC] = {
        List(indexOfNextInstruction(currentPC))
    }

    override def toString =
        "put static "+declaringClass.toJava+"."+name+" : "+fieldType.toJava

}

/**
 * General information and factory methods.
 *
 * @author Malte Limmeroth
 */
object PUTSTATIC extends InstructionMetaInformation {

    final val opcode = 179

    /**
     * Factory method to create [[PUTSTATIC]] instructions.
     *
     * @param   declaringClassName The binary name of the field's declaring class, e.g.
     *          `java/lang/Object`.
     * @param   name The field's name.
     * @param   fieldTypeName The field's type; see [[org.opalj.br.FieldType$]] for the concrete
     *          syntax.
     */
    def apply(declaringClassName: String, name: String, fieldTypeName: String): PUTSTATIC = {
        PUTSTATIC(ObjectType(declaringClassName), name, FieldType(fieldTypeName))
    }

}

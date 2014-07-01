package org.opalj.br

package object instructions {
    /**
     * Returns a constant value instruction that corresponds to the given type's default
     * value for automatically initialized fields.
     */
    def defaultValueInstruction(fieldType: FieldType): Instruction = fieldType match {
        case IntegerType      ⇒ ICONST_0
        case ByteType         ⇒ ICONST_0
        case CharType         ⇒ ICONST_0
        case ShortType        ⇒ ICONST_0
        case BooleanType      ⇒ ICONST_0
        case LongType         ⇒ LCONST_0
        case FloatType        ⇒ FCONST_0
        case DoubleType       ⇒ DCONST_0
        case _: ReferenceType ⇒ ACONST_NULL
    }

    /**
     * Returns the `return` instruction that is appropriate for returning a variable of
     * the given type (or void).
     */
    def returnInstruction(theType: Type): Instruction = theType match {
        case VoidType         ⇒ RETURN
        case IntegerType      ⇒ IRETURN
        case ShortType        ⇒ IRETURN
        case ByteType         ⇒ IRETURN
        case CharType         ⇒ IRETURN
        case BooleanType      ⇒ IRETURN
        case LongType         ⇒ LRETURN
        case FloatType        ⇒ FRETURN
        case DoubleType       ⇒ DRETURN
        case _: ReferenceType ⇒ ARETURN
    }

    /**
     * Returns a `load` instruction appropriate for a local variable of the given type
     * at the given index. I.e. for indices 0 through 3, the corresponding `XLOAD_N`
     * instruction is returned, while for any index greater than 3, the generic `XLOAD`
     * is returned.
     */
    def loadVariableInstruction(fieldType: FieldType, index: Int): Instruction =
        fieldType match {
            case IntegerType      ⇒ ILOAD(index)
            case ByteType         ⇒ ILOAD(index)
            case ShortType        ⇒ ILOAD(index)
            case CharType         ⇒ ILOAD(index)
            case BooleanType      ⇒ ILOAD(index)
            case LongType         ⇒ LLOAD(index)
            case FloatType        ⇒ FLOAD(index)
            case DoubleType       ⇒ DLOAD(index)
            case _: ReferenceType ⇒ ALOAD(index)
        }
}
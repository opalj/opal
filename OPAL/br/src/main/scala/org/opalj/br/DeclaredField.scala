/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br

/**
 * Represents a declared field of a class identified by [[declaringClassType]];
 * that is, a field which belongs to the API of the class itself or a super class thereof.
 *
 * @author Maximilian Rüsch
 */
sealed abstract class DeclaredField {

    /**
     * The declaring type; the returned type may not define the field; it could be defined by
     * one or more super classes/interfaces in case of Java 8+.
     */
    def declaringClassType: ObjectType
    def name: String
    def fieldType: FieldType
    def toJava: String = s"${fieldType.toJava} ${declaringClassType.toJava}.$name"

    val isDefinedField: Boolean
    def asDefinedField: DefinedField
    def definedField: Field

    /**
     * A unique ID.
     */
    val id: Int

    override def equals(other: Any): Boolean = other match {
        case that: DeclaredField => id == that.id
        case _                   => false
    }

    override def hashCode(): Int = id
}

/**
 * Represents a field belonging to the API of the specified class type, where the original
 * field definition is not available (in the context of the current analysis).
 */
final class VirtualDeclaredField private[br] (
        override val declaringClassType: ObjectType,
        override val name:               String,
        override val fieldType:          FieldType,
        override val id:                 Int
) extends DeclaredField {

    override val isDefinedField = false
    override def definedField: Field = throw new UnsupportedOperationException();
    override def asDefinedField: DefinedField = throw new ClassCastException();

    override def toString: String = {
        s"VirtualDeclaredField(${declaringClassType.toJava},$name,$fieldType)"
    }
}

/**
 * Represents a defined field; i.e., a field which belongs to the (public and private) API of a class along with its
 * original reference.
 */
final class DefinedField private[br] (
        override val id:           Int,
        override val definedField: Field
) extends DeclaredField {

    override def declaringClassType: ObjectType = definedField.declaringClassFile.thisType
    override def name: String = definedField.name
    override def fieldType: FieldType = definedField.fieldType

    override val isDefinedField = true
    override def asDefinedField: DefinedField = this

    override def toString: String = {
        s"DefinedField(declaringClassType=${declaringClassType.toJava},definedField=${definedField.toJava})"
    }
}

object DefinedField {
    def unapply(definedField: DefinedField): Option[Field] = {
        Some(definedField.definedField)
    }
}

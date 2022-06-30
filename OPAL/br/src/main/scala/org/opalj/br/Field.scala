/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br

import scala.math.Ordered
import org.opalj.bi.ACC_TRANSIENT
import org.opalj.bi.ACC_PUBLIC
import org.opalj.bi.ACC_VOLATILE
import org.opalj.bi.AccessFlagsContexts
import org.opalj.bi.AccessFlags

import scala.collection.immutable.ArraySeq

/**
 * Represents a single field declaration/definition.
 *
 * @note   Fields, which are directly created, have no link to "their defining" [[ClassFile]].
 *         This link is implicitly established when a method is added to a [[ClassFile]]. This
 *         operation also updates the field object.
 *
 * @note   '''Identity (w.r.t. `equals`/`hashCode`) is intentionally by reference (default
 *         behavior).'''
 *
 * @author Michael Eichberg
 */
sealed abstract class JVMField extends ClassMember with Ordered[JVMField] {

    /**
     * This field's access flags. To analyze the access flags
     * bit vector use [[org.opalj.bi.AccessFlag]] or
     * [[org.opalj.bi.AccessFlagsIterator]] or use pattern matching.
     */
    def accessFlags: Int

    /**
     * The name of this field. The name is interned (see `String.intern()` for
     * details.)
     * Note, that this name is not required to be a valid Java programming
     * language identifier.
     */
    def name: String

    /** The (erased) type of this field. */
    def fieldType: FieldType

    /**
     * The defined attributes. The JVM 8 specification defines
     * the following attributes for fields:
     *  * [[ConstantValue]],
     *  * [[Synthetic]],
     *  * [[Signature]],
     *  * [[Deprecated]],
     *  * [[RuntimeVisibleAnnotationTable]],
     *  * [[RuntimeInvisibleAnnotationTable]],
     *  * [[RuntimeVisibleTypeAnnotationTable]] and
     *  * [[RuntimeInvisibleTypeAnnotationTable]].
     */
    def attributes: Attributes

    // This method is only to be called by ..br.ClassFile to associate this method
    // with the respective class file.
    private[br] def prepareClassFileAttachement(): Field = {
        new Field(
            null /*will be set by class file*/ ,
            accessFlags, name, fieldType, attributes
        )
    }

    /**
     * Compares this field with the given one for structural equality.
     *
     * Two fields are structurally equal if they have the same names, flags, type and attributes.
     * In the latter case, the order doesn't matter!
     */
    def similar(other: JVMField, config: SimilarityTestConfiguration): Boolean = {
        this.accessFlags == other.accessFlags &&
            (this.fieldType eq other.fieldType) &&
            this.name == other.name &&
            compareAttributes(other.attributes, config).isEmpty
    }

    def copy(
        accessFlags: Int        = this.accessFlags,
        name:        String     = this.name,
        fieldType:   FieldType  = this.fieldType,
        attributes:  Attributes = this.attributes
    ): FieldTemplate = {
        new FieldTemplate(accessFlags, name, fieldType, attributes)
    }

    final def asVirtualField(declaringClassFile: ClassFile): VirtualField = {
        asVirtualField(declaringClassFile.thisType)
    }

    def asVirtualField(declaringClassType: ObjectType): VirtualField = {
        VirtualField(declaringClassType, name, fieldType)
    }

    def isTransient: Boolean = (ACC_TRANSIENT.mask & accessFlags) != 0

    def isVolatile: Boolean = (ACC_VOLATILE.mask & accessFlags) != 0

    /**
     * Returns this field's type signature.
     */
    def fieldTypeSignature: Option[FieldTypeSignature] = {
        attributes collectFirst { case s: FieldTypeSignature => s }
    }

    /**
     * Returns this field's constant value.
     */
    def constantFieldValue: Option[ConstantFieldValue[_]] = {
        attributes collectFirst { case cv: ConstantFieldValue[_] => cv }
    }

    def signatureToJava(withAccessFlags: Boolean = false): String = {
        val javaSignature = fieldType.toJava+" "+name
        if (withAccessFlags) {
            val rawAccessFlags = AccessFlags.toStrings(this.accessFlags, AccessFlagsContexts.FIELD)
            val accessFlags =
                if (rawAccessFlags.nonEmpty) rawAccessFlags.mkString("", " ", " ") else ""
            accessFlags + javaSignature
        } else
            javaSignature

    }

    /**
     * Defines an absolute order on `Field` objects w.r.t. their names and types.
     * The order is defined by first lexicographically comparing the names of the
     * fields and – if the names are identical – by comparing the types.
     */
    def compare(other: JVMField): Int = {
        if (this.name eq other.name) {
            this.fieldType compare other.fieldType
        } else if (this.name < other.name) {
            -1
        } else {
            1
        }
    }

    //
    //
    // DEBUGGING PURPOSES
    //
    //

    override def toString(): String = {
        import AccessFlagsContexts.FIELD
        val jAccessFlags = AccessFlags.toStrings(accessFlags, FIELD).mkString(" ")
        val jDescriptor = fieldType.toJava+" "+name
        val field =
            if (jAccessFlags.nonEmpty) {
                jAccessFlags+" "+jDescriptor
            } else {
                jDescriptor
            }

        if (attributes.nonEmpty) {
            field + attributes.map(_.getClass().getSimpleName()).mkString("«", ", ", "»")
        } else {
            field
        }
    }
}

final class FieldTemplate private[br] (
        val accessFlags: Int,
        val name:        String, // the name is interned to enable reference comparisons!
        val fieldType:   FieldType,
        val attributes:  Attributes
) extends JVMField {

    final override def isField: Boolean = false

}

final class Field private[br] (
        private[br] var declaringClassFile: ClassFile, // the back-link can be updated to enable efficient load-time transformations
        val accessFlags:                    Int,
        val name:                           String, // the name is interned to enable reference comparisons!
        val fieldType:                      FieldType,
        val attributes:                     Attributes
) extends JVMField {

    // see ClassFile.unsafeReplaceMethod for THE usage!
    private[br] def detach(): this.type = { declaringClassFile = null; this }

    override def isField: Boolean = true

    override def asField: Field = this

    /**
     * This method's class file.
     */
    def classFile: ClassFile = declaringClassFile

    def toJava: String = s"${declaringClassFile.thisType.toJava}{ ${signatureToJava(true)} }"

    def toJava(message: String): String = {
        s"${declaringClassFile.thisType.toJava}{ ${signatureToJava(true)} $message }"
    }

    override def toString(): String = {
        super.toString()+" // in "+declaringClassFile.thisType.toJava
    }
}

/**
 * Defines factory and extractor methods for `Field` objects.
 */
object Field {

    def apply(
        accessFlags:           Int,
        name:                  String,
        fieldType:             FieldType,
        fieldAttributeBuilder: FieldAttributeBuilder
    ): FieldTemplate = {
        this(
            accessFlags, name, fieldType,
            ArraySeq(fieldAttributeBuilder(accessFlags, name, fieldType))
        )
    }

    def apply(
        accessFlags: Int        = ACC_PUBLIC.mask,
        name:        String,
        fieldType:   FieldType,
        attributes:  Attributes = ArraySeq.empty
    ): FieldTemplate = {
        new FieldTemplate(accessFlags, name.intern(), fieldType, attributes)
    }

    // Only to be called by the class file reader!
    protected[br] def unattached(
        accessFlags: Int        = ACC_PUBLIC.mask,
        name:        String,
        fieldType:   FieldType,
        attributes:  Attributes = ArraySeq.empty
    ): Field = {
        new Field(null, accessFlags, name.intern(), fieldType, attributes)
    }

    def unapply(field: Field): Option[(Int, String, FieldType)] = {
        Some((field.accessFlags, field.name, field.fieldType))
    }
}

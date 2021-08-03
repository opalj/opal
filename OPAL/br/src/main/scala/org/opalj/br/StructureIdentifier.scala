/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br

/**
 * Uniquely identifies a specific element that can (by definition) only exist
 * once in a project. For example, in the context of OPAL a type declaration
 * is unique or the combination of a type declaration and a field name or
 * the combination of a type declaration, method name and method descriptor.
 *
 * @author Michael Eichberg
 */
trait StructureIdentifier {

    /**
     * Returns a compact, human readable representation of this structure element.
     * This representation is not guaranteed to return a unique representation.
     * However, it should be precise enough to enable developers (with some
     * additional context information) to precisely identify the structure element.
     */
    def toHRR: String

    /**
     * Returns the name of the package in which this structure element is defined. If
     * this element (e.g., a primitive type) does not belong to a specific
     * package or the concept of a package name does not apply None is returned.
     * In case of the default package, the empty string is returned.
     */
    def declaringPackage: Option[String]
}

case class TypeIdentifier(t: Type) extends StructureIdentifier {

    def toHRR: String = t.toJava

    def declaringPackage: Option[String] =
        t match {
            case o: ObjectType => Some(o.packageName);
            case _             => None
        }
}

case class MethodIdentifier(
        declaringReferenceType: ReferenceType,
        methodName:             String,
        methodDescriptor:       MethodDescriptor
)
    extends StructureIdentifier {

    def toHRR: String =
        declaringReferenceType.toJava+"."+methodName+""+methodDescriptor.toUMLNotation

    def declaringPackage: Option[String] =
        declaringReferenceType match {
            case o: ObjectType            => Some(o.packageName);
            case ArrayType(o: ObjectType) => Some(o.packageName);
            case _: ArrayType             => Some("java/lang"); // handles Arrays of primitives
            case _                        => None
        }
}

case class FieldIdentifier(
        declaringObjectType: ObjectType,
        fieldName:           String
)
    extends StructureIdentifier {

    def toHRR: String = declaringObjectType.toJava+"."+fieldName

    def declaringPackage = Some(declaringObjectType.packageName)
}

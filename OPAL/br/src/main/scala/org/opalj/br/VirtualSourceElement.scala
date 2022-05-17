/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br

import scala.math.Ordered

/**
 * A `VirtualSourceElement` is the representation of some source element that may be
 * detached from the concrete source element that represents the implementation; that is,
 * the virtual source element may not have a reference to the concrete element.
 *
 * @author Michael Eichberg
 * @author Marco Torsello
 */
sealed abstract class VirtualSourceElement
    extends SourceElement
    with Ordered[VirtualSourceElement] {

    override def attributes: Attributes = NoAttributes

    final override def isVirtual = true

    /**
     * The "natural order" is VirtualClasses < VirtualFields < VirtualMethods.
     */
    override def compare(that: VirtualSourceElement): Int

    /**
     * Returns the declared/declaring class type of this `VirtualSourceElement`.
     * If this `VirtualSourceElement` is a [[VirtualClass]], the returned type is the
     * declared class else it is the declaring class.
     */
    def classType: ReferenceType

    def toJava: String

    /**
     * Returns the best line number information available.
     */
    def getLineNumber(project: ClassFileRepository): Option[Int]

}

/**
 * Defines common helper functions related to [[VirtualSourceElement]]s.
 */
object VirtualSourceElement {

    def asVirtualSourceElements(
        classFiles:     Iterable[ClassFile],
        includeMethods: Boolean             = true,
        includeFields:  Boolean             = true
    ): Set[VirtualSourceElement] = {
        var sourceElements: Set[VirtualSourceElement] = Set.empty

        classFiles foreach { classFile =>
            val classType = classFile.thisType
            sourceElements += classFile.asVirtualClass
            if (includeMethods)
                classFile.methods.foreach(sourceElements += _.asVirtualMethod(classType))
            if (includeFields)
                classFile.fields.foreach(sourceElements += _.asVirtualField(classType))
        }
        sourceElements
    }

}

/**
 * Represents a class for which we have found some references but have not analyzed
 * any class file or do not want to keep the reference to the underlying class file.
 *
 * @author Michael Eichberg
 */
final case class VirtualClass(thisType: ObjectType) extends VirtualSourceElement {

    override def isClass: Boolean = true

    override def classType: ObjectType = thisType

    override def toJava: String = thisType.toJava

    // Recall that the class may not be the only one defined in a source file!
    override def getLineNumber(project: ClassFileRepository): Option[Int] = None

    override def compare(that: VirtualSourceElement): Int = {
        //x < 0 when this < that; x == 0 when this == that; x > 0 when this > that
        that match {
            case VirtualClass(thatType) => thisType.compare(thatType)
            case _                      => -1
        }
    }

    override def hashCode: Int = thisType.id

    /**
     * Two objects of type `VirtualClass` are considered equal if they represent
     * the same type.
     */
    override def equals(other: Any): Boolean = {
        other match {
            case that: VirtualClass => this.thisType eq that.thisType
            case _                  => false
        }
    }
}

/**
 * @author Michael Eichberg
 */
sealed abstract class VirtualClassMember extends VirtualSourceElement

/**
 * Represents a field of a virtual class.
 *
 * @author Michael Eichberg
 */
final case class VirtualField(
        declaringClassType: ObjectType,
        name:               String,
        fieldType:          FieldType
) extends VirtualClassMember {

    override def isField: Boolean = true

    override def classType: ObjectType = declaringClassType

    override def toJava: String = declaringClassType.toJava+"{ "+fieldType.toJava+" "+name+"; }"

    override def getLineNumber(project: ClassFileRepository): Option[Int] = None

    override def compare(that: VirtualSourceElement): Int = {
        // x < 0 when this < that; x == 0 when this == that; x > 0 when this > that
        that match {
            case _: VirtualClass =>
                1
            case that: VirtualField =>
                if (this.declaringClassType eq that.declaringClassType) {
                    this.name.compareTo(that.name) match {
                        case 0 => this.fieldType.compare(that.fieldType)
                        case x => x
                    }
                } else {
                    if (this.declaringClassType.id < that.declaringClassType.id)
                        -1
                    else
                        1
                }
            case _ /*VirtualMethod*/ =>
                -1
        }
    }

    override def hashCode: Int = {
        (((declaringClassType.id * 41) + name.hashCode()) * 41) + fieldType.id
    }

    override def equals(other: Any): Boolean = {
        other match {
            case that: VirtualField =>
                (this.declaringClassType eq that.declaringClassType) &&
                    (this.fieldType eq that.fieldType) &&
                    this.name == that.name
            case _ => false
        }
    }
}

/**
 * Represents a method of a virtual class.
 *
 * @author Michael Eichberg
 */
sealed class VirtualMethod(
        val declaringClassType: ReferenceType,
        val name:               String,
        val descriptor:         MethodDescriptor
) extends VirtualClassMember {

    override def isMethod: Boolean = true

    override def classType: ReferenceType = declaringClassType

    override def toJava: String = declaringClassType.toJava+"{ "+descriptor.toJava(name)+"; }"

    override def getLineNumber(project: ClassFileRepository): Option[Int] = {
        if (declaringClassType.isArrayType)
            return None;

        project.classFile(declaringClassType.asObjectType).flatMap { cf =>
            cf.findMethod(name, descriptor).flatMap(m => m.body.flatMap(b => b.firstLineNumber))
        }
    }

    override def compare(that: VirtualSourceElement): Int = {
        // x < 0 when this < that; x == 0 when this == that; x > 0 when this > that
        that match {
            case that: VirtualMethod =>
                if (this.declaringClassType eq that.declaringClassType) {
                    this.name.compareTo(that.name) match {
                        case 0 => this.descriptor.compare(that.descriptor)
                        case x => x
                    }
                } else {
                    if (this.declaringClassType.id < that.declaringClassType.id)
                        -1
                    else
                        1
                }
            case _ =>
                1
        }
    }

    override def hashCode: Int = {
        (((declaringClassType.id * 41) + name.hashCode()) * 41) + descriptor.hashCode()
    }

    override def equals(other: Any): Boolean = {
        other match {
            case that: VirtualMethod =>
                (this.declaringClassType eq that.declaringClassType) &&
                    this.descriptor == that.descriptor &&
                    this.name == that.name
            case _ => false
        }
    }

    override def toString: String = {
        s"VirtualMethod($declaringClassType,$name,$descriptor)"
    }
}

object VirtualMethod {

    def apply(
        declaringClassType: ReferenceType,
        name:               String,
        descriptor:         MethodDescriptor
    ): VirtualMethod = {
        new VirtualMethod(declaringClassType, name, descriptor)
    }

    def unapply(virtualMethod: VirtualMethod): Option[(ReferenceType, String, MethodDescriptor)] = {
        Some((
            virtualMethod.declaringClassType,
            virtualMethod.name,
            virtualMethod.descriptor
        ))
    }
}

final case class VirtualForwardingMethod(
        override val declaringClassType: ReferenceType,
        override val name:               String,
        override val descriptor:         MethodDescriptor,
        target:                          Method
) extends VirtualMethod(declaringClassType, name, descriptor) {

    override def toJava: String = declaringClassType.toJava+"{ "+descriptor.toJava(name)+" }"

    override def hashCode: Opcode = (target.hashCode() * 41) + super.hashCode

    override def equals(other: Any): Boolean = other match {
        case that: VirtualForwardingMethod =>
            (this.target eq that.target) &&
                super.equals(other)
        case _ => false
    }

}


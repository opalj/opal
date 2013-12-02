/* License (BSD Style License):
 * Copyright (c) 2009 - 2013
 * Software Technology Group
 * Department of Computer Science
 * Technische Universität Darmstadt
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  - Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *  - Neither the name of the Software Technology Group or Technische
 *    Universität Darmstadt nor the names of its contributors may be used to
 *    endorse or promote products derived from this software without specific
 *    prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) 
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package de.tud.cs.st
package bat
package resolved

import scala.annotation.tailrec

/**
 * Represents a single class file.
 *
 * @param minorVersion The minor part of this class file's version number.
 * @param majorVersion The major part of this class file's version number.
 * @param accessFlags The access flags of this class. To further analyze the access flags
 *  either use the corresponding convenience methods (e.g., isEnumDeclaration())
 *  or the class [[de.tud.cs.st.bat.AccessFlagsIterator]] or the classes which
 *  inherit from [[de.tud.cs.st.bat.AccessFlag]].
 * @param thisClass The type implemented by this class file.
 * @param superClass The class type from which this class inherits. `None` if this
 *      class file represents `java.lang.Object`.
 * @param interfaces The set of implemented interfaces. May be empty.
 * @param fields The declared fields. May be empty. The list is sorted by name.
 * @param methods The declared methods. May be empty. The list is sorted by name and
 *      number of descriptors.
 * @param attributes This class file's reified attributes. Which attributes
 *    are reified depends on the configuration of the class file reader; e.g.,
 *    [[de.tud.cs.st.bat.resolved.reader.Java7Framework]].
 *    The JVM specification defines the following attributes:
 *    - ''InnerClasses''
 *    - ''EnclosingMethod''
 *    - ''Synthetic''
 *    - ''Signature''
 *    - ''SourceFile''
 *    - ''SourceDebugExtension''
 *    - ''Deprecated''
 *    - ''RuntimeVisibleAnnotations''
 *    - ''RuntimeInvisibleAnnotations''
 *
 *    The ''BootstrapMethods'' attribute, which is also defined by the JVM specification,
 *    is, however, resolved and is not part of the attributes table of the class file.
 *    The ''BootstrapMethods'' attribute is basically the container for the bootstrap
 *    methods referred to by the [[de.tud.cs.st.bat.resolved.instructions.INVOKEDYNAMIC]]
 *    instructions.
 *
 * @note Equality of `ClassFile` objects is reference based and a class file's hash code
 *    is the same as `thisType`'s hash code.
 *
 * @author Michael Eichberg
 */
final class ClassFile private (
    val minorVersion: Int,
    val majorVersion: Int,
    val accessFlags: Int,
    val thisClass: ObjectType, // TODO [ClassFile] Rename "thisClass" to,e.g., thisType
    val superClass: Option[ObjectType], // TODO [ClassFile] Rename superClass to superclassType
    val interfaces: Seq[ObjectType], // TODO [ClassFile] Rename interfaces to interfaceTypes
    val fields: Fields,
    val methods: Methods,
    val attributes: Attributes)
        extends CommonAttributes
        with SourceElement {

    import ClassFile._

    override def isClassFile = true

    override def asClassFile = this

    def className: String = thisClass.className

    def isAbstract: Boolean = (ACC_ABSTRACT.mask & accessFlags) != 0

    def isFinal: Boolean = (ACC_FINAL.mask & accessFlags) != 0

    def isPublic: Boolean = (ACC_PUBLIC.mask & accessFlags) != 0

    def isClassDeclaration: Boolean = (accessFlags & classCategoryMask) == 0

    def isEnumDeclaration: Boolean = (accessFlags & classCategoryMask) == ACC_ENUM.mask

    def isInterfaceDeclaration: Boolean = (accessFlags & classCategoryMask) == ACC_INTERFACE.mask

    def isAnnotationDeclaration: Boolean = (accessFlags & classCategoryMask) == annotationMask

    def isInnerClass: Boolean = innerClasses.exists(_.exists(_.innerClassType == thisClass))

    def enclosingMethod: Option[EnclosingMethod] =
        attributes collectFirst { case em: EnclosingMethod ⇒ em }

    def innerClasses: Option[InnerClasses] =
        attributes collectFirst { case InnerClassTable(ice) ⇒ ice }

    /**
     * Each class has at most one explicit, direct outer type.
     *
     * @return The object type of the outer type as well as the access flags of this
     *      inner class.
     */
    def outerType: Option[(ObjectType, Int)] =
        innerClasses flatMap { innerClasses ⇒
            innerClasses collectFirst {
                case InnerClass(`thisClass`, Some(outerType), _, accessFlags) ⇒
                    (outerType, accessFlags)
            }
        }

    /**
     * Each class file optionally defines a class signature.
     */
    def classSignature: Option[ClassSignature] =
        attributes collectFirst { case s: ClassSignature ⇒ s }

    /**
     * The SourceFile attribute is an optional attribute [...]. There can be
     * at most one `SourceFile` attribute.
     */
    def sourceFile: Option[String] =
        attributes collectFirst { case SourceFile(s) ⇒ s }

    def sourceDebugExtension: Option[String] =
        attributes collectFirst { case SourceDebugExtension(s) ⇒ s }

    /**
     * All constructors/instance initialization methods defined by this class.
     *
     * (This does not include static initializers.)
     */
    def constructors: Seq[Method] = methods.view filter { _.name == "<init>" }

    /**
     * The static initializer of this class.
     *
     * @note The way how the static initializer is identified has changed
     *       with Java 7. In a class file whose version number is 51.0 or above, the
     *       method must have its ACC_STATIC flag set. Other methods named &lt;clinit&gt;
     *       in a class file are of no consequence.
     */
    def staticInitializer: Option[Method] = {
        // The set of methods is sorted - hence, the static initializer should
        // be (among) the first method(s).
        val methodsCount = methods.size
        val noArgsAndReturnVoidDescriptor = MethodDescriptor.NoArgsAndReturnVoid
        var i = 0
        while (i < methodsCount) {
            val method = methods(i)
            if (method.name == "<clinit>" &&
                method.descriptor == noArgsAndReturnVoidDescriptor &&
                (majorVersion < 51 || method.isStatic))
                return Some(method)
            else if (method.name > "<clinit>")
                return None
            i += 1
        }
        None
        // OLD - NOT MAKE USE OF THE FACT THAT THE METHODS ARE SORTED:
        //        methods find { method ⇒
        //            method.descriptor == MethodDescriptor.NoArgsAndReturnVoid &&
        //                method.name == "<clinit>" &&
        //                (majorVersion < 51 || method.isStatic)
        //        }
    }

    def findMethod(name: String, descriptor: MethodDescriptor): Option[Method] = {

        val descriptorParametersCount = descriptor.parametersCount

        @tailrec @inline def findMethod(low: Int, high: Int): Option[Method] = {
            if (high < low)
                return None

            val mid = (low + high) / 2 // <= will never overflow...(there are at most 65535 methods)
            val method = methods(mid)
            val methodName = method.name
            if (methodName == name) {
                val methodDescriptor = method.descriptor
                val methodParametersCount = methodDescriptor.parametersCount
                if (methodParametersCount < descriptorParametersCount)
                    findMethod(mid + 1, high)
                else if (methodParametersCount > descriptorParametersCount)
                    findMethod(low, mid - 1)
                else {
                    // the number of parameters is identical!
                    if (methodDescriptor == descriptor)
                        return Some(method)
                    else {
                        // the number of arguments and the name fit...
                        // we now perform a local search
                        {
                            var p = mid - 1
                            while (p >= low) {
                                val method = methods(p)
                                if (method.descriptor.parametersCount != descriptorParametersCount)
                                    p = -1 // break...
                                else if (method.name == name) {
                                    if (method.descriptor == descriptor)
                                        return Some(method)
                                    //else continue the search    
                                } else {
                                    p = -1 // break...
                                }

                                p -= 1
                            }
                        }
                        {
                            var s = mid + 1
                            while (s <= high) {
                                val method = methods(s)
                                if (method.name == name) {
                                    if (method.descriptor == descriptor)
                                        return Some(method)

                                    if (method.descriptor.parametersCount != descriptorParametersCount)
                                        return None

                                    //else continue the search
                                } else
                                    return None
                                s += 1
                            }
                        }
                        None
                    }
                }
            } else if (method.name < name) {
                findMethod(mid + 1, high)
            } else {
                findMethod(low, mid - 1)
            }
        }

        findMethod(0, methods.size - 1)
    }

    override def hashCode: Int = thisClass.id

    override def equals(other: Any): Boolean =
        other match {
            case that: ClassFile ⇒ that eq this
            case _               ⇒ false
        }

}
/**
 * A collection of constants related to class files.
 *
 * @author Michael Eichberg
 */
object ClassFile {

    val classCategoryMask: Int = ACC_INTERFACE.mask | ACC_ANNOTATION.mask | ACC_ENUM.mask

    val annotationMask: Int = ACC_INTERFACE.mask | ACC_ANNOTATION.mask

    def apply(
        minorVersion: Int,
        majorVersion: Int,
        accessFlags: Int,
        thisClass: ObjectType, // TODO [ClassFile] Rename "thisClass" to,e.g., thisType
        superClass: Option[ObjectType], // TODO [ClassFile] Rename superClass to superclassType
        interfaces: Seq[ObjectType], // TODO [ClassFile] Rename interfaces to interfaceTypes
        fields: Fields,
        methods: Methods,
        attributes: Attributes): ClassFile = {
        new ClassFile(
            minorVersion, majorVersion,
            accessFlags,
            thisClass, superClass, interfaces,
            fields, methods, attributes)
    }

    def unapply(classFile: ClassFile): Option[(Int, ObjectType, Option[ObjectType], Seq[ObjectType])] = {
        import classFile._
        Some((accessFlags, thisClass, superClass, interfaces))
    }
}
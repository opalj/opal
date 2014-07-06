/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2014
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
package org.opalj
package br

import scala.annotation.tailrec

import bi.ACC_ABSTRACT
import bi.ACC_ANNOTATION
import bi.ACC_INTERFACE
import bi.ACC_ENUM
import bi.ACC_FINAL
import bi.ACC_PUBLIC
import bi.AccessFlagsContexts
import bi.AccessFlags

/**
 * Represents a single class file which either defines a class type or an interface type.
 *
 * @param minorVersion The minor part of this class file's version number.
 * @param majorVersion The major part of this class file's version number.
 * @param accessFlags The access flags of this class. To further analyze the access flags
 *  either use the corresponding convenience methods (e.g., isEnumDeclaration())
 *  or the class [[org.opalj.bi.AccessFlagsIterator]] or the classes which
 *  inherit from [[org.opalj.bi.AccessFlag]].
 * @param thisType The type implemented by this class file.
 * @param superclassType The class type from which this class inherits. `None` if this
 *      class file defines `java.lang.Object`.
 * @param interfaceTypes The set of implemented interfaces. May be empty.
 * @param fields The declared fields. May be empty. The list is sorted by name.
 * @param methods The declared methods. May be empty. The list is first sorted by name,
 *      and then by method descriptor.
 * @param attributes This class file's reified attributes. Which attributes
 *    are reified depends on the configuration of the class file reader; e.g.,
 *    [[org.opalj.br.reader.Java7Framework]].
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
 *    methods referred to by the [[org.opalj.br.instructions.INVOKEDYNAMIC]]
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
    val thisType: ObjectType,
    val superclassType: Option[ObjectType],
    val interfaceTypes: Seq[ObjectType],
    val fields: Fields,
    val methods: Methods,
    val attributes: Attributes)
        extends SourceElement
        with CommonSourceElementAttributes {

    import ClassFile._

    final override def isClass = true

    final override def asClassFile = this

    def asVirtualClass: VirtualClass = VirtualClass(thisType)

    def id = thisType.id

    /**
     * The fully qualified name of the type defined by this class file.
     */
    def fqn: String = thisType.fqn

    def isAbstract: Boolean = (ACC_ABSTRACT.mask & accessFlags) != 0

    def isFinal: Boolean = (ACC_FINAL.mask & accessFlags) != 0

    def isPublic: Boolean = (ACC_PUBLIC.mask & accessFlags) != 0

    def isClassDeclaration: Boolean = (accessFlags & classCategoryMask) == 0

    def isEnumDeclaration: Boolean = (accessFlags & classCategoryMask) == ACC_ENUM.mask

    def isInterfaceDeclaration: Boolean = (accessFlags & classCategoryMask) == ACC_INTERFACE.mask

    def isAnnotationDeclaration: Boolean = (accessFlags & classCategoryMask) == annotationMask

    def isInnerClass: Boolean = innerClasses.exists(_.exists(_.innerClassType == thisType))

    /**
     * Returns `true` if this class file has no direct representation in the source code.
     *
     * @see [[VirtualTypeFlag]] for further information.
     */
    def isVirtualType: Boolean = attributes.contains(VirtualTypeFlag)

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
                case InnerClass(`thisType`, Some(outerType), _, accessFlags) ⇒
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

    /**
     * The SourceDebugExtension attribute is an optional attribute [...]. There can be
     * at most one `SourceDebugExtension` attribute. The data (which is modified UTF8
     * String may, however, not be representable using a String object (see the
     * spec. for further details.)
     * 
     * The returned Array must not be mutated.
     */
    def sourceDebugExtension: Option[Array[Byte]] =
        attributes collectFirst { case SourceDebugExtension(s) ⇒ s }

    /**
     * All constructors/instance initialization methods defined by this class.
     *
     * (This does not include static initializers.)
     */
    def constructors: Seq[Method] = methods.view filter { _.name == "<init>" }

    /**
     * The set of all instance methods. I.e., the set of methods that are not static,
     * constructors, or static initializers.
     */
    def instanceMethods: Iterable[Method] =
        methods.view filterNot { method ⇒
            method.isStatic || method.isConstructor || method.isStaticInitializer
        }

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
            val methodName = method.name
            if (methodName == "<clinit>" &&
                method.descriptor == noArgsAndReturnVoidDescriptor &&
                (majorVersion < 51 || method.isStatic))
                return Some(method)
            else if (methodName > "<clinit>")
                return None
            i += 1
        }
        None
        // OLD - DID NOT MAKE USE OF THE FACT THAT THE METHODS ARE SORTED:
        // methods find { method ⇒
        //  method.descriptor == MethodDescriptor.NoArgsAndReturnVoid &&
        //  method.name == "<clinit>" &&
        //  (majorVersion < 51 || method.isStatic)
        // }
    }

    /**
     * Returns the field with the given name, if any.
     *
     * @note The complexity is O(log2 n); this algorithm uses a binary search algorithm.
     */
    def findField(name: String): Option[Field] = {
        @tailrec @inline def findField(low: Int, high: Int): Option[Field] = {
            if (high < low)
                return None

            val mid = (low + high) / 2 // <= will never overflow...(there are at most 65535 fields)
            val field = fields(mid)
            val fieldName = field.name
            if (fieldName == name) {
                Some(field)
            } else if (fieldName.compareTo(name) < 0) {
                findField(mid + 1, high)
            } else {
                findField(low, mid - 1)
            }
        }

        findField(0, fields.size - 1)
    }

    /**
     * Returns the method with the given name, if any.
     *
     * @note Though the methods are sorted, no guarantee is given which method is
     *      returned if multiple methods are defined with the same name.
     * @note The complexity is O(log2 n); this algorithm uses a binary search algorithm.
     */
    def findMethod(name: String): Option[Method] = {
        @tailrec @inline def findMethod(low: Int, high: Int): Option[Method] = {
            if (high < low)
                return None

            val mid = (low + high) / 2 // <= will never overflow...(there are at most 65535 methods)
            val method = methods(mid)
            val methodName = method.name
            if (methodName == name) {
                Some(method)
            } else if (methodName.compareTo(name) < 0) {
                findMethod(mid + 1, high)
            } else {
                findMethod(low, mid - 1)
            }
        }

        findMethod(0, methods.size - 1)
    }

    /**
     * Returns the method with the given name and descriptor that is declared by
     * this class file.
     *
     * @note The complexity is O(log2 n); this algorithm uses a binary search algorithm.
     */
    def findMethod(name: String, descriptor: MethodDescriptor): Option[Method] = {

        @tailrec @inline def findMethod(low: Int, high: Int): Option[Method] = {
            if (high < low)
                return None

            val mid = (low + high) / 2 // <= will never overflow...(there are at most 65535 methods)
            val method = methods(mid)
            val methodName = method.name
            if (methodName == name) {
                val methodDescriptor = method.descriptor
                if (methodDescriptor < descriptor)
                    findMethod(mid + 1, high)
                else if (descriptor == methodDescriptor)
                    Some(method)
                else
                    findMethod(low, mid - 1)
            } else if (methodName.compareTo(name) < 0) {
                findMethod(mid + 1, high)
            } else {
                findMethod(low, mid - 1)
            }
        }

        findMethod(0, methods.size - 1)
    }

    /**
     * This class file's `hasCode`. The `hashCode` is (by purpose) identical to
     * the id of the `ObjectType` it implements.
     */
    override def hashCode: Int = thisType.id

    override def equals(other: Any): Boolean =
        other match {
            case that: ClassFile ⇒ that eq this
            case _               ⇒ false
        }

    override def toString: String = {
        "ClassFile(\n\t"+
            AccessFlags.toStrings(accessFlags, AccessFlagsContexts.CLASS).mkString("", " ", " ") +
            thisType.toJava+"\n"+
            superclassType.map("\textends "+_.toJava+"\n").getOrElse("") +
            (if (interfaceTypes.nonEmpty) interfaceTypes.mkString("\t\twith ", " with ", "\n") else "") +
            annotationsToJava(runtimeVisibleAnnotations, "\t", "\n") +
            annotationsToJava(runtimeInvisibleAnnotations, "\t", "\n")+
            "\t{version="+majorVersion+"."+minorVersion+"}\n)"
    }

    protected[br] def updateAttributes(newAttributes: Attributes): ClassFile = {
        new ClassFile(
            this.minorVersion, this.majorVersion, this.accessFlags,
            this.thisType, this.superclassType, this.interfaceTypes,
            this.fields, this.methods, newAttributes
        )
    }
}
/**
 * Defines factory and extractor methods for `ClassFile` objects as well as related
 * constants.
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
        thisType: ObjectType,
        superclassType: Option[ObjectType],
        interfaceTypes: Seq[ObjectType],
        fields: Fields,
        methods: Methods,
        attributes: Attributes): ClassFile = {
        new ClassFile(
            minorVersion, majorVersion,
            accessFlags,
            thisType, superclassType, interfaceTypes,
            fields sortWith { (f1, f2) ⇒ f1 < f2 },
            methods sortWith { (m1, m2) ⇒ m1 < m2 },
            attributes)
    }

    /**
     * Creates a class that acts as a proxy for the specified class and that implements
     * a single method that calls the specified method.
     *
     * I.e., a class is generated using the following template:
     * {{{
     * class <definingType.objectType>
     *  extends <definingType.theSuperclassType>
     *  implements <definingType.theSuperinterfaceTypes> {
     *
     *  private <calleeType> receiver;
     *
     *  public "<init>"( <calleeType> receiver) { // the constructor
     *      this.receiver = receiver;
     *  }
     *
     *  public <methodDescriptor.returnType> <methodName> <methodDescriptor.paramterTypes>{
     *     return/*<= if the return type is not void*/ this.receiver.<calleMethodName>(<parameters>)
     *  }
     *  }
     * }}}
     * The class, the constructor and the method will be public. The field which holds
     * the receiver object is private.
     *
     * If the receiver method is static, an empty '''default constructor''' is created
     * and no field is generated.
     *
     * The synthetic access flag is always set, as well as the [[VirtualTypeFlag]]
     * attribute.
     *
     * The used class file version is 49.0 (Java 5) (Using this version, we are not
     * required to create the stack map table attribute to create a valid class file.)
     */
    //    def Proxy(
    //        definingType: TypeDeclaration,
    //        methodName: String,
    //        methodDescriptor: MethodDescriptor,
    //        calleeType: ObjectType,
    //        calleeMethodName: String,
    //        calleeMethodDescriptor: String): ClassFile = {
    //
    //        val field: Option[Field] = None
    //        val method: Method = null
    //        ClassFile(0, 49,
    //            bi.ACC_SYNTHETIC.mask | bi.ACC_PUBLIC.mask | bi.ACC_SUPER.mask,
    //            definingType.objectType,
    //            definingType.theSuperclassType,
    //            definingType.theSuperinterfaceTypes.toSeq,
    //            field.map(IndexedSeq(_)).getOrElse(IndexedSeq.empty),
    //            IndexedSeq(method),
    //            IndexedSeq(VirtualTypeFlag))
    //    }

    def unapply(classFile: ClassFile): Option[(Int, ObjectType, Option[ObjectType], Seq[ObjectType])] = {
        import classFile._
        Some((accessFlags, thisType, superclassType, interfaceTypes))
    }
}
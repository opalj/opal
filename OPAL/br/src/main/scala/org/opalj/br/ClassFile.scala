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
import scala.util.control.ControlThrowable

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

    /**
     * Returns the `inner classes attribute`, if defined.
     *
     * @note The inner classes attribute contains (for inner classes) also a reference
     *      to its outer class. Furthermore, it contains references to other inner
     *      classes that are not an inner class of this class.
     *      If you are just interested in the inner classes
     *      of this class, use the method nested classes.
     * @see [[nestedClasses]]
     */
    def innerClasses: Option[InnerClasses] =
        attributes collectFirst { case InnerClassTable(ice) ⇒ ice }

    /**
     * Returns the set of all immediate nested classes of this class. I.e., returns those
     * nested classes that are not defined in the scope of a nested class of this
     * class.
     */
    def nestedClasses(classFileRepository: ClassFileRepository): Seq[ObjectType] = {
        // From the Java __8__ specification:
        // - every inner class must have an inner class attribute (at least for itself)
        // - every class that has inner classes must have an innerclasses attribute
        //   and the inner classes array must contain an entry
        // - the InnerClasses attribute only encodes information about its immediate
        //   inner classes
        var outerClassType: Option[ObjectType] = enclosingMethod.map(_.clazz)
        var isInnerType = false

        def isThisType(innerClass: InnerClass): Boolean = {
            if (innerClass.innerClassType eq thisType) {
                if (innerClass.outerClassType.isDefined)
                    outerClassType = innerClass.outerClassType
                isInnerType = true
                true
            } else
                false
        }

        val nestedClassesCandidates = innerClasses.map { innerClasses ⇒
            innerClasses.filter(innerClass ⇒
                // it does not describe this class:
                (!isThisType(innerClass)) &&
                    // it does not give information about an outer class:     
                    (!this.fqn.startsWith(innerClass.innerClassType.fqn)) &&
                    // it does not give information about some other inner class of this type:
                    (
                        innerClass.outerClassType.isEmpty ||
                        (innerClass.outerClassType.get eq thisType)
                    )
            ).map(_.innerClassType)
        }.getOrElse {
            Nil
        }

        // THE FOLLOWING CODE IS NECESSARY TO COPE WITH BYTECODE GENERATED
        // BY OLD JAVA COMPILERS (IN PARTICULAR JAVA 1.1); 
        // IT BASICALLY TRIES TO RECREATE THE INNER-OUTERCLASSES STRUCTURE 
        if (isInnerType && outerClassType.isEmpty) {
            // let's try to find the outer class that refers to this class
            val thisFQN = thisType.fqn
            val innerTypeNameStartIndex = thisFQN.indexOf('$')
            if (innerTypeNameStartIndex == -1) {
                println(
                    Console.YELLOW+"[warn] the inner class "+thisType.toJava+
                        " does not use the standard naming schema"+
                        "; the inner classes information may be incomplete"+
                        Console.RESET
                )
                return nestedClassesCandidates.filter(_.fqn.startsWith(this.fqn))
            }
            val outerFQN = thisFQN.substring(0, innerTypeNameStartIndex)
            classFileRepository.classFile(ObjectType(outerFQN)) match {
                case Some(outerClass) ⇒

                    def directNestedClasses(objectTypes: Iterable[ObjectType]): Set[ObjectType] = {
                        var nestedTypes: Set[ObjectType] = Set.empty
                        objectTypes.foreach { objectType ⇒
                            classFileRepository.classFile(objectType) match {
                                case Some(classFile) ⇒
                                    nestedTypes ++= classFile.nestedClasses(classFileRepository)
                                case None ⇒
                                    println(
                                        Console.YELLOW+"[warn] project information incomplete; "+
                                            "cannot get informaton about "+objectType.toJava+
                                            "; the inner classes information may be incomplete"+
                                            Console.RESET
                                    )
                            }
                        }
                        nestedTypes
                    }

                    // let's filter those classes that are known innerclasses of this type's
                    // (indirect) outertype (they cannot be innerclasses of this class..)
                    var nestedClassesOfOuterClass = outerClass.nestedClasses(classFileRepository)
                    while (!nestedClassesOfOuterClass.contains(thisType) &&
                        !nestedClassesOfOuterClass.exists(nestedClassesCandidates.contains(_))) {
                        // We are still lacking sufficient information to make a decision 
                        // which class is a nested class of which other class
                        // e.g. we might have the following situation:
                        // class X { 
                        //  class Y {                                // X$Y
                        //      void m(){ 
                        //          new Listener(){                  // X$Listener$1
                        //              void event(){ 
                        //                  new Listener(){...}}}}}} // X$Listener$2
                        nestedClassesOfOuterClass = directNestedClasses(nestedClassesOfOuterClass).toSeq
                    }
                    val filteredNestedClasses = nestedClassesCandidates.filterNot(nestedClassesOfOuterClass.contains(_))
                    return filteredNestedClasses
                case None ⇒
                    println(
                        Console.YELLOW+"[warn] project information incomplete; "+
                            "cannot identify outer type of "+thisType.toJava+
                            "; the inner classes information may be incomplete"+
                            Console.RESET
                    )
                    return nestedClassesCandidates.filter(_.fqn.startsWith(this.fqn))
            }

        }

        nestedClassesCandidates
    }

    /**
     * Iterates over '''all ''direct'' and ''indirect'' nested classes''' of this class file.
     *
     * @example To collect all nested types:
     * {{{
     *   var allNestedTypes: Set[ObjectType] = Set.empty
     *   foreachNestedClasses(innerclassesProject, { nc ⇒ allNestedTypes += nc.thisType })
     * }}}
     */
    def foreachNestedClass(
        classFileRepository: ClassFileRepository,
        f: (ClassFile) ⇒ Unit): Unit = {
        nestedClasses(classFileRepository).foreach { nestedType ⇒
            classFileRepository.classFile(nestedType).map { nestedClassFile ⇒
                f(nestedClassFile)
                nestedClassFile.foreachNestedClass(classFileRepository, f)
            }
        }
    }

    /**
     * Each class has at most one explicit, direct outer type. Note that a local
     * class (a class defined in the scope of a method) or an anonymous class
     * do not specify an outer type.
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
    def constructors: Iterator[Method] = {
        new Iterator[Method] {
            var i = -1

            private def lookupNextConstructor() {
                i += 1
                if (i >= methods.size)
                    i = -1
                else {
                    val methodName = methods(i).name
                    if (methodName < "<init>")
                        lookupNextConstructor()
                    else if (methodName > "<init>")
                        i = -1;
                }
            }

            lookupNextConstructor()

            def hasNext: Boolean = i >= 0
            def next: Method = {
                val m = methods(i)
                lookupNextConstructor
                m
            }
        }
    }

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
        try {
            "ClassFile(\n\t"+
                AccessFlags.toStrings(accessFlags, AccessFlagsContexts.CLASS).mkString("", " ", " ") +
                thisType.toJava+"\n"+
                superclassType.map("\textends "+_.toJava+"\n").getOrElse("") +
                (if (interfaceTypes.nonEmpty) interfaceTypes.mkString("\t\twith ", " with ", "\n") else "") +
                annotationsToJava(runtimeVisibleAnnotations, "\t", "\n") +
                annotationsToJava(runtimeInvisibleAnnotations, "\t", "\n")+
                "\t{version="+majorVersion+"."+minorVersion+"}\n)"
        } catch {
            case ct: ControlThrowable ⇒ throw ct
            case e: Exception ⇒
                throw new RuntimeException(
                    "creating a string representation for "+thisType.toJava+" failed",
                    e)
        }
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

    def unapply(classFile: ClassFile): Option[(Int, ObjectType, Option[ObjectType], Seq[ObjectType])] = {
        import classFile._
        Some((accessFlags, thisType, superclassType, interfaceTypes))
    }
}
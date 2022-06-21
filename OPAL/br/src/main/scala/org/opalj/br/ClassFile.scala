/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br

import scala.annotation.tailrec
import org.opalj.log.OPALLogger
import org.opalj.collection.immutable.UShortPair
import org.opalj.bi.ACC_ABSTRACT
import org.opalj.bi.ACC_ANNOTATION
import org.opalj.bi.ACC_PRIVATE
import org.opalj.bi.ACC_ENUM
import org.opalj.bi.ACC_FINAL
import org.opalj.bi.ACC_INTERFACE
import org.opalj.bi.ACC_MODULE
import org.opalj.bi.ACC_PUBLIC
import org.opalj.bi.ACC_SUPER
import org.opalj.bi.AccessFlags
import org.opalj.bi.AccessFlagsContexts
import org.opalj.bi.AccessFlagsMatcher
import org.opalj.bi.VisibilityModifier
import org.opalj.collection.{binarySearch, insertedAt}

import scala.collection.immutable.ArraySeq

/**
 * Represents a single class file which either defines a class type or an interface type.
 * (`Annotation` types are also interface types and `Enum`s are class types.)
 *
 * @param   version A pair of unsigned short values identifying the class file version number.
 *          `UShortPair(minorVersion, majorVersion)`.
 * @param   accessFlags The access flags of this class. To further analyze the access flags
 *          either use the corresponding convenience methods (e.g., isEnumDeclaration())
 *          or the class [[org.opalj.bi.AccessFlagsIterator]] or the classes which
 *          inherit from [[org.opalj.bi.AccessFlag]].
 * @param   thisType The type implemented by this class file.
 * @param   superclassType The class type from which this class inherits. `None` if this
 *          class file defines `java.lang.Object` or a module.
 * @param   interfaceTypes The set of implemented interfaces. May be empty.
 * @param   fields The declared fields. May be empty. The list is sorted by name.
 * @param   methods The declared methods. May be empty. The list is first sorted by name,
 *          and then by method descriptor.
 * @param   attributes This class file's reified attributes. Which attributes
 *          are reified depends on the configuration of the class file reader; e.g.,
 *          [[org.opalj.br.reader.Java8Framework]].
 *          The JVM specification defines the following attributes:
 *           - ''InnerClasses''
 *           - ''EnclosingMethod''
 *           - ''Synthetic''
 *           - ''Signature''
 *           - ''SourceFile''
 *           - ''SourceDebugExtension''
 *           - ''Deprecated''
 *           - ''RuntimeVisibleAnnotations''
 *           - ''RuntimeInvisibleAnnotations''
 *          In case of Java 9 ([[org.opalj.br.reader.Java9Framework]]) the following
 *          attributes are added:
 *           - ''Module''
 *           - ''ModuleMainClass''
 *           - ''ModulePackages''
 *
 *          The ''BootstrapMethods'' attribute, which is also defined by the JVM specification,
 *          may, however, be resolved and is then no longer part of the attributes table of
 *          the class file.
 *          The ''BootstrapMethods'' attribute is basically the container for the bootstrap
 *          methods referred to by the [[org.opalj.br.instructions.INVOKEDYNAMIC]]
 *          instructions.
 *
 * @note    Equality of `ClassFile` objects is reference based and a class file's hash code
 *          is the same as the underlying [[ObjectType]]'s hash code; i.e., ' `thisType`'s hash code.
 *
 * @author  Michael Eichberg
 */
final class ClassFile private (
        val version:        UShortPair,
        val accessFlags:    Int,
        val thisType:       ObjectType,
        val superclassType: Option[ObjectType],
        val interfaceTypes: ObjectTypes,
        val fields:         Fields,
        val methods:        Methods,
        val attributes:     Attributes
) extends ConcreteSourceElement {

    methods.foreach { m => assert(m.declaringClassFile == null); m.declaringClassFile = this }
    fields.foreach { f => assert(f.declaringClassFile == null); f.declaringClassFile = this }

    /**
     * Compares this class file with the given one; returns (the first) differences if any. The
     * comparison tries to be stable in the presence of difference that are not runtime relevant.
     * For example, the precise structure of the constant pool is completely irrelevant.
     * Additionally, some variance in the bytecode (e.g., `bipush(2)` vs `iconst_2`) is generally
     * irrelevant and also the order in which [[Attribute]]s are found.
     *
     * The degree to which the two class files have to be similar can be configured using
     * a [[SimilarityTestConfiguration]] object. By default, all parts will be compared and have to
     * be equal except of irrelevant differences.
     * The default ([[CompareAllConfiguration]]) compares all parts.
     *
     * @return `None` if this class file and the other are equal - i.e., if both
     *          effectively implement the same class.
     */
    def findDissimilarity(
        other:  ClassFile,
        config: SimilarityTestConfiguration = CompareAllConfiguration
    ): Option[AnyRef] = {

        if (this.version != other.version) {
            return Some(("class file version", this.version, other.version));
        }

        if (this.accessFlags != other.accessFlags) {
            return Some(("class file access flags", this.accessFlags, other.accessFlags));
        }

        if (this.thisType != other.thisType) {
            return Some(("declared type", this.thisType.toJava, other.thisType.toJava));
        }

        if (this.superclassType != other.superclassType) {
            return Some(("declared supertype", this.superclassType, other.superclassType));
        }

        if (this.interfaceTypes != other.interfaceTypes) {
            return Some(("inherited interface types", this.interfaceTypes, other.interfaceTypes));
        }

        val (thisFields, otherFields) = config.compareFields(this, this.fields, other.fields)
        if (thisFields.size != otherFields.size) {
            val message = "number of (filtered) fields differ"
            return Some((message, thisFields.size, otherFields.size));
        }
        // RECALL The fields are always strictly ordered in the same way!
        val thisFieldIt = thisFields.iterator
        val otherFieldIt = otherFields.iterator
        while (thisFieldIt.hasNext) {
            val thisField = thisFieldIt.next()
            val otherField = otherFieldIt.next()
            if (!thisField.similar(otherField, config)) {
                return Some(("the fields are different", thisField, otherField));
            }
        }

        val (thisMethods, otherMethods) = config.compareMethods(this, this.methods, other.methods)
        if (thisMethods.size != otherMethods.size) {
            val message = "number of (filtered) methods differ"
            return Some((message, thisMethods.size, otherMethods.size));
        }
        // RECALL The methods are always strictly ordered in the same way!
        val thisMethodIt = thisMethods.iterator
        val otherMethodIt = otherMethods.iterator
        while (thisMethodIt.hasNext) {
            val thisMethod = thisMethodIt.next()
            val otherMethod = otherMethodIt.next()
            if (!thisMethod.similar(otherMethod, config)) {
                return Some(("the methods are different", thisMethod, otherMethod));
            }
        }

        compareAttributes(other.attributes, config)
    }

    /**
     * Compares this class file with the given one to check if both define the same class modulo
     * those parts which are not considered relevant.
     *
     * @see [[findDissimilarity]] for further information.
     *
     * @param config Configures which parts of the class files should be compared.
     */
    def similar(
        other:  ClassFile,
        config: SimilarityTestConfiguration = CompareAllConfiguration
    ): Boolean = {
        findDissimilarity(other, config).isEmpty
    }

    /**
     * Creates a deep copy of this class file object which also copies the methods and fields.
     *
     * @note If the requirements of `unsafeReplaceMethod` are met you should use that method!
     */
    def copy(
        version:        UShortPair         = this.version,
        accessFlags:    Int                = this.accessFlags,
        thisType:       ObjectType         = this.thisType,
        superclassType: Option[ObjectType] = this.superclassType,
        interfaceTypes: ObjectTypes        = this.interfaceTypes,
        fields:         FieldTemplates     = this.fields.map[FieldTemplate](f => f.copy()),
        methods:        MethodTemplates    = this.methods.map[MethodTemplate](m => m.copy()),
        attributes:     Attributes         = this.attributes
    ): ClassFile = {
        ClassFile(
            version.minor, version.major, accessFlags,
            thisType, superclassType, interfaceTypes,
            fields,
            methods,
            attributes
        )
    }

    /**
     * Creates a new class file object which has the specified attributes.
     *
     * '''The old class file object must not be used after this call; if this cannot be guaranteed
     * `copy` has to be used; otherwise the back-references (field -> class file and method ->
     * class file) are broken!'''
     *
     * @note This method is primarily intended to be used to perform load-time transformations!
     */
    def _UNSAFE_replaceAttributes(newAttributes: Attributes): ClassFile = {
        val newMethods = this.methods
        newMethods.foreach(m => m.detach())

        val newFields = this.fields
        newFields.foreach(f => f.detach())

        new ClassFile(
            this.version,
            this.accessFlags,
            this.thisType,
            this.superclassType,
            this.interfaceTypes,
            newFields,
            newMethods,
            newAttributes
        )
    }

    /**
     * Creates a new class file object where the method `oldMethod` is replaced by the `newMethod`.
     * Hence, the old method must be defined by this class file!
     *
     * '''Both methods have to have the same name and descriptor!'''
     *
     * '''The old class file object must not be used after this call; if this cannot be guaranteed
     * `copy` has to be used; otherwise the back-references (field -> class file and method ->
     * class file) are broken!'''
     *
     * @note This method is primarily intended to be used to perform load-time transformations!
     */
    def _UNSAFE_replaceMethod(oldMethod: Method, newMethod: MethodTemplate): this.type = {
        assert(oldMethod.name == newMethod.name)
        assert(oldMethod.descriptor == newMethod.descriptor)

        val index = binarySearch[Method, JVMMethod](this.methods, oldMethod)
        val newPreparedMethod: Method = newMethod.prepareClassFileAttachement()
        newPreparedMethod.declaringClassFile = this
        val methods = this.methods.unsafeArray.asInstanceOf[Array[AnyRef]]
        methods(index) = newPreparedMethod

        oldMethod.detach(); // TO BE SURE THAT THE OLD METHOD NO LONGER REFERENCES THIS CLASS FILE

        this
    }

    /**
     * Creates a new class file object with the given method.
     *
     * '''This class file must not contain a method with the same name and descriptor!'''
     *
     * '''The old class file object must not be used after this call; if this cannot be guaranteed
     * `copy` has to be used; otherwise the back-references (field -> class file and method ->
     * class file) are broken!'''
     *
     * @note This method is primarily intended to be used to perform load-time transformations!
     */
    def _UNSAFE_addMethod(methodTemplate: MethodTemplate): ClassFile = {
        val newMethod = methodTemplate.prepareClassFileAttachement()

        assert(this.findMethod(newMethod.name, newMethod.descriptor).isEmpty)

        val index = binarySearch[Method, JVMMethod](this.methods, newMethod)
        if (index >= 0)
            throw new IllegalArgumentException(
                s"$this: a method with the given name and descriptor already exists: $newMethod"
            )
        val insertionPoint = -index - 1
        var newMethods = this.methods
        newMethods.foreach(m => m.detach())
        newMethods = insertedAt(newMethods, insertionPoint, newMethod)

        val newFields = this.fields
        newFields.foreach(f => f.detach())

        new ClassFile(
            this.version,
            this.accessFlags,
            this.thisType,
            this.superclassType,
            this.interfaceTypes,
            newFields,
            newMethods,
            this.attributes
        )
    }

    def methodsWithBody: Iterator[Method] = methods.iterator.filter(_.body.isDefined)

    def methodBodies: Iterator[Code] = methods.iterator.flatMap(_.body)

    import ClassFile._

    def minorVersion: UShort = version.minor

    def majorVersion: UShort = version.major

    def jdkVersion: String = org.opalj.bi.jdkVersion(majorVersion)

    override def isClass: Boolean = true

    override def asClassFile: this.type = this

    def asVirtualClass: VirtualClass = VirtualClass(thisType)

    /**
     * The unique id associated with the type defined by this class file.
     */
    def id = thisType.id

    /**
     * The fully qualified name of the type defined by this class file.
     */
    def fqn: String = thisType.fqn

    def isAbstract: Boolean = (ACC_ABSTRACT.mask & accessFlags) != 0

    def isFinal: Boolean = (ACC_FINAL.mask & accessFlags) != 0

    /**
     * Returns `true` if the class is final or if it only defines private constructors and it
     * is therefore not possible to inherit from this class.
     *
     * An abstract type (abstract classes and interfaces) is never effectively final.
     */
    def isEffectivelyFinal: Boolean = {
        isFinal || (
            !isAbstract && (constructors forall { _.isPrivate })
        )
    }

    /**
     * `true` if the class file has public visibility. If `false` the method `isPackageVisible`
     * will return `true`.
     *
     * @note There is no private or protected visibility.
     */
    def isPublic: Boolean = (ACC_PUBLIC.mask & accessFlags) != 0

    /**
     * `true` if the class file has package visibility. If `false` the method `isPublic`
     * will return `true`.
     *
     * @note    A class file cannot have private or protected visibility.
     */
    def isPackageVisible: Boolean = !isPublic

    def isClassDeclaration: Boolean = (accessFlags & classCategoryMask) == 0

    def isEnumDeclaration: Boolean = (accessFlags & ACC_ENUM.mask) == ACC_ENUM.mask

    // JVM 9 Specification:
    // If ACC_MODULE is set in ClassFile.access_flags, then no other flag in
    // ClassFile.access_flags may be set.
    def isModuleDeclaration: Boolean = accessFlags == ACC_MODULE.mask

    /**
     * Returns true if this class file represents an interface.
     *
     * @note From the JVM point-of-view annotations are also interfaces!
     *
     * @see [[org.opalj.br.analyses.Project]] to determine if this interface declaration is a
     *      functional interface.
     */
    def isInterfaceDeclaration: Boolean = (accessFlags & ACC_INTERFACE.mask) == ACC_INTERFACE.mask

    def isAnnotationDeclaration: Boolean = (accessFlags & classCategoryMask) == annotationMask

    def isInnerClass: Boolean = innerClasses.exists(_.exists(_.innerClassType == thisType))

    /**
     * Returns `true` if this class file has no direct representation in the source code.
     *
     * @see [[VirtualTypeFlag]] for further information.
     */
    def isVirtualType: Boolean = attributes.contains(VirtualTypeFlag)

    /**
     * Returns Java 9's module attribute if defined.
     */
    def module: Option[Module] = { attributes collectFirst { case m: Module => m } }

    def enclosingMethod: Option[EnclosingMethod] = {
        attributes collectFirst { case em: EnclosingMethod => em }
    }

    /**
     * Returns this class file's bootstrap method table.
     *
     * @note    A class file's bootstrap method table may be removed at load time if
     *          the corresponding [[org.opalj.br.instructions.INVOKEDYNAMIC]] instructions
     *          are rewritten.
     */
    def bootstrapMethodTable: Option[BootstrapMethodTable] = {
        attributes collectFirst { case bmt: BootstrapMethodTable => bmt }
    }

    /**
     * Returns OPAL's [[SynthesizedClassFiles]] attribute if it is defined.
     */
    def synthesizedClassFiles: Option[SynthesizedClassFiles] = {
        attributes collectFirst { case scf: SynthesizedClassFiles => scf }
    }

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
    def innerClasses: Option[InnerClasses] = {
        attributes collectFirst { case InnerClassTable(ice) => ice }
    }

    /**
     * Returns `true` if this class file defines an anonymous inner class.
     *
     * This method relies on the inner classes attribute to identify anonymous inner
     * classes.
     */
    def isAnonymousInnerClass: Boolean = {
        /*
        isClassDeclaration && innerClasses.isDefined &&  innerClasses.get.exists { i =>
                    i.innerClassType == thisType && {
                        if (i.innerName.isEmpty) true else return false;
                    }
            }
        */
        isClassDeclaration && innerClasses.isDefined && !innerClasses.get.forall { i =>
            i.innerClassType != thisType || i.innerName.nonEmpty
        }
    }

    /**
     * Returns the set of all immediate nested classes of this class. I.e., returns those
     * nested classes that are not defined in the scope of a nested class of this
     * class.
     */
    def nestedClasses(implicit classFileRepository: ClassFileRepository): Seq[ObjectType] = {

        import classFileRepository.logContext

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

        val nestedClassesCandidates =
            innerClasses.map { innerClasses =>
                innerClasses
                    .filter(innerClass =>
                        // it does not describe this class:
                        (!isThisType(innerClass)) &&
                            // it does not give information about an outer class:
                            (!this.fqn.startsWith(innerClass.innerClassType.fqn)) &&
                            // it does not give information about some other inner class of this type:
                            (
                                innerClass.outerClassType.isEmpty ||
                                (innerClass.outerClassType.get eq thisType)
                            ))
                    .map[ObjectType](_.innerClassType)
            }.getOrElse {
                ArraySeq.empty
            }

        // THE FOLLOWING CODE IS NECESSARY TO COPE WITH BYTECODE GENERATED
        // BY OLD JAVA COMPILERS (IN PARTICULAR JAVA 1.1);
        // IT BASICALLY TRIES TO RECREATE THE INNER-OUTERCLASSES STRUCTURE
        if (isInnerType && outerClassType.isEmpty) {
            // let's try to find the outer class that refers to this class
            val thisFQN = thisType.fqn
            val innerTypeNameStartIndex = thisFQN.indexOf('$')
            if (innerTypeNameStartIndex == -1) {
                OPALLogger.warn(
                    "processing bytecode",
                    "the inner class "+thisType.toJava+
                        " does not use the standard naming schema"+
                        "; the inner classes information may be incomplete"
                )

                return nestedClassesCandidates.filter(_.fqn.startsWith(this.fqn));
            }
            val outerFQN = thisFQN.substring(0, innerTypeNameStartIndex)
            classFileRepository.classFile(ObjectType(outerFQN)) match {
                case Some(outerClass) =>

                    def directNestedClasses(objectTypes: Iterable[ObjectType]): Set[ObjectType] = {
                        var nestedTypes: Set[ObjectType] = Set.empty
                        objectTypes.foreach { objectType =>
                            classFileRepository.classFile(objectType) match {
                                case Some(classFile) =>
                                    nestedTypes ++= classFile.nestedClasses(classFileRepository)
                                case None =>
                                    OPALLogger.warn(
                                        "class file reader",
                                        "cannot get informaton about "+objectType.toJava+
                                            "; the inner classes information may be incomplete"
                                    )
                            }
                        }
                        nestedTypes
                    }

                    // let's filter those classes that are known innerclasses of this type's
                    // (indirect) outertype (they cannot be innerclasses of this class..)
                    var nestedClassesOfOuterClass = outerClass.nestedClasses(classFileRepository)
                    while (nestedClassesOfOuterClass.nonEmpty &&
                        !nestedClassesOfOuterClass.contains(thisType) &&
                        !nestedClassesOfOuterClass.exists(nestedClassesCandidates.contains)) {
                        // We are still lacking sufficient information to make a decision
                        // which class is a nested class of which other class
                        // e.g., we might have the following situation:
                        // class X {
                        //  class Y {                                // X$Y
                        //      void m(){
                        //          new Listener(){                  // X$Listener$1
                        //              void event(){
                        //                  new Listener(){...}}}}}} // X$Listener$2
                        nestedClassesOfOuterClass =
                            directNestedClasses(nestedClassesOfOuterClass).toSeq
                    }
                    val filteredNestedClasses =
                        nestedClassesCandidates.filterNot(nestedClassesOfOuterClass.contains)
                    return filteredNestedClasses;
                case None =>
                    val disclaimer = "; the inner classes information may be incomplete"
                    OPALLogger.warn(
                        "project configuration",
                        s"cannot identify the outer type of ${thisType.toJava}$disclaimer"
                    )

                    return nestedClassesCandidates.filter(_.fqn.startsWith(this.fqn));
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
     *   foreachNestedClasses(innerclassesProject, { nc => allNestedTypes += nc.thisType })
     * }}}
     */
    def foreachNestedClass(
        f: (ClassFile) => Unit
    )(
        implicit
        classFileRepository: ClassFileRepository
    ): Unit = {
        nestedClasses(classFileRepository) foreach { nestedType =>
            classFileRepository.classFile(nestedType) foreach { nestedClassFile =>
                f(nestedClassFile)
                nestedClassFile.foreachNestedClass(f)
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
    def outerType: Option[(ObjectType, Int)] = {
        innerClasses flatMap { innerClasses =>
            innerClasses collectFirst {
                case InnerClass(`thisType`, Some(outerType), _, accessFlags) =>
                    (outerType, accessFlags)
            }
        }
    }

    /**
     * Each class file optionally defines a class signature.
     */
    def classSignature: Option[ClassSignature] = {
        attributes collectFirst { case s: ClassSignature => s }
    }

    /**
     * The SourceFile attribute is an optional attribute [...]. There can be
     * at most one `SourceFile` attribute.
     */
    def sourceFile: Option[String] = attributes collectFirst { case SourceFile(s) => s }

    /**
     * The SourceDebugExtension attribute is an optional attribute [...]. There can be
     * at most one `SourceDebugExtension` attribute. The data (which is modified UTF8
     * String may, however, not be representable using a String object (see the
     * spec. for further details.)
     *
     * The returned Array must not be mutated.
     */
    def sourceDebugExtension: Option[Array[Byte]] = {
        attributes collectFirst { case SourceDebugExtension(s) => s }
    }

    /**
     * All constructors/instance initialization methods (`<init>`) defined by this class.
     *
     * (This does not include the static initializer.)
     */
    def constructors: Iterator[Method] = new Iterator[Method] {
        private[this] var i = -1

        private[this] def gotoNextConstructor(): Unit = {
            i += 1
            if (i >= methods.size) {
                i = -1
            } else {
                val methodName = methods(i).name
                val r = methodName.compareTo("<init>")
                if (r < 0 /*methodName < "<init>"*/ )
                    gotoNextConstructor()
                else if (r > 0 /*methodName > "<init>"*/ )
                    i = -1;
            }
        }
        gotoNextConstructor()

        def hasNext: Boolean = i >= 0
        def next(): Method = { val m = methods(i); gotoNextConstructor(); m }
    }

    /**
     * Returns `true` if this class defines a so-called default constructor. A
     * default constructor needs to be present, e.g., when the class is serializable.
     *
     * The default constructor is the constructor that takes no parameters.
     *
     * @note The result is recomputed.
     */
    def hasDefaultConstructor: Boolean = constructors exists { _.descriptor.parametersCount == 0 }

    /**
     * All defined instance methods. I.e., all methods that are not static,
     * constructors, or static initializers.
     */
    def instanceMethods: Iterator[Method] = {
        methods.iterator.filterNot { m => m.isStatic || m.isConstructor || m.isStaticInitializer }
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
            val methodNameComparison = method.name.compareTo("<clinit>")

            if (methodNameComparison == 0 &&
                method.descriptor == noArgsAndReturnVoidDescriptor &&
                (majorVersion < 51 || method.isStatic))
                return Some(method);

            else if (methodNameComparison < 0)
                return None;

            i += 1
        }
        None
    }

    /**
     * Returns the field with the given name, if any.
     *
     * @note The complexity is O(log2 n); this algorithm uses binary search.
     */
    def findField(name: String): List[Field] = {
        @tailrec @inline def findField(low: Int, high: Int): List[Field] = {
            if (high < low)
                return List.empty;

            val mid = (low + high) / 2 // <= will never overflow...(there are at most 65535 fields)
            val field = fields(mid)
            val fieldNameComparison = field.name.compareTo(name)
            if (fieldNameComparison == 0) {
                var theFields = List(field)
                var d = mid - 1
                while (low <= d && fields(d).name.equals(name)) {
                    theFields ::= fields(d)
                    d -= 1
                }
                var u = mid + 1
                while (u <= high && fields(u).name.equals(name)) {
                    theFields ::= fields(u)
                    u += 1
                }
                theFields
            } else if (fieldNameComparison < 0) {
                findField(mid + 1, high)
            } else {
                findField(low, mid - 1)
            }
        }

        findField(0, fields.size - 1)
    }

    /**
     * Returns the field with the given name and type.
     */
    def findField(name: String, fieldType: FieldType): Option[Field] = {
        findField(name).find(f => f.fieldType eq fieldType)
    }

    /**
     * Returns the methods (including constructors and static initializers) with the given name,
     * if any.
     *
     * @note The complexity is O(log2 n); this algorithm uses binary search.
     */
    def findMethod(name: String): List[Method] = {
        @tailrec @inline def findMethod(low: Int, high: Int): List[Method] = {
            if (high < low)
                return List.empty;

            val mid = (low + high) / 2 // <= will never overflow...(there are at most 65535 methods)
            val method = methods(mid)
            val methodName = method.name
            val methodNameComparison = methodName.compareTo(name)
            if (methodNameComparison == 0) {
                var theMethods = List(method)
                var d = mid - 1
                while (low <= d && methods(d).name.equals(name)) {
                    theMethods ::= methods(d)
                    d -= 1
                }
                var u = mid + 1
                while (u <= high && methods(u).name.equals(name)) {
                    theMethods ::= methods(u)
                    u += 1
                }
                theMethods
            } else if (methodNameComparison < 0) {
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
                return None;

            val mid = (low + high) / 2 // <= will never overflow...(there are at most 65535 methods)
            val method = methods(mid)
            val nameComparison = method.name.compareTo(name)
            if (nameComparison == 0) {
                val methodDescriptorComparison = method.descriptor.compare(descriptor)
                if (methodDescriptorComparison < 0)
                    findMethod(mid + 1, high)
                else if (methodDescriptorComparison == 0)
                    Some(method)
                else
                    findMethod(low, mid - 1)
            } else if (nameComparison < 0) {
                findMethod(mid + 1, high)
            } else {
                findMethod(low, mid - 1)
            }
        }

        findMethod(0, methods.size - 1)
    }

    /**
     * Returns the method which directly overrides a method with the given properties. The result
     * is `Success(<Method>)`` if we can find a method; `Empty` if no method can be found and
     * `Failure` if a method is found which supposedly overrides the specified method,
     * but which is less visible.
     *
     * @note    This method is only defined for proper virtual methods. I.e., asking for
     *          overridings of a private methods is not supported.
     */
    def findDirectlyOverridingMethod(
        packageName: String,
        visibility:  Option[VisibilityModifier],
        name:        String,
        descriptor:  MethodDescriptor
    ): Result[Method] = {
        assert(visibility.isEmpty || visibility.get != ACC_PRIVATE)

        findMethod(name, descriptor).filter(m => !m.isStatic) match {

            case Some(candidateMethod) =>
                import VisibilityModifier.isAtLeastAsVisibleAs
                if (Method.canDirectlyOverride(thisType.packageName, visibility, packageName) &&
                    isAtLeastAsVisibleAs(candidateMethod.visibilityModifier, visibility))
                    Success(candidateMethod)
                else
                    Failure

            case None =>
                Empty
        }

    }

    final def findDirectlyOverridingMethod(
        packageName: String,
        method:      Method
    ): Result[Method] = {
        findDirectlyOverridingMethod(
            packageName,
            method.visibilityModifier,
            method.name,
            method.descriptor
        )
    }

    def findMethod(
        name:       String,
        descriptor: MethodDescriptor,
        matcher:    AccessFlagsMatcher
    ): Option[Method] = {
        findMethod(name, descriptor) filter { m => matcher.unapply(m.accessFlags) }
    }

    /**
     * This class file's `hasCode`. The `hashCode` is (by purpose) identical to
     * the id of the `ObjectType` it implements.
     */
    override def hashCode: Int = thisType.id

    override def equals(other: Any): Boolean = {
        other match {
            case that: ClassFile => that eq this
            case _               => false
        }
    }

    override def toString: String = {
        val superIntefaces =
            if (interfaceTypes.nonEmpty)
                interfaceTypes.iterator.map[String](_.toJava).mkString("\t\twith ", " with ", "\n")
            else
                ""

        "ClassFile(\n\t"+
            AccessFlags.toStrings(accessFlags, AccessFlagsContexts.CLASS).mkString("", " ", " ") +
            thisType.toJava+"\n"+
            superclassType.map("\textends "+_.toJava+"\n").getOrElse("") +
            superIntefaces +
            annotationsToJava(runtimeVisibleAnnotations, "\t@{ ", " }\n") +
            annotationsToJava(runtimeInvisibleAnnotations, "\t@{ ", " }\n")+
            "\t[version="+majorVersion+"."+minorVersion+"]\n)"

    }

}
/**
 * Defines factory and extractor methods for `ClassFile` objects as well as related
 * constants.
 *
 * @author Michael Eichberg
 */
object ClassFile {

    val classCategoryMask: Int = {
        ACC_INTERFACE.mask | ACC_ANNOTATION.mask | ACC_ENUM.mask | ACC_MODULE.mask
    }

    val annotationMask: Int = ACC_INTERFACE.mask | ACC_ANNOTATION.mask

    /**
     * @note   The default version is equivalent to Java 5, i.e.,
     *         no StackMapTable attribute is required.
     * @param  accessFlags This class' access flags, by default: PUBLIC and SUPER
     *         (always need to be set)
     * @param  superclassType The class from which this class/interface inherits from. By default
     *         `java.lang.Object`.
     */
    def apply(
        minorVersion:   Int                = 0,
        majorVersion:   Int                = 50,
        accessFlags:    Int                = { ACC_PUBLIC.mask | ACC_SUPER.mask },
        thisType:       ObjectType,
        superclassType: Option[ObjectType] = Some(ObjectType.Object),
        interfaceTypes: Interfaces         = NoInterfaces,
        fields:         FieldTemplates     = NoFieldTemplates,
        methods:        MethodTemplates    = NoMethodTemplates,
        attributes:     Attributes         = NoAttributes
    ): ClassFile = {
        new ClassFile(
            UShortPair(minorVersion, majorVersion),
            accessFlags,
            thisType, superclassType, interfaceTypes,
            fields.sorted[JVMField].map[Field](f => f.prepareClassFileAttachement()),
            methods.sorted[JVMMethod].map[Method](f => f.prepareClassFileAttachement()),
            attributes
        )
    }

    // This method is only intended to be called by the ClassFileReader/ClassFileBinding!
    protected[br] def reify(
        minorVersion:   Int                = 0,
        majorVersion:   Int                = 50,
        accessFlags:    Int                = { ACC_PUBLIC.mask | ACC_SUPER.mask },
        thisType:       ObjectType,
        superclassType: Option[ObjectType] = Some(ObjectType.Object),
        interfaceTypes: Interfaces         = NoInterfaces,
        fields:         Fields             = NoFields,
        methods:        Methods            = NoMethods,
        attributes:     Attributes         = NoAttributes
    ): ClassFile = {
        new ClassFile(
            UShortPair(minorVersion, majorVersion),
            accessFlags,
            thisType, superclassType, interfaceTypes,
            fields.sorted[JVMField],
            methods.sorted[JVMMethod],
            attributes
        )
    }

    def unapply(
        classFile: ClassFile
    ): Option[(Int, ObjectType, Option[ObjectType], Seq[ObjectType])] = {
        import classFile._
        Some((accessFlags, thisType, superclassType, interfaceTypes))
    }
}

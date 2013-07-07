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
package de.tud.cs.st.bat
package resolved


/**
 * Represents a single class file.
 *
 * @param minorVersion The minor part of this class file's version number.
 * @param majorVersion The major part of this class file's version number.
 * @param accessFlags This class' access flags. To further analyze the access flags
 *  either use the corresponding convenience methods (e.g., isEnumDeclaration())
 *  or the class [[de.tud.cs.st.bat.AccessFlagsIterator]] or the classes which
 *  inherit from [[de.tud.cs.st.bat.AccessFlag]].
 * @param thisClass The type implemented by this class file.
 * @param superClass The class from which this class inherits. None, if this
 * 	class file represents java.lang.Object.
 * @param interfaces The set of implemented interfaces. May be empty.
 * @param fields The set of declared fields. May be empty.
 * @param methods The set of declared methods. May be empty.
 * @param attributes This class file's reified attributes. Which attributes
 *  are reified depends on the configuration of the class file reader; e.g.,
 *  [[de.tud.cs.st.bat.resolved.reader.Java7Framework]].
 *
 * @author Michael Eichberg
 */
case class ClassFile(minorVersion: Int,
                     majorVersion: Int,
                     accessFlags: Int,
                     thisClass: ObjectType,
                     superClass: Option[ObjectType],
                     interfaces: Seq[ObjectType],
                     fields: Fields,
                     methods: Methods,
                     attributes: Attributes)
        extends CommonAttributes with SourceElement {

    import ClassFile._

    override def isClassFile = true

    override def asClassFile = this

    def isAbstract: Boolean = ACC_ABSTRACT element_of accessFlags

    def isFinal: Boolean = ACC_FINAL element_of accessFlags

    def isPublic: Boolean = ACC_PUBLIC element_of accessFlags

    def isClassDeclaration: Boolean = (accessFlags & classCategoryMask) == 0

    def isEnumDeclaration: Boolean = (accessFlags & classCategoryMask) == ACC_ENUM.mask

    def isInterfaceDeclaration: Boolean = (accessFlags & classCategoryMask) == ACC_INTERFACE.mask

    def isAnnotationDeclaration: Boolean = (accessFlags & classCategoryMask) == annotationMask

    def isInnerClass: Boolean = innerClasses.exists(_.exists(_.innerClassType == thisClass))

    def enclosingMethod: Option[EnclosingMethod] =
        attributes collectFirst { case em: EnclosingMethod ⇒ em }

    def innerClasses: Option[InnerClasses] =
        attributes collectFirst { case InnerClassTable(ice) ⇒ ice }

    // TODO should we get rid of constant value attributes... they are resolved?
    
    // TODO [Java 7] should we keep it or should we completely resolve and remove it???
    lazy val bootstrapMethods: Option[BootstrapMethods] =
        attributes collectFirst { case BootstrapMethodTable(bms) ⇒ bms }

    /**
     * Each class has at most one explicit, direct outer type.
     *
     * @return The object type of the outer type as well as this inner classes' access flags.
     */
    def outerType: Option[(ObjectType, Int)] = {
        innerClasses.flatMap(_ collectFirst {
            case InnerClass(`thisClass`, Some(outerType), _, accessFlags) ⇒ (outerType, accessFlags)
        })
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
    def sourceFile: Option[String] = attributes collectFirst { case SourceFile(s) ⇒ s }

    def sourceDebugExtension: Option[String] = attributes collectFirst { case SourceDebugExtension(s) ⇒ s }

    /**
     * All constructors/instance initialization methods defined by this class. (This does not include static initializers.)
     */
    def constructors: Seq[Method] = methods.view.filter(_.name == "<init>")

    def staticInitializer: Option[Method] =
        methods.collectFirst({
            case method @ Method(_, "<clinit>", MethodDescriptor(Seq(), VoidType), _) ⇒ method
        })
}

object ClassFile {

    private val classCategoryMask: Int = ACC_INTERFACE.mask | ACC_ANNOTATION.mask | ACC_ENUM.mask

    private val annotationMask: Int = ACC_INTERFACE.mask | ACC_ANNOTATION.mask
}
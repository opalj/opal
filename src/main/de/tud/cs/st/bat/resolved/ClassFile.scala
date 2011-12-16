/* License (BSD Style License):
*  Copyright (c) 2009, 2011
*  Software Technology Group
*  Department of Computer Science
*  Technische Universität Darmstadt
*  All rights reserved.
*
*  Redistribution and use in source and binary forms, with or without
*  modification, are permitted provided that the following conditions are met:
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
*  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
*  AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
*  IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
*  ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
*  LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
*  CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
*  SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
*  INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
*  CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
*  ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
*  POSSIBILITY OF SUCH DAMAGE.
*/
package de.tud.cs.st.bat
package resolved
import de.tud.cs.st.bat.ACC_FINAL

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
 *  are reified dependes on the configuration of the class file reader; e.g.,
 *  [[de.tud.cs.st.bat.resolved.reader.Java6Framework]].
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
        extends CommonAttributes {

    private val classCategoryMask: Int = ACC_INTERFACE.mask | ACC_ANNOTATION.mask | ACC_ENUM.mask

    private val annotationMask: Int = ACC_INTERFACE.mask | ACC_ANNOTATION.mask

    def isFinal: Boolean = ACC_FINAL element_of accessFlags

    def isPublic: Boolean = ACC_PUBLIC element_of accessFlags

    def isClassDeclaration: Boolean = (accessFlags & classCategoryMask) == 0

    def isEnumDeclaration: Boolean = (accessFlags & classCategoryMask) == ACC_ENUM.mask

    def isInterfaceDeclaration: Boolean = (accessFlags & classCategoryMask) == ACC_INTERFACE.mask

    def isAnnotationDeclaration: Boolean = (accessFlags & classCategoryMask) == annotationMask

    def enclosingMethod: Option[EnclosingMethod] =
        attributes collectFirst { case em: EnclosingMethod ⇒ em }

    def innerClasses: Option[InnerClasses] =
        attributes collectFirst { case InnerClassesAttribute(ice) ⇒ ice }

    /**
     * Each class file optionally defines a class signature.
     */
    def classSignature: Option[ClassSignature] =
        attributes collectFirst { case s: ClassSignature ⇒ s }

    /**
     * The SourceFile attribute is an optional attribute [...]. There can be
     * at most one SourceFile attribute.
     */
    def sourceFile: Option[String] =
        attributes collectFirst { case SourceFileAttribute(s) ⇒ s }

    def sourceDebugExtension: Option[String] =
        attributes collectFirst { case SourceDebugExtension(s) ⇒ s }

    /**
     * All constructors/instance initialization methods defined by this class. (This does not include static initializers.)
     */
    def constructors: Seq[Method] = methods.view.filter(_.name == "<init>")

    def staticInitializer: Option[Method] =
        methods.collectFirst({
            case method @ Method(_, "<clinit>", MethodDescriptor(Seq(), VoidType), _) ⇒ method
        })

    //
    //
    // SUPPORT FOR SPECIAL REPRESENTATIONS
    //
    //

    import AccessFlagsContexts.CLASS

    def toXML =
        <class
			type={ thisClass.className }
			minor_version={ minorVersion.toString }
			major_version={ majorVersion.toString } >
			<flags>{ AccessFlagsIterator(accessFlags, CLASS) map(_.toXML) }</flags>
			<attributes>{ for (attribute ← attributes) yield attribute.toXML }</attributes>
			<extends type={ if (superClass.isDefined) { Some(scala.xml.Text(superClass.get.className)) } else { None } } />
			{ for (interface ← interfaces) yield <implements type={ interface.className }/> }
			{ for (field ← fields) yield field.toXML }
			{ for (method ← methods) yield method.toXML }
		</class>

    /**
     * Converts this object-oriented representation of a class file to a list of Prolog facts.
     *
     * Structure of a class_file fact (representing only the class declaration):
     * {{{
     * class_file(
     * 		classFileKey : Atom
     * 		classCategory : Atom
     * 		thisType : Term,
     * 		superType : Term, // null if this class file represents "java.lang.Object"
     * 		implementedTypes : Terms[ObjectTypeTerm],
     * 		visibility : Atom
     * 		final : Term
     * 		abstract : Term
     * 		synthetic : Term
     * 		deprecated : Term
     * )
     * }}}
     *
     * The "super" access_flag is not modeled since it is a legacy flag that is always set.
     *
     * The modeling of the facts is inspired by the representation used in:
     * "Verification of Java Bytecode using Analysis and Transformation of Logic Programs;
     * E. Albert, M. Gomez-Zamalloa, L. Hubert and G. Puebla"
     */
    def toProlog[F, T, A <: T](factory: PrologTermFactory[F, T, A]): List[F] = {

        import factory._

        var facts: List[F] = Nil

        val key = KeyAtom("cf_")

        for (field ← fields) {
            facts = field.toProlog(factory, key) ::: facts
        }
        for (method ← methods) {
            facts = method.toProlog(factory, key) ::: facts
        }
        for (attribute ← attributes) {
            facts = (attribute match {
                case sfa: SourceFileAttribute   ⇒ sfa.toProlog(factory, key)
                case aa: AnnotationsAttribute   ⇒ aa.toProlog(factory, key)
                case ema: EnclosingMethod       ⇒ ema.toProlog(factory, key)
                case ics: InnerClassesAttribute ⇒ ics.toProlog(factory, key)
                case _                          ⇒ Nil
            }) ::: facts
        }

        Fact(
            "class_file", // functor
            key,
            getClassCategoryAtom(factory),
            thisClass.toProlog(factory),
            if (superClass.isDefined) superClass.get.toProlog(factory) else NoneAtom,
            Terms(interfaces, (_: ObjectType).toProlog(factory)),
            VisibilityAtom(accessFlags, CLASS),
            FinalTerm(accessFlags),
            AbstractTerm(accessFlags),
            SyntheticTerm(accessFlags, attributes),
            DeprecatedTerm(attributes)
        ) :: facts

    }

    private def getClassCategoryAtom[F, T, A <: T](factory: PrologTermFactory[F, T, A]): A = {
        accessFlags & classCategoryMask match {
            case 0                  ⇒ factory.StringAtom("class_declaration")
            case ACC_INTERFACE.mask ⇒ factory.StringAtom("interface_declaration")
            case ACC_ENUM.mask      ⇒ factory.StringAtom("enum_declaration")
            case _                  ⇒ factory.StringAtom("annotation_declaration")
        }
    }
}
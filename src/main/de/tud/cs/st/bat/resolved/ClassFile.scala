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
package de.tud.cs.st.bat.resolved

import scala.xml.Elem
import scala.xml.Null
import scala.xml.Text
import scala.xml.TopScope

import de.tud.cs.st.bat.canonical.AccessFlagsContext
import de.tud.cs.st.bat.canonical.ACC_INTERFACE
import de.tud.cs.st.bat.canonical.ACC_ANNOTATION
import de.tud.cs.st.bat.canonical.ACC_ENUM

import TypeAliases._

/**
 * Represents a single class file.
 *
 * @author Michael Eichberg
 */
case class ClassFile(
        val minorVersion: Int,
        val majorVersion: Int,
        val accessFlags: Int,
        val thisClass: ObjectType,
        val superClass: ObjectType,
        val interfaces: Seq[ObjectType],
        val fields: Fields,
        val methods: Methods,
        val attributes: Attributes) {

    /**
     * Each class file optionally defines a clas signature.
     */
    def classSignature: Option[ClassSignature] = {
        attributes find {
            case Signature_attribute(s: ClassSignature) ⇒ return Some(s)
            case _                                      ⇒ false
        }
        None
    }

    /**
     * The SourceFile attribute is an optional attribute [...]. There can be
     * at most one SourceFile attribute.
     */
    def sourceFile: Option[String] = {
        // for (attribute ← attributes) {
        // 	attribute match {
        // 		case SourceFile_attribute(s) ⇒ return Some(s)
        // 		case _                                      ⇒ ;
        // 	}
        // }
        attributes find {
            case SourceFile_attribute(s) ⇒ return Some(s)
            case _                       ⇒ false
        }
        None
    }

    import de.tud.cs.st.bat.canonical.AccessFlagsContext.CLASS
    import de.tud.cs.st.bat.canonical.AccessFlagsIterator

    def toXML = {
        <class
			type={ thisClass.className }
			minor_version={ minorVersion.toString }
			major_version={ majorVersion.toString } >
			<flags>{ AccessFlagsIterator(accessFlags, CLASS) map((f) ⇒ Elem(null, f.toString, Null, TopScope)) }</flags>
			<attributes>{ for (attribute ← attributes) yield attribute.toXML }</attributes>
			<extends type={ if (superClass ne null) { Some(Text(superClass.className)) } else { None } } />
			{ for (interface ← interfaces) yield <implements type={ interface.className }/> }
			{ for (field ← fields) yield field.toXML }
			{ for (method ← methods) yield method.toXML }
		</class>
    }

    /**
     * Converts this object-oriented representation of a class file to a list of Prolog facts.
     * 	<br/>
     * Structure of a class_file fact (representing only the class declaration):
     * <pre>
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
     * </pre>
     * The "super" access_flag is not modeled since it is a legacy flag that is always set.
     * <br />
     * <i>
     * The modeling of the facts is inspired by the representation used in:
     * "Verification of Java Bytecode using Analysis and Transformation of Logic Programs;
     * E. Albert, M. Gomez-Zamalloa, L. Hubert and G. Puebla"
     * </i>
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
                case sfa: SourceFile_attribute      ⇒ sfa.toProlog(factory, key)
                case aa: Annotations_Attribute      ⇒ aa.toProlog(factory, key)
                case ema: EnclosingMethod_attribute ⇒ ema.toProlog(factory, key)
                case _                              ⇒ Nil
            }) ::: facts
        }

        Fact(
            "class_file", // functor
            key,
            getClassCategoryAtom(factory),
            thisClass.toProlog(factory),
            if (superClass ne null) superClass.toProlog(factory) else NoneAtom,
            Terms(interfaces, (_: ObjectType).toProlog(factory)),
            VisibilityAtom(accessFlags, CLASS),
            FinalTerm(accessFlags),
            AbstractTerm(accessFlags),
            SyntheticTerm(accessFlags, attributes),
            DeprecatedTerm(attributes)
        ) :: facts

    }

    private val classCategoryMask: Int =
        ACC_INTERFACE.mask | ACC_ANNOTATION.mask | ACC_ENUM.mask

    private val annotationMask: Int =
        ACC_ANNOTATION.mask | ACC_INTERFACE.mask

    private def getClassCategoryAtom[F, T, A <: T](factory: PrologTermFactory[F, T, A]): A = {

        import factory._

        accessFlags & classCategoryMask match {
            case 0                  ⇒ StringAtom("class_declaration")
            case ACC_INTERFACE.mask ⇒ StringAtom("interface_declaration")
            case ACC_ENUM.mask      ⇒ StringAtom("enum_declaration")
            case annotation_mask    ⇒ StringAtom("annotation_declaration")
        }
    }
}
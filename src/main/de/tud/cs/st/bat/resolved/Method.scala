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

/**
 * Represents a single method.
 *
 * @author Michael Eichberg
 */
case class Method(accessFlags: Int,
                  name: String,
                  descriptor: MethodDescriptor,
                  attributes: Attributes)
        extends ClassMember {

    override def isMethod = true

    override def asMethod = this

    def runtimeVisibleParameterAnnotations: Option[ParameterAnnotations] =
        attributes collectFirst { case RuntimeVisibleParameterAnnotationTable(pas) ⇒ pas }

    def runtimeInvisibleParameterAnnotations: Option[ParameterAnnotations] =
        attributes collectFirst { case RuntimeInvisibleParameterAnnotationTable(pas) ⇒ pas }

    def isVarargs: Boolean = ACC_VARARGS element_of accessFlags

    def isSynchronized: Boolean = ACC_SYNCHRONIZED element_of accessFlags

    def isBridge: Boolean = ACC_BRIDGE element_of accessFlags

    def isNative: Boolean = ACC_NATIVE element_of accessFlags

    def isStrict: Boolean = ACC_STRICT element_of accessFlags

    def isAbstract: Boolean = ACC_ABSTRACT element_of accessFlags

    /**
     * This method's implementation (if it is not abstract).
     */
    def body: Option[Code] = attributes collectFirst { case c: Code ⇒ c }

    /**
     * Each method optionally defines a method type signature.
     */
    def methodTypeSignature: Option[MethodTypeSignature] =
        attributes collectFirst { case s: MethodTypeSignature ⇒ s }

    def exceptionTable: Option[ExceptionTable] =
        attributes collectFirst { case et: ExceptionTable ⇒ et }

    //
    //
    // SUPPORT FOR SPECIAL REPRESENTATIONS
    //
    //

    import AccessFlagsContexts.METHOD

    def toXML =
        <method
			name={ name }>
			{ descriptor.toXML }
			<flags>{ AccessFlagsIterator(accessFlags, METHOD) map(_.toXML) }</flags>
			<attributes>{ for (attribute ← attributes) yield attribute.toXML }</attributes>
		</method>

    /**
     * Structure of a class fact:
     * <pre>
     * method(
     * 		classFileKey : Atom  // FOREIGN KEY
     * 		methodKey : Atom // PRIMARY KEY
     * 		name : Atom
     * 		methodDescriptor : Term
     * 		visibility : Atom
     * 		abstract : Term
     * 		final : Term
     * 		static : Term
     * 		synchronized : Term
     * 		strict : Term
     * 		native : Term
     * 		varargs : Term
     * 		bridge : Term
     * 		synthetic : Term
     * 		deprecated : Term
     * )
     * </pre>
     */
    def toProlog[F, T, A <: T](
        factory: PrologTermFactory[F, T, A],
        classFileKeyAtom: A): List[F] = {

        import factory._

        var facts: List[F] = Nil

        val key = KeyAtom("m_")

        for (attribute ← attributes) {
            facts = (attribute match {
                case aa: AnnotationTable           ⇒ aa.toProlog(factory, key)
                case paa: ParameterAnnotationTable ⇒ paa.toProlog(factory, key)
                case ea: ExceptionTable            ⇒ ea.toProlog(factory, key)
                case ada: ElementValue             ⇒ ada.toProlog(factory, key)
                case ca: Code                      ⇒ ca.toProlog(factory, key)
                case _                             ⇒ Nil
            }) ::: facts
        }

        Fact(
            "method", // functor
            classFileKeyAtom,
            key,
            TextAtom(name),
            descriptor.toProlog(factory),
            VisibilityAtom(accessFlags, METHOD),
            AbstractTerm(accessFlags),
            FinalTerm(accessFlags),
            StaticTerm(accessFlags),
            SynchronizedTerm(accessFlags),
            StrictTerm(accessFlags),
            NativeTerm(accessFlags),
            VarargsTerm(accessFlags),
            BridgeTerm(accessFlags),
            SyntheticTerm(accessFlags, attributes),
            DeprecatedTerm(attributes)
        ) :: facts

    }

}
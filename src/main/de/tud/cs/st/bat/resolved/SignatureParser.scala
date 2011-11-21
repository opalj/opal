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

import scala.util.parsing.combinator._

/**
 * A parser to parse signature attributes.
 *
 * @author Michael Eichberg
 */
object SignatureParser extends RegexParsers {

    // TODO evaluate if the (fieldType/...)Signature parsers are created over and over again....

    def parseClassSignature(signature: String): ClassSignature = {
        parseAll(_ClassSignature, signature).get
    }

    def parseFieldTypeSignature(signature: String): FieldTypeSignature = {

        parseAll(_FieldTypeSignature, signature).get
    }

    def parseMethodTypeSignature(signature: String): MethodTypeSignature = {
        parseAll(_MethodTypeSignature, signature).get
    }

    //
    // The methods to parse the code. The methods which create the parsers
    // start with an underscore to make them easily distinguishable from
    // the DataStructure they parse/create.
    //

    protected def _ClassSignature: Parser[ClassSignature] =
        opt(_FormalTypeParameters) ~ _SuperclassSignature ~ rep(_SuperinterfaceSignature) ^^ {
            case ftps ~ scs ~ siss ⇒ ClassSignature(ftps, scs, siss)
        }

    protected def _FieldTypeSignature: Parser[FieldTypeSignature] =
        _ClassTypeSignature | _ArrayTypeSignature | _TypeVariableSignature

    protected def _MethodTypeSignature: Parser[MethodTypeSignature] =
        opt(_FormalTypeParameters) ~ ("(" ~> rep(_TypeSignature) <~ ")") ~ _ReturnType ~ rep(_ThrowsSignature) ^^ {
            case ftps ~ psts ~ rt ~ tss ⇒ MethodTypeSignature(ftps, psts, rt, tss)
        }

    protected def _Identifier: Parser[String] = """[^.;\[\]/\<>\:]*+""".r

    protected def _FormalTypeParameters: Parser[List[FormalTypeParameter]] =
        "<" ~> rep1(_FormalTypeParameter) <~ ">"

    protected def _FormalTypeParameter: Parser[FormalTypeParameter] =
        _Identifier ~ _ClassBound ~ opt(_InterfaceBound) ^^ {
            case id ~ cb ~ ib ⇒ FormalTypeParameter(id, cb, ib)
        }

    protected def _ClassBound: Parser[Option[FieldTypeSignature]] =
        ":" ~> opt(_FieldTypeSignature)

    protected def _InterfaceBound: Parser[FieldTypeSignature] =
        ":" ~> _FieldTypeSignature

    protected def _SuperclassSignature: Parser[ClassTypeSignature] =
        _ClassTypeSignature

    protected def _SuperinterfaceSignature: Parser[ClassTypeSignature] =
        _ClassTypeSignature

    /**
     * '''From the Specification'''
     *
     * A class type signature gives complete type information for a class or
     * interface type. The class type signature must be formulated such that
     * it can be reliably mapped to the binary name of the class it denotes
     * by erasing any type arguments and converting each ‘.’ character in
     * the signature to a ‘$’ character.
     */
    protected def _ClassTypeSignature: Parser[ClassTypeSignature] =
        "L" ~> opt(_PackageSpecifier) ~ _SimpleClassTypeSignature ~ rep(_ClassTypeSignatureSuffix) <~ ";" ^^ {
            case ps ~ scts ~ ctsss ⇒ ClassTypeSignature(ps, scts, ctsss)
        }

    //protected def _PackageSpecifier: Parser[String] = (_Identifier ~ ("/" ~> rep(_PackageSpecifier)))^^{case id ~ rest => id + "/"+ rest.toString}
    protected def _PackageSpecifier: Parser[String] = """[^.;\[\]\<>\:]*+""".r // the same as an identifer, but "/" is treated as a normal symbol.

    protected def _SimpleClassTypeSignature: Parser[SimpleClassTypeSignature] =
        _Identifier ~ opt(_TypeArguments) ^^ {
            case id ~ tas ⇒ SimpleClassTypeSignature(id, tas)
        }

    protected def _ClassTypeSignatureSuffix: Parser[SimpleClassTypeSignature] =
        "." ~> _SimpleClassTypeSignature

    protected def _TypeVariableSignature: Parser[TypeVariableSignature] =
        ("T" ~> _Identifier <~ ";") ^^ {
            TypeVariableSignature(_)
        }

    protected def _TypeArguments: Parser[List[TypeArgument]] =
        "<" ~> rep1(_TypeArgument) <~ ">"

    protected def _TypeArgument: Parser[TypeArgument] =
        (opt(_WildcardIndicator) ~ _FieldTypeSignature) ^^ { case wi ~ fts ⇒ ProperTypeArgument(wi, fts) } |
            ("*" ^^ { _ ⇒ StarTypeArgument })

    protected def _WildcardIndicator: Parser[WildcardIndicator] =
        "+" ^^ { _ ⇒ PlusWildcardIndicator } |
            "-" ^^ { _ ⇒ MinusWildcardIndicator }

    protected def _ArrayTypeSignature: Parser[ArrayTypeSignature] =
        "[" ~> _TypeSignature ^^ {
            ArrayTypeSignature(_)
        }

    protected def _TypeSignature: Parser[TypeSignature] =
        _FieldTypeSignature | _BaseType

    protected def _ThrowsSignature: Parser[ThrowsSignature] =
        "^" ~> (_ClassTypeSignature | _TypeVariableSignature)

    protected def _BaseType: Parser[BaseType] =
        "B" ^^ (_ ⇒ ByteType()) |
            "C" ^^ (_ ⇒ CharType()) |
            "D" ^^ (_ ⇒ DoubleType()) |
            "F" ^^ (_ ⇒ FloatType()) |
            "I" ^^ (_ ⇒ IntegerType()) |
            "J" ^^ (_ ⇒ LongType()) |
            "S" ^^ (_ ⇒ ShortType()) |
            "Z" ^^ (_ ⇒ BooleanType())

    protected def _ReturnType: Parser[ReturnTypeSignature] =
        _TypeSignature |
            "V" ^^ (_ ⇒ VoidType())

}





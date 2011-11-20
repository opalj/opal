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

import de.tud.cs.st.prolog.{ GroundTerm, Atom, Fact }

import scala.util.parsing.combinator._

/**
 * Representation of a Signature.
 *
 * @author Michael Eichberg
 */
sealed trait Signature {
}

case class ClassSignature(
    formalTypeParameters: Seq[FormalTypeParameter],
    superClassSignature: ClassTypeSignature,
    superInterfaceSignature: Seq[ClassTypeSignature]) extends Signature {

}

case class MethodTypeSignature extends Signature {

}

trait FieldTypeSignature extends Signature {

}

case class ClassTypeSignature extends FieldTypeSignature {

}

case class ArrayTypeSignature extends FieldTypeSignature {

}

case class TypeVariableSignature extends FieldTypeSignature {

}

case class FormalTypeParameter {

}

object SignatureParser extends RegexParsers {

  def ClassSignature(signature: String): ClassSignature = {
    // SignatureParser.parseAll(SignatureParser.ClassSignature, signature)
    // new ClassSignature(null, null, null)
    null
  }

  def FieldTypeSignature(signature: String): FieldTypeSignature = {
    null
  }

  def MethodTypeSignature(signature: String): MethodTypeSignature = {
    null
  }

  //
  // The primary parser methods
  //

  def ClassSignature: Parser[Any] = opt(FormalTypeParameters) ~ SuperclassSignature ~ rep(SuperinterfaceSignature)

  def FieldTypeSignature: Parser[Any] = ClassTypeSignature | ArrayTypeSignature | TypeVariableSignature

  def MethodTypeSignature: Parser[Any] = opt(FormalTypeParameters) ~ "(" ~ rep(TypeSignature) ~ ")" ~ ReturnType ~ rep(ThrowsSignature)

  //
  // Helper methods
  //

  protected def Identifier: Parser[Any] = """[^.;\[\]\<>\:]*+""".r // e.g., """[a-zA-Z_]\w*""".r

  protected def FormalTypeParameters: Parser[Any] = "<" ~ rep1(FormalTypeParameter) ~ ">"

  protected def FormalTypeParameter: Parser[Any] = Identifier ~ ClassBound ~ opt(InterfaceBound)

  protected def ClassBound: Parser[Any] = ":" ~ opt(FieldTypeSignature)

  protected def InterfaceBound: Parser[Any] = ":" ~ FieldTypeSignature

  protected def SuperclassSignature: Parser[Any] = ClassTypeSignature

  protected def SuperinterfaceSignature: Parser[Any] = ClassTypeSignature

  protected def ClassTypeSignature: Parser[Any] = "L" ~ opt(PackageSpecifier) ~ SimpleClassTypeSignature ~ rep(ClassTypeSignatureSuffix) ~ ";"

  protected def PackageSpecifier: Parser[Any] = Identifier ~ "/" ~ rep(PackageSpecifier)

  protected def SimpleClassTypeSignature: Parser[Any] = Identifier ~ opt(TypeArguments)

  protected def ClassTypeSignatureSuffix: Parser[Any] = "." ~ SimpleClassTypeSignature

  protected def TypeVariableSignature: Parser[Any] = "T" ~ Identifier ~ ";"

  protected def TypeArguments: Parser[Any] = "<" ~ rep1(TypeArgument) ~ ">"

  protected def TypeArgument: Parser[Any] = (opt(WildcardIndicator) ~ FieldTypeSignature) | "*"

  protected def WildcardIndicator: Parser[Any] = "+" | "-"

  protected def ArrayTypeSignature: Parser[Any] = "[" ~ TypeSignature

  protected def TypeSignature: Parser[Any] = FieldTypeSignature | BaseType

  protected def BaseType: Parser[Any] =
    "B" /*=>ByteType()*/ |
      "C" /*=>CharType()*/ |
      "D" /*=>DoubleType()*/ |
      "F" /*=>FloatType()*/ |
      "I" /*=>IntegerType()*/ |
      "J" /*=>LongType()*/ |
      "S" /*=>ShortType()*/ |
      "Z" /*=>BooleanType() */

  protected def ReturnType: Parser[Any] = TypeSignature | VoidType

  protected def VoidType: Parser[Any] = "V"

  protected def ThrowsSignature: Parser[Any] = "^" ~ (ClassTypeSignature | TypeVariableSignature)

}





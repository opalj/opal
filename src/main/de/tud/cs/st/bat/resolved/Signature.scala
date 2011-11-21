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

/**
 * Represents a Java signature.
 *
 * @author Michael Eichberg
 */
sealed trait PrimarySignature {
}

case class ClassSignature(
        formalTypeParameters: Option[List[FormalTypeParameter]],
        superClassSignature: ClassTypeSignature,
        superInterfaceSignature: List[ClassTypeSignature]) extends PrimarySignature {

}

case class MethodTypeSignature(
        formalTypeParameters: Option[List[FormalTypeParameter]],
        parametersTypeSignatures: List[TypeSignature],
        returnType: ReturnTypeSignature,
        throwsSignature: List[ThrowsSignature]) extends PrimarySignature {

}

sealed trait ReturnTypeSignature

// TODO consider mixing in the trait "ReturnTypeSignature" in VoidTypeSignature
case object VoidTypeSignature extends ReturnTypeSignature

trait TypeSignature extends ReturnTypeSignature {

}
// TODO consider mixing in the TypeSignature trait in BaseType
case class BaseTypeSignature(baseType: BaseType) extends TypeSignature {

}
// TODO Implement sharing - i.e., we do not create BaseTypeSignatures over and over again...

trait FieldTypeSignature extends PrimarySignature with TypeSignature {

}

case class ArrayTypeSignature(typeSignature: TypeSignature) extends FieldTypeSignature {

}

trait ThrowsSignature

case class ClassTypeSignature(
        packageIdentifier: Option[String],
        simpleClassTypeSignature: SimpleClassTypeSignature,
        classTypeSignatureSuffix: List[SimpleClassTypeSignature]) extends FieldTypeSignature with ThrowsSignature {

}

case class SimpleClassTypeSignature(
    simpleName: String,
    typeArguments: Option[List[TypeArgument]])

case class TypeVariableSignature(identifier: String) extends FieldTypeSignature with ThrowsSignature {

}

case class FormalTypeParameter(
        identifier: String,
        classBound: Option[FieldTypeSignature],
        interfaceBound: Option[FieldTypeSignature]) {

}

trait TypeArgument

case class ProperTypeArgument(wildcardIndicator: Option[WildcardIndicator], fieldTypeSignature: FieldTypeSignature) extends TypeArgument

sealed trait WildcardIndicator
case object PlusWildcardIndicator extends WildcardIndicator // TODO find better name!
case object MinusWildcardIndicator extends WildcardIndicator // TODO find better name

case object StarTypeArgument extends TypeArgument

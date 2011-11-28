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

trait ReturnTypeSignature {

    //getTypes

}

trait TypeSignature extends ReturnTypeSignature {

}

trait ThrowsSignature {

}

/**
 * The common super trait of those classes that represent attribute-level signatures as defined
 * in the JVM specification.
 */
sealed trait Signature

case class ClassSignature(formalTypeParameters: Option[List[FormalTypeParameter]],
                          superClassSignature: ClassTypeSignature,
                          superInterfaceSignature: List[ClassTypeSignature])
        extends Signature
        with SignatureAttribute {

}

case class MethodTypeSignature(formalTypeParameters: Option[List[FormalTypeParameter]],
                               parametersTypeSignatures: List[TypeSignature],
                               returnType: ReturnTypeSignature,
                               throwsSignature: List[ThrowsSignature])
        extends Signature
        with SignatureAttribute {

}

trait FieldTypeSignature extends Signature with TypeSignature with SignatureAttribute {

}

case class ArrayTypeSignature(typeSignature: TypeSignature)
        extends FieldTypeSignature {

}

case class ClassTypeSignature(packageIdentifier: Option[String],
                              simpleClassTypeSignature: SimpleClassTypeSignature,
                              classTypeSignatureSuffix: List[SimpleClassTypeSignature])
        extends FieldTypeSignature
        with ThrowsSignature {

    def referredTypes: Iterable[Type] = new Iterable[Type] {
        // TODO implement
        def iterator: Iterator[Type] = new Iterator[Type] {
            def next: Type = null
            def hasNext: Boolean = false
        }
    }
}

case class TypeVariableSignature(identifier: String)
        extends FieldTypeSignature
        with ThrowsSignature {

}

case class SimpleClassTypeSignature(simpleName: String,
                                    typeArguments: Option[List[TypeArgument]])

case class FormalTypeParameter(identifier: String,
                               classBound: Option[FieldTypeSignature],
                               interfaceBound: Option[FieldTypeSignature]) {

}

trait TypeArgument {

}

case class ProperTypeArgument(varianceIndicator: Option[VarianceIndicator],
                              fieldTypeSignature: FieldTypeSignature)
        extends TypeArgument {

}

/**
 * Indicates a TypeArgument's variance.
 */
sealed trait VarianceIndicator
/**
 * If you have declaration such as <? extends Entry> then the "? extends" part
 * is represented by the CovariantIndicator.
 */
case object CovariantIndicator extends VarianceIndicator
/**
 * A declaration such as <? super Entry> is represented in class file signatures
 * by the ContravariantIndicator ("? super") and a FieldTypeSignature.
 */
case object ContravariantIndicator extends VarianceIndicator

/**
 * If a type argument is not further specified (e.g. List<?> l = …) then the
 * type argument "?" is represented by this object.
 */
case object Wildcard extends TypeArgument

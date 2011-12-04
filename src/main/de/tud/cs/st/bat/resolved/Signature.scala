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

trait SignatureElement {
    def accept[T](sv: SignatureVisitor[T]): T
}

trait SignatureVisitor[T] {
    def visit(cs: ClassSignature): T
    def visit(mts: MethodTypeSignature): T
    def visit(cts: ClassTypeSignature): T
    def visit(ats: ArrayTypeSignature): T
    def visit(tvs: TypeVariableSignature): T
    def visit(scts: SimpleClassTypeSignature): T
    def visit(ftp: FormalTypeParameter): T
    def visit(pta: ProperTypeArgument): T
    def visit(cvi: CovariantIndicator): T
    def visit(cvi: ContravariantIndicator): T
    def visit(wc: Wildcard): T
    def visit(bt: BooleanType): T
    def visit(bt: ByteType): T
    def visit(it: IntegerType): T
    def visit(lt: LongType): T
    def visit(ct: CharType): T
    def visit(st: ShortType): T
    def visit(ft: FloatType): T
    def visit(dt: DoubleType): T
    def visit(vt: VoidType): T
}
trait TraversingVisitor extends SignatureVisitor[Unit] {

    def visit(cs: ClassSignature) {
        cs.formalTypeParameters foreach (_.foreach(_.accept(this)))
        cs.superClassSignature.accept(this)
        cs.superInterfacesSignature.foreach(_.accept(this))
    }

    def visit(mts: MethodTypeSignature) {
        mts.formalTypeParameters.foreach(_.foreach(_.accept(this)))
        mts.parametersTypeSignatures.foreach(_.accept(this))
        mts.returnTypeSignature.accept(this)
        mts.throwsSignature.foreach(_.accept(this))
    }

    def visit(cts: ClassTypeSignature) {
        cts.simpleClassTypeSignature.accept(this)
        cts.classTypeSignatureSuffix.foreach(_.accept(this))
    }

    def visit(ats: ArrayTypeSignature) {
        ats.typeSignature.accept(this)
    }

    def visit(tvs: TypeVariableSignature) { /* Leadnode */ }

    def visit(scts: SimpleClassTypeSignature) {
        scts.typeArguments.foreach(_.foreach(_.accept(this)))
    }

    def visit(ftp: FormalTypeParameter) {
        ftp.classBound.foreach(_.accept(this))
        ftp.interfaceBound.foreach(_.accept(this))
    }

    def visit(pta: ProperTypeArgument) {
        pta.varianceIndicator.foreach(_.accept(this))
        pta.fieldTypeSignature.accept(this)
    }

    def visit(cvi: CovariantIndicator) { /* Leafnode */ }

    def visit(cvi: ContravariantIndicator) { /* Leafnode */ }

    def visit(wc: Wildcard) { /* Leafnode */ }

    def visit(bt: BooleanType) { /* Leafnode */ }

    def visit(bt: ByteType) { /* Leafnode */ }

    def visit(it: IntegerType) { /* Leafnode */ }

    def visit(lt: LongType) { /* Leafnode */ }

    def visit(ct: CharType) { /* Leafnode */ }

    def visit(st: ShortType) { /* Leafnode */ }

    def visit(ft: FloatType) { /* Leafnode */ }

    def visit(dt: DoubleType) { /* Leafnode */ }

    def visit(vt: VoidType) { /* Leafnode */ }

}

/**
 * Traverses a signature and calls for each Type the given method.
 *
 * '''Thread Safety'''
 * This class is thread-safe and reusable. I.e., you can use one instance
 * of this visitor to simultaneously process multiple signatures. In this
 * case, however, the given function f also has to be thread safe.
 */
class TypesVisitor(val f: Type ⇒ Unit) extends TraversingVisitor {
    override def visit(cts: ClassTypeSignature) { f(cts.objectType); super.visit(cts) }
    override def visit(bt: BooleanType) { f(bt) }
    override def visit(bt: ByteType) { f(bt) }
    override def visit(it: IntegerType) { f(it) }
    override def visit(lt: LongType) { f(lt) }
    override def visit(ct: CharType) { f(ct) }
    override def visit(st: ShortType) { f(st) }
    override def visit(ft: FloatType) { f(ft) }
    override def visit(dt: DoubleType) { f(dt) }
    override def visit(vt: VoidType) { f(vt) }
}

trait ReturnTypeSignature extends SignatureElement {
    // EMPTY
}

trait TypeSignature extends ReturnTypeSignature {
    // EMPTY
}

sealed trait ThrowsSignature extends SignatureElement {
    // EMPTY
}

/**
 * The common super trait of those classes that represent attribute-level signatures as defined
 * in the JVM specification.
 */
sealed trait Signature extends SignatureElement

case class ClassSignature(formalTypeParameters: Option[List[FormalTypeParameter]],
                          superClassSignature: ClassTypeSignature,
                          superInterfacesSignature: List[ClassTypeSignature])
        extends Signature
        with SignatureAttribute {

    def accept[T](sv: SignatureVisitor[T]) = sv.visit(this)
}

case class MethodTypeSignature(formalTypeParameters: Option[List[FormalTypeParameter]],
                               parametersTypeSignatures: List[TypeSignature],
                               returnTypeSignature: ReturnTypeSignature,
                               throwsSignature: List[ThrowsSignature])
        extends Signature
        with SignatureAttribute {

    def accept[T](sv: SignatureVisitor[T]) = sv.visit(this)
}

trait FieldTypeSignature
        extends Signature
        with TypeSignature
        with SignatureAttribute {

}

case class ArrayTypeSignature(typeSignature: TypeSignature)
        extends FieldTypeSignature {

    def accept[T](sv: SignatureVisitor[T]) = sv.visit(this)
}

case class ClassTypeSignature(packageIdentifier: Option[String],
                              simpleClassTypeSignature: SimpleClassTypeSignature,
                              classTypeSignatureSuffix: List[SimpleClassTypeSignature])
        extends FieldTypeSignature
        with ThrowsSignature {

    def objectType: ObjectType = {
        val className: java.lang.StringBuilder = new java.lang.StringBuilder(packageIdentifier.getOrElse(""))
        className.append(simpleClassTypeSignature.simpleName)
        classTypeSignatureSuffix.foreach(
            scts ⇒ {
                className.append('$')
                className.append(scts.simpleName)
            })

        ObjectType(className.toString)
    }

    def accept[T](sv: SignatureVisitor[T]) = sv.visit(this)
}

case class TypeVariableSignature(identifier: String)
        extends FieldTypeSignature
        with ThrowsSignature {

    def accept[T](sv: SignatureVisitor[T]) = sv.visit(this)
}

case class SimpleClassTypeSignature(simpleName: String,
                                    typeArguments: Option[List[TypeArgument]]) {

    def accept[T](sv: SignatureVisitor[T]) = sv.visit(this)
}

case class FormalTypeParameter(identifier: String,
                               classBound: Option[FieldTypeSignature],
                               interfaceBound: Option[FieldTypeSignature]) {

    def accept[T](sv: SignatureVisitor[T]) = sv.visit(this)
}

sealed trait TypeArgument extends SignatureElement {
}

case class ProperTypeArgument(varianceIndicator: Option[VarianceIndicator],
                              fieldTypeSignature: FieldTypeSignature)
        extends TypeArgument {
    def accept[T](sv: SignatureVisitor[T]) = sv.visit(this)
}

/**
 * Indicates a TypeArgument's variance.
 */
sealed trait VarianceIndicator extends SignatureElement {
    // EMPTY
}
/**
 * If you have declaration such as <? extends Entry> then the "? extends" part
 * is represented by the CovariantIndicator.
 */
sealed trait CovariantIndicator extends VarianceIndicator {
    def accept[T](sv: SignatureVisitor[T]) = sv.visit(this)
}
final case object CovariantIndicator extends CovariantIndicator

/**
 * A declaration such as <? super Entry> is represented in class file signatures
 * by the ContravariantIndicator ("? super") and a FieldTypeSignature.
 */
sealed trait ContravariantIndicator extends VarianceIndicator {
    def accept[T](sv: SignatureVisitor[T]) = sv.visit(this)
}
final case object ContravariantIndicator extends ContravariantIndicator

/**
 * If a type argument is not further specified (e.g. List<?> l = …) then the
 * type argument "?" is represented by this object.
 */
sealed trait Wildcard extends TypeArgument {
    def accept[T](sv: SignatureVisitor[T]) = sv.visit(this)
}
final case object Wildcard extends Wildcard

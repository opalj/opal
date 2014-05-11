/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2014
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
package org.opalj
package br

/**
 * @author Michael Eichberg
 */
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

    override def visit(cs: ClassSignature) {
        cs.formalTypeParameters foreach (_.foreach(_.accept(this)))
        cs.superClassSignature.accept(this)
        cs.superInterfacesSignature.foreach(_.accept(this))
    }

    override def visit(mts: MethodTypeSignature) {
        mts.formalTypeParameters.foreach(_.foreach(_.accept(this)))
        mts.parametersTypeSignatures.foreach(_.accept(this))
        mts.returnTypeSignature.accept(this)
        mts.throwsSignature.foreach(_.accept(this))
    }

    override def visit(cts: ClassTypeSignature) {
        cts.simpleClassTypeSignature.accept(this)
        cts.classTypeSignatureSuffix.foreach(_.accept(this))
    }

    override def visit(ats: ArrayTypeSignature) {
        ats.typeSignature.accept(this)
    }

    override def visit(tvs: TypeVariableSignature) { /* Leafnode */ }

    override def visit(scts: SimpleClassTypeSignature) {
        scts.typeArguments.foreach(_.foreach(_.accept(this)))
    }

    override def visit(ftp: FormalTypeParameter) {
        ftp.classBound.foreach(_.accept(this))
        ftp.interfaceBound.foreach(_.accept(this))
    }

    override def visit(pta: ProperTypeArgument) {
        pta.varianceIndicator.foreach(_.accept(this))
        pta.fieldTypeSignature.accept(this)
    }

    override def visit(cvi: CovariantIndicator) { /* Leafnode */ }

    override def visit(cvi: ContravariantIndicator) { /* Leafnode */ }

    override def visit(wc: Wildcard) { /* Leafnode */ }

    override def visit(bt: BooleanType) { /* Leafnode */ }

    override def visit(bt: ByteType) { /* Leafnode */ }

    override def visit(it: IntegerType) { /* Leafnode */ }

    override def visit(lt: LongType) { /* Leafnode */ }

    override def visit(ct: CharType) { /* Leafnode */ }

    override def visit(st: ShortType) { /* Leafnode */ }

    override def visit(ft: FloatType) { /* Leafnode */ }

    override def visit(dt: DoubleType) { /* Leafnode */ }

    override def visit(vt: VoidType) { /* Leafnode */ }

}

/**
 * Traverses a signature and calls for each `Type` the given method.
 *
 * ==Thread Safety==
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

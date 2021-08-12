/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br

/**
 * Implements a visitor for type signatures.
 *
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
/**
 * This visitor's `visit` methods completely traverse all elements of a type signature.
 */
trait TraversingVisitor extends SignatureVisitor[Unit] {

    override def visit(cs: ClassSignature): Unit = {
        cs.formalTypeParameters.foreach(_.accept(this))
        cs.superClassSignature.accept(this)
        cs.superInterfacesSignature.foreach(_.accept(this))
    }

    override def visit(mts: MethodTypeSignature): Unit = {
        mts.formalTypeParameters.foreach(_.accept(this))
        mts.parametersTypeSignatures.foreach(_.accept(this))
        mts.returnTypeSignature.accept(this)
        mts.throwsSignature.foreach(_.accept(this))
    }

    override def visit(cts: ClassTypeSignature): Unit = {
        cts.simpleClassTypeSignature.accept(this)
        cts.classTypeSignatureSuffix.foreach(_.accept(this))
    }

    override def visit(ats: ArrayTypeSignature): Unit = {
        ats.typeSignature.accept(this)
    }

    override def visit(tvs: TypeVariableSignature): Unit = { /* Leafnode */ }

    override def visit(scts: SimpleClassTypeSignature): Unit = {
        scts.typeArguments.foreach(_.accept(this))
    }

    override def visit(ftp: FormalTypeParameter): Unit = {
        ftp.classBound.foreach(_.accept(this))
        ftp.interfaceBound.foreach(_.accept(this))
    }

    override def visit(pta: ProperTypeArgument): Unit = {
        pta.varianceIndicator.foreach(_.accept(this))
        pta.fieldTypeSignature.accept(this)
    }

    override def visit(cvi: CovariantIndicator): Unit = { /* Leafnode */ }

    override def visit(cvi: ContravariantIndicator): Unit = { /* Leafnode */ }

    override def visit(wc: Wildcard): Unit = { /* Leafnode */ }

    override def visit(bt: BooleanType): Unit = { /* Leafnode */ }

    override def visit(bt: ByteType): Unit = { /* Leafnode */ }

    override def visit(it: IntegerType): Unit = { /* Leafnode */ }

    override def visit(lt: LongType): Unit = { /* Leafnode */ }

    override def visit(ct: CharType): Unit = { /* Leafnode */ }

    override def visit(st: ShortType): Unit = { /* Leafnode */ }

    override def visit(ft: FloatType): Unit = { /* Leafnode */ }

    override def visit(dt: DoubleType): Unit = { /* Leafnode */ }

    override def visit(vt: VoidType): Unit = { /* Leafnode */ }

}

/**
 * Traverses a signature and calls for each `Type` the given method.
 *
 * ==Thread Safety==
 * This class is thread-safe and reusable. I.e., you can use one instance
 * of this visitor to simultaneously process multiple signatures. In this
 * case, however, the given function `f` also has to be thread safe or you have
 * to use different functions.
 */
class TypesVisitor(val f: Type => Unit) extends TraversingVisitor {

    override def visit(cts: ClassTypeSignature): Unit = {
        f(cts.objectType)
        super.visit(cts)
    }

    override def visit(bt: BooleanType): Unit = { f(bt) }

    override def visit(bt: ByteType): Unit = { f(bt) }

    override def visit(it: IntegerType): Unit = { f(it) }

    override def visit(lt: LongType): Unit = { f(lt) }

    override def visit(ct: CharType): Unit = { f(ct) }

    override def visit(st: ShortType): Unit = { f(st) }

    override def visit(ft: FloatType): Unit = { f(ft) }

    override def visit(dt: DoubleType): Unit = { f(dt) }

    override def visit(vt: VoidType): Unit = { f(vt) }
}

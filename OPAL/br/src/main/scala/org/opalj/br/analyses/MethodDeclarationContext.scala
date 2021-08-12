/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package analyses

import org.opalj.bi.VisibilityModifier
import org.opalj.bi.ACC_PUBLIC
import org.opalj.bi.ACC_PROTECTED
import org.opalj.bi.ACC_PRIVATE

/**
 * Encapsulates the information about a '''non-abstract''', '''non-static''' method which is
 * '''not an initializer''' (`<(cl)init>`) and which is required when determining potential call
 * targets.
 *
 * @note    A class may have -- w.r.t. a given package name -- at most one package
 *          visible method which has a specific name and descriptor combination.
 *          For methods with protected or public visibility a class always has at
 *          most one method with a given name and descriptor.
 *
 * @note    Equality is defined based on the name, descriptor and declaring package
 *          of a method (the concrete declaring class is not considered!).
 *
 * @author Michael Eichberg
 */
final class MethodDeclarationContext(val method: Method) extends Ordered[MethodDeclarationContext] {

    assert(!method.isStatic)
    assert(!method.isInitializer)

    def declaringClassType: ObjectType = method.declaringClassFile.thisType
    def packageName: String = declaringClassType.packageName
    def isPublic: Boolean = method.isPublic
    def name: String = method.name
    def descriptor: MethodDescriptor = method.descriptor

    override def equals(other: Any): Boolean = {
        other match {
            case that: MethodDeclarationContext =>
                this.packageName == that.packageName && {
                    val thisMethod = this.method
                    val thatMethod = that.method
                    thisMethod.name == thatMethod.name &&
                        thisMethod.descriptor == thatMethod.descriptor
                }
            case _ =>
                false
        }
    }

    /**
     * The hash code is equal to the ``"hashCode of the descriptor of the underlying method"
     * * 113 + "the hashCode of the package of the declaring class"``.
     */
    override def hashCode: Int = method.descriptor.hashCode * 113 + packageName.hashCode()

    override def toString: String = {
        val packageName = if (this.packageName == "") "<default>" else this.packageName
        s"MethodDeclarationContext($packageName, ${method.signatureToJava()})"
    }

    /**
     * Compares this `MethodDeclarationContext` with the given one. Defines a total order w.r.t.
     * the name, descriptor and declaring package of a method. (The declaring class is not
     * considered and, therefore, two `MethodDeclarationContext`s may be considered equal
     * even though the underlying method is not the same one.)
     */
    def compare(that: MethodDeclarationContext): Int = {
        val result = this.method compare that.method
        if (result == 0)
            this.packageName compareTo that.packageName
        else
            result
    }

    def compareWithPublicMethod(thatMethod: Method): Int = {
        assert(thatMethod.isPublic, s"${thatMethod.toJava} is not public")
        this.method compare thatMethod
    }

    /**
     * Compares this method (declaration context) with an (implicit) method which has the
     * given `name` and `descriptor` and which is defined in the given `package`
     * unless this method is protected or public. In that case the
     * `packageName` is not compared and "0" (<=> equal) is returned.
     * Hence, this `compare` method is well suited to make a lookup for a matching
     * method declaration context in a sorted array of method declaration
     * contexts.
     */
    def compareAccessibilityAware(
        packageName: String, // only considered if name and descriptor already match...
        name:        String,
        descriptor:  MethodDescriptor
    ): Int = {
        val method = this.method
        val result = method.compare(name, descriptor)
        if (result == 0 && method.hasDefaultVisibility) {
            this.packageName compareTo packageName
        } else {
            result
        }
    }

    def compareAccessibilityAware(
        declaringClass: ObjectType,
        m:              Method
    ): Int = {
        compareAccessibilityAware(declaringClass.packageName, m.name, m.descriptor)
    }

    /**
     * Returns `true` if this method directly overrides the given method.
     *
     * (Note: indirect overriding can only be determined if all intermediate methods
     * are known; only in that case it is possible to test if, e.g., a public method
     * in package x indirectly overrides a package visible method in a package y.)
     *
     * @note    The overrides relation is reflexive as defined by the JVM specification
     *          (Section: "Overriding").
     *
     * @param   that The [[MethodDeclarationContext]] object of another method which is defined by
     *          the same class as this method or a superclass thereof. If the
     *          other method is defined by some other class with which this class
     *          is not in a sub-/supertype relation, the result is not defined.
     */
    def directlyOverrides(that: MethodDeclarationContext): Boolean = {
        // mc and ma are used as in the JVM spec
        val mc = this.method
        val ma = that.method

        mc == ma || (
            mc.name == ma.name &&
            mc.descriptor == ma.descriptor &&
            canDirectlyOverride(ma.visibilityModifier, that.packageName)
        )
    }

    /**
     * Returns true if a method with the same signature as this method that is defined in
     * the given package directly overrides this encapsultated method. This property
     * always holds if this method has public or protected visiblity. If this method
     * has package visibility, the other (implicit) method has to be defined in
     * this method's package.
     */
    def isDirectlyOverriddenBy(packageName: String): Boolean = {
        !this.method.hasDefaultVisibility || this.packageName == packageName
    }

    /**
     * Performs the accessibility check required when we need to determine
     * if this method (`mc`) overrides another method (`ma`).
     *
     * @note    This method must be defined by a class which is a subtype of the
     *          declaring class of the other method.
     */
    def canDirectlyOverride(
        visibility:  Option[VisibilityModifier],
        packageName: String
    ): Boolean = {
        visibility match {
            case Some(ACC_PUBLIC) | Some(ACC_PROTECTED) => true
            case Some(ACC_PRIVATE)                      => false
            case None                                   => this.packageName == packageName
        }
    }
}

/**
 * Definition of factory and extractor methods for [[MethodDeclarationContext]] objects.
 */
object MethodDeclarationContext {

    def apply(method: Method): MethodDeclarationContext = {
        new MethodDeclarationContext(method)
    }

    def unapply(mdc: MethodDeclarationContext): Some[Method] = Some(mdc.method)

}

/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2016
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
package analyses

import org.opalj.bi.VisibilityModifier
import org.opalj.bi.ACC_PUBLIC
import org.opalj.bi.ACC_PROTECTED
import org.opalj.bi.ACC_PRIVATE

/**
 * Encapsulates the information about  '''non-abstract''', '''non-private''', '''non-static''
 * methods which are '''not initializers''' (`<(cl)init>`) and which is required when determining
 * potential call targets.
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
final class MethodDeclarationContext(
        val method:         Method,
        val declaringClass: ObjectType
) extends Ordered[MethodDeclarationContext] {

    assert(!method.isPrivate)
    assert(!method.isStatic)
    assert(!method.isInitializer)

    def packageName = declaringClass.packageName
    def isPublic: Boolean = method.isPublic
    def name: String = method.name
    def descriptor: MethodDescriptor = method.descriptor

    override def equals(other: Any): Boolean = {
        other match {
            case that: MethodDeclarationContext ⇒
                this.packageName == that.packageName && {
                    val thisMethod = this.method
                    val thatMethod = that.method
                    thisMethod.name == thatMethod.name &&
                        thisMethod.descriptor == thatMethod.descriptor
                }
            case _ ⇒
                false
        }
    }

    override def hashCode: Int = method.hashCode * 113 + packageName.hashCode()

    override def toString: String = s"MethodDeclarationContext($packageName, ${method.toJava})"

    def compare(that: MethodDeclarationContext): Int = {
        val result = this.method compare that.method
        if (result == 0)
            this.packageName compareTo that.packageName
        else
            result
    }

    /**
     * Compares this method (declaration context) with a method which has the
     * given name and descriptor and which is defined in the given package
     * unless this method is protected or public. In that case the
     * packagename is not compared and "0" (<=> equal) is returned. I.e.,
     * this `compare` method is well suited to make a lookup for a matching
     * method declaration context in a sorted array of method declaration
     * contexts.
     */
    def compareAccessibilityAware(
        name:        String,
        descriptor:  MethodDescriptor,
        packageName: String // only considered if name and descriptor already match...
    ): Int = {
        val method = this.method
        val result = method.compare(name, descriptor)
        if (result == 0 && method.hasDefaultVisibility) {
            this.packageName compareTo packageName
        } else {
            result
        }
    }

    /**
     * Returns true if this method directly overrides the given method.
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
     *          is not in a sub-/supertype relation the result is not defined.
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
     * has package visibility the other (implicit) method has to be defined in
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
            case Some(ACC_PUBLIC) | Some(ACC_PROTECTED) ⇒ true
            case None                                   ⇒ this.packageName == packageName
            case Some(ACC_PRIVATE)                      ⇒ false
        }
    }
}

/**
 * Definition of factory and extractor methods for [[MethodDeclarationContext]] objects.
 */
object MethodDeclarationContext {

    def apply(method: Method, declaringClassFile: ClassFile): MethodDeclarationContext = {
        new MethodDeclarationContext(method, declaringClassFile.thisType)
    }

    def apply(method: Method, declaringClass: ObjectType): MethodDeclarationContext = {
        new MethodDeclarationContext(method, declaringClass)
    }

    def unapply(mi: MethodDeclarationContext): Some[(Method, ObjectType)] = {
        Some((mi.method, mi.declaringClass))
    }
}

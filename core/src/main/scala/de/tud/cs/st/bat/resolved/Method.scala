/* License (BSD Style License):
 * Copyright (c) 2009 - 2013
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
 *  - Neither the name of the Software Technology Group or Technische
 *    Universität Darmstadt nor the names of its contributors may be used to
 *    endorse or promote products derived from this software without specific
 *    prior written permission.
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
package de.tud.cs.st
package bat
package resolved

/**
 * Represents a single method.
 *
 * @note Equality of methods is – by purpose – reference based. Furthermore, each method
 *      has a unique id/hash value in the range [1,Method.methodCount].
 *      This makes it, e.g., possible to use an array to associate information with
 *      methods instead of a `Map`. However, a `Map` is more efficient if you will not
 *      associate information with (nearly) all methods.
 *
 * @author Michael Eichberg
 */
class Method private (
    val id: Int, // also used as the "hashCode"
    val accessFlags: Int,
    val name: String,
    val descriptor: MethodDescriptor,
    val attributes: Attributes)
        extends ClassMember
        with UniqueID {

    override final def isMethod = true

    override final def asMethod = this

    def runtimeVisibleParameterAnnotations: Option[ParameterAnnotations] =
        attributes collectFirst { case RuntimeVisibleParameterAnnotationTable(pas) ⇒ pas }

    def runtimeInvisibleParameterAnnotations: Option[ParameterAnnotations] =
        attributes collectFirst { case RuntimeInvisibleParameterAnnotationTable(pas) ⇒ pas }

    def isVarargs: Boolean = ACC_VARARGS isElementOf accessFlags

    def isSynchronized: Boolean = ACC_SYNCHRONIZED isElementOf accessFlags

    def isBridge: Boolean = ACC_BRIDGE isElementOf accessFlags

    def isNative = ACC_NATIVE isElementOf accessFlags

    def isStrict: Boolean = ACC_STRICT isElementOf accessFlags

    def isAbstract: Boolean = ACC_ABSTRACT isElementOf accessFlags

    def returnType = descriptor.returnType

    def parameterTypes = descriptor.parameterTypes

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

    def toJava(): String = descriptor.toJava(name)

    override def hashCode: Int = id

    override def equals(other: Any): Boolean =
        other match {
            case that: AnyRef ⇒ this eq that
            case _            ⇒ false
        }

    override def toString(): String = {
        AccessFlags.toStrings(accessFlags, AccessFlagsContexts.METHOD).mkString("", " ", " ") +
            descriptor.toJava(name) +
            attributes.view.map(_.getClass().getSimpleName()).mkString(" « ", ", ", " »")
    }

}
/**
 * Defines factory and extractor methods for `Method` objects.
 */
object Method {

    private val nextId = new java.util.concurrent.atomic.AtomicInteger(0)

    def methodsCount = nextId.get

    def apply(
        accessFlags: Int,
        name: String,
        descriptor: MethodDescriptor,
        attributes: Attributes): Method = {
        new Method(
            nextId.getAndIncrement(),
            accessFlags,
            name,
            descriptor,
            attributes)
    }

    def unapply(method: Method): Option[(Int, String, MethodDescriptor, Attributes)] =
        Some((
            method.accessFlags,
            method.name,
            method.descriptor,
            method.attributes
        ))
}

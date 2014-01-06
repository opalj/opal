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
 *      has a unique id/hash value in the range [0,Method.methodsCount].
 *      This makes it, e.g., possible to use an array to associate information with
 *      methods instead of a `HashMap` or `HashTrie`. However, a `Map` is more
 *      efficient if you will not associate information with (nearly) all methods.
 *
 * @param id The unique if of this method.
 * @param accessFlags The ''access flags'' of this method. Though it is possible to
 *     directly work with the `accessFlags` field, it may be more convenient to use
 *     the respective methods (`isNative`, `isAbstract`,...) to query the access flags.
 * @param name The name of the method. The name is interned (see `String.intern()`
 *      for details).
 * @param descriptor This method's descriptor.
 * @param body The body of the method if any.
 * @param attributes This method's defined attributes. (Which attributes are available
 *      generally depends on the configuration of the class file reader. However,
 *      the `Code_Attribute` is – if it was loaded – always directly accessible by
 *      means of the `body` attribute.).
 *
 * @author Michael Eichberg
 */
final class Method private (
    val id: Int, // also used as the "hashCode"
    val accessFlags: Int,
    val name: String, // the name is interned to enable reference comparisons!
    val descriptor: MethodDescriptor,
    val body: Option[Code],
    val attributes: Attributes)
        extends ClassMember
        with UID {

    override final def isMethod = true

    override final def asMethod = this

    def runtimeVisibleParameterAnnotations: Option[ParameterAnnotations] =
        attributes collectFirst { case RuntimeVisibleParameterAnnotationTable(pas) ⇒ pas }

    def runtimeInvisibleParameterAnnotations: Option[ParameterAnnotations] =
        attributes collectFirst { case RuntimeInvisibleParameterAnnotationTable(pas) ⇒ pas }

    // This is directly supported due to its need for the resolution of signature 
    // polymorphic methods. 
    final def isNativeAndVarargs = Method.isNativeAndVarargs(accessFlags)

    final def isVarargs: Boolean = (ACC_VARARGS.mask & accessFlags) != 0

    final def isSynchronized: Boolean = (ACC_SYNCHRONIZED.mask & accessFlags) != 0

    final def isBridge: Boolean = ACC_BRIDGE isElementOf accessFlags

    final def isNative = (ACC_NATIVE.mask & accessFlags) != 0

    final def isStrict: Boolean = ACC_STRICT isElementOf accessFlags

    final def isAbstract: Boolean = (ACC_ABSTRACT.mask & accessFlags) != 0

    def returnType = descriptor.returnType

    def parameterTypes = descriptor.parameterTypes

    /**
     * Each method optionally defines a method type signature.
     */
    def methodTypeSignature: Option[MethodTypeSignature] =
        attributes collectFirst { case s: MethodTypeSignature ⇒ s }

    def exceptionTable: Option[ExceptionTable] =
        attributes collectFirst { case et: ExceptionTable ⇒ et }

    def toJava(): String = descriptor.toJava(name)

    /**
     * Defines an absolute order on `Method` instances w.r.t. their method signatures.
     * The order is defined by lexicographically comparing the names of the methods
     * and – in case that the names of both methods are identical – by comparing
     * their method descriptors.
     */
    def <(other: Method): Boolean = {
        this.name < other.name || (
            (this.name eq other.name) &&
            this.descriptor < other.descriptor)
    }

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

    private final val ACC_NATIVEAndACC_VARARGS = ACC_NATIVE.mask | ACC_VARARGS.mask

    private def isNativeAndVarargs(accessFlags: Int) =
        (accessFlags & ACC_NATIVEAndACC_VARARGS) == ACC_NATIVEAndACC_VARARGS

    private[this] val nextId = new java.util.concurrent.atomic.AtomicInteger(0)

    def methodsCount = nextId.get

    def apply(
        accessFlags: Int,
        name: String,
        descriptor: MethodDescriptor,
        attributes: Attributes): Method = {

        val (bodySeq, remainingAttributes) = attributes partition { _.isInstanceOf[Code] }
        val theBody =
            if (bodySeq.nonEmpty)
                Some(bodySeq.head.asInstanceOf[Code])
            else
                None
        new Method(
            nextId.getAndIncrement(),
            accessFlags,
            name.intern(),
            descriptor,
            theBody,
            remainingAttributes)
    }

    def unapply(method: Method): Option[(Int, String, MethodDescriptor)] =
        Some((method.accessFlags, method.name, method.descriptor))
}

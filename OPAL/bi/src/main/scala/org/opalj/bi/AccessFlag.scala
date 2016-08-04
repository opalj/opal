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
package bi

/**
 * A class, field or method declaration's access flags. An access flag (e.g., `public`
 * or `static`) is basically just a specific bit that can be combined with other
 * access flags to create an integer based bit vector that represents all
 * flags defined for a class, method or field declaration. Access flags are
 * generally context dependent and the same value means different things
 * depending on the context.
 *
 * @author Michael Eichberg
 */
sealed trait AccessFlag extends PrimitiveAccessFlagsMatcher {

    /**
     * The Java (source code) name of the access flag if it exists. E.g., Some("public"),
     * Some("native"), etc.
     */
    def javaName: Option[String]

    /**
     * The `Int` mask of this access flag as defined by the JVM specification.
     */
    override def mask: Int

    /**
     * Facilitates pattern matching against this `AccessFlag`.
     *
     * ==Example==
     * {{{
     * case ClassFile(ACC_PUBLIC(),...)
     * }}}
     *
     * To create more complex matchers, use the `&` and `!` methods.
     *
     * @return `True` iff " `this` " flag is set in the given access flags bit vector.
     */
    def unapply(accessFlags: Int): Boolean = (accessFlags & mask) == mask

    /**
     * Checks if `this` access flag is set in the given access flags bit vector.
     *
     * @note This method is just a more intuitively named alias for the [[unapply]] method.
     */
    def isSet(accessFlags: Int): Boolean = unapply(accessFlags)

}

/**
 * Common supertype of all explicit visibility modifiers/access flags.
 *
 * @author Michael Eichberg
 */
sealed trait VisibilityModifier extends AccessFlag {
    val javaName: Some[String]
}

/**
 * Defines extractor methods related to visibility modifiers.
 *
 * @author Michael Eichberg
 */
object VisibilityModifier {

    final val mask = ACC_PRIVATE.mask | ACC_PUBLIC.mask | ACC_PROTECTED.mask

    private val SOME_PUBLIC = Some(ACC_PUBLIC)
    private val SOME_PRIVATE = Some(ACC_PRIVATE)
    private val SOME_PROTECTED = Some(ACC_PROTECTED)

    /**
     * Returns the specified visibility modifier.
     *
     * @param accessFlags The access flags of a class or a member thereof.
     * @return The visibility modifier of the respective element or `None` if the
     *      element has default visibility.
     */
    def get(accessFlags: Int): Option[VisibilityModifier] =
        ((accessFlags & VisibilityModifier.mask): @scala.annotation.switch) match {
            case 1 /*ACC_PUBLIC.mask*/    ⇒ SOME_PUBLIC
            case 2 /*ACC_PRIVATE.mask*/   ⇒ SOME_PRIVATE
            case 4 /*ACC_PROTECTED.mask*/ ⇒ SOME_PROTECTED
            case _                        ⇒ None /*DEFAULT VISIBILITY*/
        }

    def unapply(accessFlags: Int): Option[VisibilityModifier] = get(accessFlags)
}

object ACC_PUBLIC extends VisibilityModifier {
    final override val javaName: Some[String] = Some("public")
    final override val mask: Int = 0x0001
    override def toString = "PUBLIC"
}

object ACC_PRIVATE extends VisibilityModifier {
    final override val javaName: Some[String] = Some("private")
    final override val mask: Int = 0x0002
    override def toString = "PRIVATE"
}

object ACC_PROTECTED extends VisibilityModifier {
    final override val javaName: Some[String] = Some("protected")
    final override val mask: Int = 0x0004
    override def toString = "PROTECTED"
}

object ACC_STATIC extends AccessFlag {
    final override val javaName: Some[String] = Some("static")
    final override val mask = 0x0008
    override def toString = "STATIC"
}

object ACC_FINAL extends AccessFlag {
    final override val javaName: Some[String] = Some("final")
    final override val mask = 0x0010
    override def toString = "FINAL"
}

object ACC_SUPER extends AccessFlag {
    final val javaName: None.type = None
    final val mask = 0x0020
    override def toString = "SUPER"
}

object ACC_SYNCHRONIZED extends AccessFlag {
    final override val javaName: Some[String] = Some("synchronized")
    final override val mask = 0x0020
    override def toString = "SYNCHRONIZED"
}

object ACC_VOLATILE extends AccessFlag {
    final override val javaName: Some[String] = Some("volatile")
    final override val mask = 0x0040
    override def toString = "VOLATILE"
}

object ACC_BRIDGE extends AccessFlag {
    final val javaName: None.type = None
    final val mask = 0x0040
    override def toString = "BRIDGE"
}

object ACC_TRANSIENT extends AccessFlag {
    final override val javaName: Some[String] = Some("transient")
    final override val mask = 0x0080
    override def toString = "TRANSIENT"
}

object ACC_VARARGS extends AccessFlag {
    final override val javaName: None.type = None
    final override val mask = 0x0080
    override def toString = "VARARGS"
}

object ACC_NATIVE extends AccessFlag {
    final val javaName: Some[String] = Some("native")
    final val mask = 0x0100
    override def toString = "NATIVE"
}

object ACC_INTERFACE extends AccessFlag {
    // this flag modifies the semantics of a class, but it is not an additional flag
    final override val javaName: None.type = None
    final override val mask = 0x0200
    override def toString = "INTERFACE"
}

object ACC_ABSTRACT extends AccessFlag {
    final override val javaName: Some[String] = Some("abstract")
    final override val mask = 0x0400
    override def toString = "ABSTRACT"
}

object ACC_STRICT extends AccessFlag {
    final override val javaName: Some[String] = Some("strictfp")
    final val mask = 0x0800
    override def toString = "STRICT"
}

object ACC_SYNTHETIC extends AccessFlag {
    final override val javaName: None.type = None
    final override val mask = 0x1000
    override def toString = "SYNTHETIC"
}

object ACC_ANNOTATION extends AccessFlag {
    final override val javaName: None.type = None
    final override val mask = 0x2000
    override def toString = "ANNOTATION"
}

object ACC_ENUM extends AccessFlag {
    final override val javaName: None.type = None
    final override val mask = 0x4000
    override def toString = "ENUM"
}

/**
 * Identifies a class as defining a Java 9 module.
 *
 * @note From the specification: "If ACC_MODULE is set in ClassFile.access_flags, then
 * 		no other flag in `ClassFile.access_flags` may be set."
 * 		The name of the class has to be "/module-info".
 * @note '''super_class, interfaces_count, fields_count, methods_count: zero. I.e.,
 * 		a module does not have a super class.'''
 * @note Only used in combination with Java 9 modules.
 */
object ACC_MODULE extends AccessFlag {
    final override val javaName: None.type = None
    final override val mask = 0x8000 // SAME AS ACC_MANDATED (!)
    override def toString = "MODULE"
}

object ACC_MANDATED extends AccessFlag {
    final override val javaName: None.type = None
    final override val mask = 0x8000
    override def toString = "MANDATED"
}

/**
 * Indicates that any module which depends on the current module, implicitly declares a
 * dependence on the module indicated by this entry.
 */
object ACC_TRANSITIVE extends AccessFlag {
    final override val javaName = Some("public") // IMPROVE [JDK9] Check if the name "public <-> transitive" is chosen 
    final override val mask = 0x0010
    override def toString = "TRANSITIVE"
}

/**
 *
 * @note From the JVM 9 specification: "Indicates that this [inter-module] dependence is
 * 		mandatory in the static phase, i.e., at compile time, but is optional in the
 * 		dynamic phase, i.e., at run time."
 * @note Only used in combination with Java 9 modules.
 */
object ACC_STATIC_PHASE extends AccessFlag {
    final override val javaName = Some("static")
    final override val mask = 0x0020
    override def toString = "STATIC_PHASE"
}

/**
 *
 * @note From the JVM 9 specification: "Indicates that the package is concealed in the
 * 		static phase, i.e., at compile time, but is exported in the dynamic phase, i.e.,
 * 		at run time."
 * @note Only used in combination with Java 9 modules.
 */
object ACC_DYNAMIC_PHASE extends AccessFlag {
    final override val javaName = Some("dynamic")
    final override val mask = 0x0040
    override def toString = "DYNAMIC_PHASE"
}

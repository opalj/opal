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

/**
 * A class, field or method declaration's access flags. An access flag is
 * basically just a specific bit that can be combined with other
 * access flags to create an integer based bit vector that represents all
 * flags defined for a class, method or field declaration.
 *
 * @author Michael Eichberg
 */
sealed trait AccessFlag extends AccessFlagsMatcher {

    /**
     * The Java (source code) name of the access flag if it exists. E.g., Some("public"),
     * Some("native"), etc.
     */
    def javaName: Option[String]

    /**
     * The int mask of this access flag as defined by the JVM specification.
     */
    def mask: Int

    /**
     * Facilitates pattern matching against this AccessFlag.
     *
     * ==Example==
     * {{{
     * case ClassFile(ACC_PUBLIC(),...)
     * }}}
     *
     * To create more complex matchers, use the `&` and `!` methods.
     *
     * @return `True` iff all specified flags are set in the given access flags bit vector.
     */
    def unapply(accessFlags: Int): Boolean = (accessFlags & mask) == mask

    /**
     * Determines if this access flag is set in the given `access_flags` bit vector.
     * E.g., to determine if a method's static modifier is set it is sufficient
     * to call {{{ACC_STATIC isElementOf method.access_flags}}}.
     */
    def isElementOf(access_flags: Int): Boolean = (access_flags & mask) != 0

}

sealed trait VisibilityModifier extends AccessFlag

/**
 * Utility methods related to the visibility modifiers.
 *
 * @author Michael Eichberg
 */
object VisibilityModifier {

    private val mask = ACC_PRIVATE.mask | ACC_PUBLIC.mask | ACC_PROTECTED.mask

    private val SOME_PUBLIC = Some(ACC_PUBLIC)
    private val SOME_PRIVATE = Some(ACC_PRIVATE)
    private val SOME_PROTECTED = Some(ACC_PROTECTED)

    def get(accessFlags: Int): Option[VisibilityModifier] =
        ((accessFlags & VisibilityModifier.mask): @scala.annotation.switch) match {
            case 1 /*ACC_PUBLIC.mask*/    ⇒ SOME_PUBLIC
            case 2 /*ACC_PRIVATE.mask*/   ⇒ SOME_PRIVATE
            case 4 /*ACC_PROTECTED.mask*/ ⇒ SOME_PROTECTED
            case _                        ⇒ None
        }

    def unapply(accessFlags: Int): Option[VisibilityModifier] = get(accessFlags)
}

object ACC_PUBLIC extends VisibilityModifier {
    final val javaName: Option[String] = Some("public")
    final val mask: Int = 0x0001
    override def toString = "PUBLIC"
}

object ACC_PRIVATE extends VisibilityModifier {
    final val javaName: Option[String] = Some("private")
    final val mask: Int = 0x0002
    override def toString = "PRIVATE"
}

object ACC_PROTECTED extends VisibilityModifier {
    final val javaName: Option[String] = Some("protected")
    final val mask: Int = 0x0004
    override def toString = "PROTECTED"
}

object ACC_STATIC extends AccessFlag {
    final val javaName: Option[String] = Some("static")
    final val mask = 0x0008
    override def toString = "STATIC"
}

object ACC_FINAL extends AccessFlag {
    final val javaName: Option[String] = Some("final")
    final val mask = 0x0010
    override def toString = "FINAL"
}

object ACC_SUPER extends AccessFlag {
    final val javaName: Option[String] = None
    final val mask = 0x0020
    override def toString = "SUPER"
}

object ACC_SYNCHRONIZED extends AccessFlag {
    final val javaName: Option[String] = Some("synchronized")
    final val mask = 0x0020
    override def toString = "SYNCHRONIZED"
}

object ACC_VOLATILE extends AccessFlag {
    final val javaName: Option[String] = Some("volatile")
    final val mask = 0x0040
    override def toString = "VOLATILE"
}

object ACC_BRIDGE extends AccessFlag {
    final val javaName: Option[String] = None
    final val mask = 0x0040
    override def toString = "BRIDGE"
}

object ACC_TRANSIENT extends AccessFlag {
    final val javaName: Option[String] = Some("transient")
    final val mask = 0x0080
    override def toString = "TRANSIENT"
}

object ACC_VARARGS extends AccessFlag {
    final val javaName: Option[String] = None
    final val mask = 0x0080
    override def toString = "VARARGS"
}

object ACC_NATIVE extends AccessFlag {
    final val javaName: Option[String] = Some("native")
    final val mask = 0x0100
    override def toString = "NATIVE"
}

object ACC_INTERFACE extends AccessFlag {
    // this flag modifies the semantics of a class, but it is not an additional flag
    final val javaName: Option[String] = None
    final val mask = 0x0200
    override def toString = "INTERFACE"
}

object ACC_ABSTRACT extends AccessFlag {
    final val javaName: Option[String] = Some("abstract")
    final val mask = 0x0400
    override def toString = "ABSTRACT"
}

object ACC_STRICT extends AccessFlag {
    final val javaName: Option[String] = Some("strictfp")
    final val mask = 0x0800
    override def toString = "STRICT"
}

object ACC_SYNTHETIC extends AccessFlag {
    final val javaName: Option[String] = None
    final val mask = 0x1000
    override def toString = "SYNTHETIC"
}

object ACC_ANNOTATION extends AccessFlag {
    final val javaName: Option[String] = None
    final val mask = 0x2000
    override def toString = "ANNOTATION"
}

object ACC_ENUM extends AccessFlag {
    final val javaName: Option[String] = None
    final val mask = 0x4000
    override def toString = "ENUM"
}

object AccessFlags {

    def toStrings(
        accessFlags: Int,
        ctx: AccessFlagsContext,
        sep: String = ", "): Iterator[String] = {
        (
            AccessFlagsIterator(accessFlags, ctx) map { accessFlag ⇒
                accessFlag.javaName.getOrElse("["+accessFlag.toString+"]")
            }
        )
    }

}

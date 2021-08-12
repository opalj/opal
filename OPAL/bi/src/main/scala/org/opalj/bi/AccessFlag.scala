/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package bi

import scala.annotation.switch

/**
 * A class, field or method declaration's access flags. An access flag (e.g., `public`
 * or `static`) is basically just a specific bit that can be combined with other
 * access flags to create an integer based bit vector that represents all
 * flags defined for a class, method or field declaration. Access flags are
 * generally context dependent and the same value means different things
 * depending on the context.
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
 */
object VisibilityModifier {

    // partial mask: we can't define a mask w.r.t. default visibility
    private[bi] final val mask = ACC_PRIVATE.mask | ACC_PUBLIC.mask | ACC_PROTECTED.mask

    def hasDefaultVisibility(accessFlags: Int): Boolean = (accessFlags & mask) == 0

    final val SOME_PUBLIC = Some(ACC_PUBLIC)
    final val SOME_PRIVATE = Some(ACC_PRIVATE)
    final val SOME_PROTECTED = Some(ACC_PROTECTED)

    /**
     * Returns the specified visibility modifier.
     *
     * @param accessFlags The access flags of a class or a member thereof.
     * @return  The visibility modifier of the respective element or `None` if the
     *          element has default visibility.
     */
    def get(accessFlags: Int): Option[VisibilityModifier] = {
        ((accessFlags & mask): @switch) match {
            case ACC_PUBLIC.mask    => SOME_PUBLIC
            case ACC_PRIVATE.mask   => SOME_PRIVATE
            case ACC_PROTECTED.mask => SOME_PROTECTED
            case _                  => None /*DEFAULT VISIBILITY*/
        }
    }

    /**
     * `true` if `a` is at least as visible as `b`; for example, true if `a` is public and
     * `b` is just protected.
     */
    def isAtLeastAsVisibleAs(
        a: Option[VisibilityModifier],
        b: Option[VisibilityModifier]
    ): Boolean = {
        a match {
            case Some(ACC_PUBLIC)      => true
            case Some(ACC_PROTECTED)   => b.isEmpty || b.get != ACC_PUBLIC
            case None                  => b.isEmpty || b.get == ACC_PRIVATE
            case a @ Some(ACC_PRIVATE) => b == a

        }
    }

    def isLessVisibleAs(a: Option[VisibilityModifier], b: Option[VisibilityModifier]): Boolean = {
        !isAtLeastAsVisibleAs(b, a)
    }

    def unapply(accessFlags: Int): Option[VisibilityModifier] = get(accessFlags)
}

object ACC_PUBLIC extends VisibilityModifier {
    final override val javaName: Some[String] = Some("public")
    final override val mask = 0x0001
    override def toString: String = "PUBLIC"
}

object ACC_PRIVATE extends VisibilityModifier {
    final override val javaName: Some[String] = Some("private")
    final override val mask = 0x0002
    override def toString: String = "PRIVATE"
}

object ACC_PROTECTED extends VisibilityModifier {
    final override val javaName: Some[String] = Some("protected")
    final override val mask = 0x0004
    override def toString: String = "PROTECTED"
}

object ACC_STATIC extends AccessFlag {
    final override val javaName: Some[String] = Some("static")
    final override val mask = 0x0008
    override def toString: String = "STATIC"
}

object ACC_FINAL extends AccessFlag {
    final override val javaName: Some[String] = Some("final")
    final override val mask = 0x0010
    override def toString: String = "FINAL"
}

object ACC_SUPER extends AccessFlag {
    final override val javaName: None.type = None
    final override val mask = 0x0020
    override def toString: String = "SUPER"
}

object ACC_SYNCHRONIZED extends AccessFlag {
    final override val javaName: Some[String] = Some("synchronized")
    final override val mask = 0x0020
    override def toString: String = "SYNCHRONIZED"
}

object ACC_VOLATILE extends AccessFlag {
    final override val javaName: Some[String] = Some("volatile")
    final override val mask = 0x0040
    override def toString: String = "VOLATILE"
}

object ACC_BRIDGE extends AccessFlag {
    final override val javaName: None.type = None
    final override val mask = 0x0040
    override def toString: String = "BRIDGE"
}

object ACC_TRANSIENT extends AccessFlag {
    final override val javaName: Some[String] = Some("transient")
    final override val mask = 0x0080
    override def toString: String = "TRANSIENT"
}

object ACC_VARARGS extends AccessFlag {
    final override val javaName: None.type = None
    final override val mask = 0x0080
    override def toString: String = "VARARGS"
}

object ACC_NATIVE extends AccessFlag {
    final override val javaName: Some[String] = Some("native")
    final override val mask = 0x0100
    override def toString: String = "NATIVE"
}

object ACC_INTERFACE extends AccessFlag {
    // this flag modifies the semantics of a class, but it is not an additional flag
    final override val javaName: None.type = None
    final override val mask = 0x0200
    override def toString: String = "INTERFACE"
}

object ACC_ABSTRACT extends AccessFlag {
    final override val javaName: Some[String] = Some("abstract")
    final override val mask = 0x0400
    override def toString: String = "ABSTRACT"
}

object ACC_STRICT extends AccessFlag {
    final override val javaName: Some[String] = Some("strictfp")
    final val mask = 0x0800
    override def toString: String = "STRICT"
}

object ACC_SYNTHETIC extends AccessFlag {
    final override val javaName: None.type = None
    final override val mask = 0x1000
    override def toString: String = "SYNTHETIC"
}

object ACC_ANNOTATION extends AccessFlag {
    final override val javaName: None.type = None
    final override val mask = 0x2000
    override def toString: String = "ANNOTATION"
}

object ACC_ENUM extends AccessFlag {
    final override val javaName: None.type = None
    final override val mask = 0x4000
    override def toString: String = "ENUM"
}

/**
 * Identifies a class as defining a Java 9 module.
 *
 * @note From the specification: "If ACC_MODULE is set in ClassFile.access_flags, then
 *      no other flag in `ClassFile.access_flags` may be set."
 *      The name of the class has to be "/module-info".
 * @note '''super_class, interfaces_count, fields_count, methods_count: zero. I.e.,
 *      a module does not have a super class.'''
 */
object ACC_MODULE extends AccessFlag {
    final override val javaName: None.type = None
    final override val mask = 0x8000 // SAME AS ACC_MANDATED (!)
    override def toString: String = "MODULE"
}

object ACC_OPEN extends AccessFlag {
    final val javaName = Some("open")
    final val mask = 0x0020
    override def toString: String = "OPEN"
}

object ACC_MANDATED extends AccessFlag {
    final override val javaName: None.type = None
    final override val mask = 0x8000
    override def toString: String = "MANDATED"
}

/**
 * @note From the JVM 9 specification: "Indicates that any module which depends on the current
 *       module, implicitly declares a dependence on the module indicated by this entry."
 * @note Only used in combination with Java 9 modules.
 */
object ACC_TRANSITIVE extends AccessFlag {
    final override val javaName = Some("transitive")
    final override val mask = 0x0010
    override def toString: String = "TRANSITIVE"
}

/**
 * @note From the JVM 9 specification: "Indicates that this [inter-module] dependence is
 *       mandatory in the static phase, i.e., at compile time, but is optional in the
 *       dynamic phase, i.e., at run time."
 * @note Only used in combination with Java 9 modules.
 */
object ACC_STATIC_PHASE extends AccessFlag {
    final override val javaName = Some("static")
    final override val mask = 0x0040
    override def toString: String = "STATIC_PHASE"
}

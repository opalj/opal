/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package bi

/**
 * Matches a given access flags bit array and enables the construction of complex matchers.
 *
 * @author Michael Eichberg
 */
sealed trait AccessFlagsMatcher { left =>

    def unapply(accessFlags: Int): Boolean

    /**
     * Creates a new matcher that matches `accessFlags` vectors where all flags
     * defined by this matcher and the given matcher have to be defined.
     */
    def &&(right: AccessFlagsMatcher): AccessFlagsMatcher = {
        new AccessFlagsMatcher {

            override def unapply(accessFlags: Int): Boolean = {
                left.unapply(accessFlags) && right.unapply(accessFlags)
            }

            override def toString: String = "("+left.toString+" && "+right.toString+")"
        }
    }

    def ||(right: AccessFlagsMatcher): AccessFlagsMatcher = {
        new AccessFlagsMatcher {

            override def unapply(accessFlags: Int): Boolean = {
                left.unapply(accessFlags) || right.unapply(accessFlags)
            }

            override def toString: String = "("+left.toString+" || "+right.toString+")"
        }
    }

    /**
     * Creates a new matcher that matches `accessFlags` that do not have (all of) the
     * accessFlags specified by the given matcher.
     */
    def unary_! : AccessFlagsMatcher =
        new AccessFlagsMatcher {

            override def unapply(accessFlags: Int): Boolean = !left.unapply(accessFlags)

            override def toString: String = "!("+left.toString+")"
        }
}

trait PrimitiveAccessFlagsMatcher extends AccessFlagsMatcher { left =>

    /**
     * An integer value that represents an access flags bit mask.
     */
    protected def mask: Int

    override def &&(right: AccessFlagsMatcher): AccessFlagsMatcher = {
        right match {
            case PrimitiveAccessFlagsMatcher(rightMask) =>
                new PrimitiveAccessFlagsMatcher {
                    protected val mask = left.mask | rightMask
                    def unapply(accessFlags: Int): Boolean = (accessFlags & mask) == mask
                    override def toString: String = mask.toString
                }
            case _ => super.&&(right)
        }
    }

    override def unary_! : AccessFlagsMatcher =
        new AccessFlagsMatcher { // <= it is no longer a primitive matcher
            val mask = left.mask
            override def unapply(accessFlags: Int): Boolean = (accessFlags & mask) != mask
            override def toString: String = "!("+mask.toString+")"
        }
}
/**
 * Extractor for the bitmask used by a [[PrimitiveAccessFlagsMatcher]].
 */
object PrimitiveAccessFlagsMatcher {
    def unapply(accessFlagsMatcher: PrimitiveAccessFlagsMatcher): Some[Int] =
        Some(accessFlagsMatcher.mask)
}

/**
 * Predefines several access flags matchers.
 *
 * @example
 * The predefined matchers are used in the following way:
 * {{{
 *  method match { case Method(PUBLIC_STATIC(),...) => ... }
 *  field match { case Field(PUBLIC_STATIC_FINAL(),...) => ... }
 * }}}
 *
 * @author Michael Eichberg
 */
object AccessFlagsMatcher {

    // DEFINED FOR READABILITY PURPOSES:
    final val PUBLIC = ACC_PUBLIC
    final val PRIVATE = ACC_PRIVATE
    final val PROTECTED = ACC_PROTECTED

    final val STATIC = ACC_STATIC

    final val PUBLIC_INTERFACE = ACC_PUBLIC && ACC_INTERFACE
    final val PUBLIC_ABSTRACT = ACC_PUBLIC && ACC_ABSTRACT
    final val PUBLIC_FINAL = ACC_PUBLIC && ACC_FINAL
    final val PRIVATE_FINAL = ACC_PRIVATE && ACC_FINAL
    final val PUBLIC_STATIC = ACC_PUBLIC && ACC_STATIC

    final val NOT_INTERFACE = !ACC_INTERFACE
    final val NOT_STATIC = !ACC_STATIC
    final val NOT_PRIVATE = !ACC_PRIVATE
    final val NOT_FINAL = !ACC_FINAL
    final val NOT_SYNCHRONIZED = !ACC_SYNCHRONIZED
    final val NOT_NATIVE = !ACC_NATIVE
    final val NOT_ABSTRACT = !ACC_ABSTRACT
    final val NOT_ENUM = !ACC_ENUM

    final val PUBLIC_STATIC_FINAL = PUBLIC_FINAL && ACC_STATIC

    final val PUBLIC___OR___PROTECTED_AND_NOT_FINAL = ACC_PUBLIC || (ACC_PROTECTED && NOT_FINAL)

    final val ANY = new AccessFlagsMatcher { def unapply(accessFlags: Int): Boolean = true }
}
